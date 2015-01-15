package org.lic.ip.crawler;

import org.lic.ip.util.IPUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by lc on 15/1/8.
 */
public class IPv4RadixTree {
    /**
     * Special value that designates that there are no value stored in the key so far.
     * One can't use store value in a tree.
     */
    public static final Data NO_VALUE = null;

    private static final int NULL_PTR = -1;
    private static final int ROOT_PTR = 0;

    private static final long MAX_IPV4_BIT = 0x80000000L;

    private int[] rights;
    private int[] lefts;
    private Data[] values;

    private int allocatedSize;
    private int size;

    /**
     * Initializes IPv4 radix tree with default capacity of 1024 nodes. It should
     * be sufficient for small databases.
     */
    public IPv4RadixTree() {
        init(1024);
    }

    /**
     * Initializes IPv4 radix tree with a given capacity.
     * @param allocatedSize initial capacity to allocate
     */
    public IPv4RadixTree(int allocatedSize) {
        init(allocatedSize);
    }

    private void init(int allocatedSize) {
        this.allocatedSize = allocatedSize;

        rights = new int[this.allocatedSize];
        lefts = new int[this.allocatedSize];
        values = new Data[this.allocatedSize];

        size = 1;
        lefts[0] = NULL_PTR;
        rights[0] = NULL_PTR;
        values[0] = NO_VALUE;
    }

    /**
     * Puts a key-value pair in a tree.
     * @param key IPv4 network prefix
     * @param mask IPv4 netmask in networked byte order format (for example,
     * 0xffffff00L = 4294967040L corresponds to 255.255.255.0 AKA /24 network
     * bitmask)
     * @param value an arbitrary value that would be stored under a given key
     */
    public void put(long key, long mask, Data value) {
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
            //            if (node.value != NO_VALUE) {
            //                throw new IllegalArgumentException();
            //            }

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

        int[] newLefts = new int[allocatedSize];
        System.arraycopy(lefts, 0, newLefts, 0, oldSize);
        lefts = newLefts;

        int[] newRights = new int[allocatedSize];
        System.arraycopy(rights, 0, newRights, 0, oldSize);
        rights = newRights;

        Data[] newValues = new Data[allocatedSize];
        System.arraycopy(values, 0, newValues, 0, oldSize);
        values = newValues;
    }

    /**
     * Selects a value for a given IPv4 address, traversing tree and choosing
     * most specific value available for a given address.
     * @param key IPv4 address to look up
     * @return value at most specific IPv4 network in a tree for a given IPv4
     * address
     */
    public Data selectValue(long key) {
        long bit = MAX_IPV4_BIT;
        Data value = NO_VALUE;
        int node = ROOT_PTR;

        while (node != NULL_PTR) {
            if (values[node] != NO_VALUE)
                value = values[node];
            node = ((key & bit) != 0) ? rights[node] : lefts[node];
            bit >>= 1;
        }

        return value;
    }

    /**
     * Puts a key-value pair in a tree, using a string representation of IPv4 prefix.
     * @param ipNet IPv4 network as a string in form of "a.b.c.d/e", where a, b, c, d
     * are IPv4 octets (in decimal) and "e" is a netmask in CIDR notation
     * @param value an arbitrary value that would be stored under a given key
     * @throws UnknownHostException
     */
    public void put(String ipNet, Data value) throws UnknownHostException {
        int pos = ipNet.indexOf('/');
        String ipStr = ipNet.substring(0, pos);
        long ip = inet_aton(ipStr);

        String netmaskStr = ipNet.substring(pos + 1);
        int cidr = Integer.parseInt(netmaskStr);
        long netmask =  ((1L << (32 - cidr)) - 1L) ^ 0xffffffffL;

        put(ip, netmask, value);
    }

    /**
     * Selects a value for a given IPv4 address, traversing tree and choosing
     * most specific value available for a given address.
     * @param ipStr IPv4 address to look up, in string form (i.e. "a.b.c.d")
     * @return value at most specific IPv4 network in a tree for a given IPv4
     * address
     * @throws UnknownHostException
     */
    public Data selectValue(String ipStr) throws UnknownHostException {
        return selectValue(inet_aton(ipStr));
    }

    /**
     * Helper function that reads IPv4 radix tree from a local file in tab-separated format:
     * (IPv4 net => value)
     * @param filename name of a local file to read
     * @return a fully constructed IPv4 radix tree from that file
     * @throws IOException
     */
    public void loadFromLocalFile(String filename) throws IOException {
        IPv4RadixTree tr = new IPv4RadixTree(countLinesInLocalFile(filename));
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String l;
        Data value;
        //["country", "province", "city", "isp", "ip", "ip_amount"]
        while ((l = br.readLine()) != null) {
            String[] c = l.split(";");
            //1.0.0.0/24;澳大利亚;;;;223.255.255.111;256
            //1.0.1.0/24;中国;福建省;福州市;电信;1.0.1.53;256
            value = new Data();
            value.setCountry(c[1]);
            value.setProvince(c[2]);
            value.setCity(c[3]);
            value.setIsp(c[4]);
            value.setIp(c[5]);
            value.setIpAmount(Integer.parseInt(c[6]));

            put(c[0], value);
        }
    }

    private static long inet_aton(String ipStr) throws UnknownHostException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putInt(0);
        bb.put(InetAddress.getByName(ipStr).getAddress());
        bb.rewind();
        return bb.getLong();
    }

    private static int countLinesInLocalFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        int n = 0;
        String l;
        while ((l = br.readLine()) != null) {
            n++;
        }
        return n;
    }

    /**
     * Returns a size of tree in number of nodes (not number of prefixes stored).
     * @return a number of nodes in current tree
     */
    public int size() { return size; }

    public void writeRawToFile(String filename) throws IOException {

        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(filename)), "UTF-8");
        for (Data data : values) {
            if (data != null) {
                writer.write(data.toFileString() + "\n");
            }
        }
        writer.close();
    }

    public void merge() {
        Deque<Data> mergedDeque = new ArrayDeque<Data>();
        Deque<Data> tmpDeque = new ArrayDeque<Data>();
        for (Data data : values) {
            if (data == null) continue;
            data.setIpAmount(IPUtil.getAmount(data.getNetwork()));
            if (!tmpDeque.isEmpty()) {
                Data pdata = tmpDeque.peekLast();
                if (data.equals(pdata) || (data.getCountry().equals("中国") && data.getCountry().equals(pdata.getCountry()))) {
                    // 相同
                    tmpDeque.addLast(data);
                } else {
                    // 不同，合并tmpDeque，写入mergedDeque
                    Data first = tmpDeque.peekFirst().copy();
                    List<String> tmpCidrs = new ArrayList<String>();
                    for (Data d : tmpDeque) {
                        tmpCidrs.add(d.getNetwork());
                    }
                    List<String> mergedCidrs = IPUtil.mergeCidrs(tmpCidrs);
                    for (String cidr : mergedCidrs) {
                        Data d = first.copy();
                        d.setNetwork(cidr);
                        mergedDeque.addLast(d);
                    }
                }
            } else {
                tmpDeque.addLast(data);
            }
        }

        for (Data d : mergedDeque) {
            System.out.println(d.toFileString());
        }
    }
}
