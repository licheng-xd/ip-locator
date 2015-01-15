/*
 * Copyright (C) 2012 Openstat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lic.ip.iplocator;

import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * A minimalistic, memory size-savvy and fairly fast radix tree (AKA Patricia
 * trie) implementation that uses IPv4 addresses with netmasks as keys and
 * 32-bit signed integers as values. This tree is generally uses in read-only
 * manner: there are no key removal operation and the whole thing works best in
 * pre-allocated fashion.
 */

/**
 * https://github.com/openstat/ip-radix-tree
 */
public class IPv4RadixIntTree {
    private static final Logger logger = LoggerFactory
        .getLogger(IPv4RadixIntTree.class);

    /**
     * Special value that designates that there are no value stored in the key
     * so far. One can't use store value in a tree.
     */
    public static final IPLocation NO_VALUE = IPLocation.getNullInstance();

    private static final int NULL_PTR = -1;

    private static final int ROOT_PTR = 0;

    private static final long MAX_IPV4_BIT = 0x80000000L;

    private int[] rights;

    private int[] lefts;

    private IPLocation[] values;

    private int allocatedSize;

    private int size;

    private static class SingletonHolder {
        public static final IPv4RadixIntTree instance = new IPv4RadixIntTree();
    }

    public static IPv4RadixIntTree getInstance() {
        return SingletonHolder.instance;
    }

    private IPv4RadixIntTree() {
        StopWatch sw = new StopWatch();
        sw.start();

        try {
            String filepath = getClass().getClassLoader()
                .getResource("ip_location.txt").getPath();

            int lines = countLinesInLocalFile(filepath);
            logger.info("file lines: {}", lines);

            init(lines);
            loadFromLocalFile(filepath);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        sw.stop();
        logger.info("init cost: {}ms", sw.getTime());
    }

    private void init(int allocatedSize) {
        this.allocatedSize = allocatedSize;

        rights = new int[this.allocatedSize];
        lefts = new int[this.allocatedSize];
        values = new IPLocation[this.allocatedSize];

        size = 1;
        lefts[0] = NULL_PTR;
        rights[0] = NULL_PTR;
        values[0] = NO_VALUE;
    }

    private int countLinesInLocalFile(String filepath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        int n = 0;
        while (br.readLine() != null) {
            n++;
        }
        br.close();
        return n;
    }

    /**
     * Helper function that reads IPv4 radix tree from a local file in
     * tab-separated format: (IPv4 net => value)
     * 
     * @param filepath
     *            name of a local file to read
     * @return a fully constructed IPv4 radix tree from that file
     * @throws java.io.IOException
     */
    private void loadFromLocalFile(String filepath) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(
            new FileInputStream(filepath), "UTF-8"));
        String l;
        IPLocation value;

        // 1.0.1.0/24;中国;福建省;福州市;电信;1.0.1.208;256
        while ((l = br.readLine()) != null) {
            String[] c = l.split(";");

            // value = String.format("%s %s %s %s", c[1], c[2], c[3], c[4]);
            value = new IPLocation();
            value.country = c[1].equals("中国") ? c[2] : c[1]; // 如果是国内ip，country字段放省名
            value.area = c[4]; // 运营商名

            put(c[0], value);
        }

        br.close();
        logger.info("load ok, tree size: {}", size());
    }

    public void prefixMerge() {

    }

    /**
     * Puts a key-value pair in a tree, using a string representation of IPv4
     * prefix.
     *
     * @param ipNet
     *            IPv4 network as a string in form of "a.b.c.d/e", where a, b,
     *            c, d are IPv4 octets (in decimal) and "e" is a netmask in CIDR
     *            notation
     * @param value
     *            an arbitrary value that would be stored under a given key
     * @throws java.net.UnknownHostException
     */
    private void put(String ipNet, IPLocation value)
        throws UnknownHostException {
        int pos = ipNet.indexOf('/');
        String ipStr = ipNet.substring(0, pos);
        long ip = inet_aton(ipStr);

        String netmaskStr = ipNet.substring(pos + 1);
        int cidr = Integer.parseInt(netmaskStr);
        long netmask = ((1L << (32 - cidr)) - 1L) ^ 0xffffffffL;

        put(ip, netmask, value);
    }

    /**
     * Puts a key-value pair in a tree.
     *
     * @param key
     *            IPv4 network prefix
     * @param mask
     *            IPv4 netmask in networked byte order format (for example,
     *            0xffffff00L = 4294967040L corresponds to 255.255.255.0 AKA /24
     *            network bitmask)
     * @param value
     *            an arbitrary value that would be stored under a given key
     */
    private void put(long key, long mask, IPLocation value) {
        long bit = MAX_IPV4_BIT;
        int node = ROOT_PTR;
        int next = ROOT_PTR;

        while ((bit & mask) != 0) {
            next = ((key & bit) != 0) ? rights[node] : lefts[node];
            if (next == NULL_PTR)
                break;
            bit >>= 1;
            node = next;
        }

        if (next != NULL_PTR) {
            // if (node.value != NO_VALUE) {
            // throw new IllegalArgumentException();
            // }

            values[node] = value;
            return;
        }

        while ((bit & mask) != 0) {
            if (size == allocatedSize)
                expandAllocatedSize();

            next = size;
            values[next] = NO_VALUE;
            rights[next] = NULL_PTR;
            lefts[next] = NULL_PTR;

            if ((key & bit) != 0) {
                rights[node] = next;
            } else {
                lefts[node] = next;
            }

            bit >>= 1;
            node = next;
            size++;
        }

        values[node] = value;
    }

    private void expandAllocatedSize() {
        int oldSize = allocatedSize;
        allocatedSize = allocatedSize * 2;
        logger.info("expandAllocatedSize: {} -> {}", oldSize, allocatedSize);

        int[] newLefts = new int[allocatedSize];
        System.arraycopy(lefts, 0, newLefts, 0, oldSize);
        lefts = newLefts;

        int[] newRights = new int[allocatedSize];
        System.arraycopy(rights, 0, newRights, 0, oldSize);
        rights = newRights;

        IPLocation[] newValues = new IPLocation[allocatedSize];
        System.arraycopy(values, 0, newValues, 0, oldSize);
        values = newValues;
    }

    /**
     * Selects a value for a given IPv4 address, traversing tree and choosing
     * most specific value available for a given address.
     *
     * @param ipStr
     *            IPv4 address to look up, in string form (i.e. "a.b.c.d")
     * @return value at most specific IPv4 network in a tree for a given IPv4
     *         address
     * @throws java.net.UnknownHostException
     */
    public IPLocation get(String ipStr) {
        return get(inet_aton(ipStr));
    }

    /**
     * Selects a value for a given IPv4 address, traversing tree and choosing
     * most specific value available for a given address.
     * 
     * @param key
     *            IPv4 address to look up
     * @return value at most specific IPv4 network in a tree for a given IPv4
     *         address
     */
    public IPLocation get(long key) {
        long bit = MAX_IPV4_BIT;
        IPLocation value = NO_VALUE;
        int node = ROOT_PTR;

        while (node != NULL_PTR) {
            if (values[node] != NO_VALUE)
                value = values[node];
            node = ((key & bit) != 0) ? rights[node] : lefts[node];
            bit >>= 1;
        }

        return value;
    }

    private static long inet_aton(String ipStr) {
        try {
            ByteBuffer bb = ByteBuffer.allocate(8);
            bb.putInt(0);
            bb.put(InetAddress.getByName(ipStr).getAddress());
            bb.rewind();
            return bb.getLong();
        } catch (UnknownHostException e) {
            logger.error(e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Returns a size of tree in number of nodes (not number of prefixes
     * stored).
     * 
     * @return a number of nodes in current tree
     */
    public int size() {
        return size;
    }

    public static void main(String[] args) throws Exception {
        final IPv4RadixIntTree ipTree = IPv4RadixIntTree.getInstance();
//
//        final String ipArray[] = { "123.58.181.1", "115.236.97.158",
//            "182.140.134.24", "115.236.153.148", "114.113.197.131",
//            "115.236.153.148", "123.58.181.1", "115.236.153.148",
//            "123.58.181.58", "127.0.0.1" };
//
//        for (int i = 0; i < 1; i++) {
//            new Thread() {
//                @Override
//                public void run() {
//                    // while (true) {
//                    try {
//                        for (String ip: ipArray) {
//                            IPLocation ipl = ipTree.get(ip);
//                            System.out.println(String.format("%s [%s %s]", ip,
//                                ipl.country, ipl.area));
//                        }
//
//                        Thread.sleep(10000);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    // }
//                }
//            }.start();
//        }
//        int cidr = 16;
//        long netmask = ((1L << (32 - cidr)) - 1L) ^ 0xffffffffL;
//        System.out.println(Long.toHexString(netmask));
    }

}
