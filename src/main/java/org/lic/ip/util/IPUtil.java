package org.lic.ip.util;

import org.lic.ip.crawler.IPRange;
import org.lic.ip.crawler.IPv4Network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by lc on 14/10/30.
 */
public class IPUtil {
    private static Random random = new Random();

    // Convert dotted IPv4 address to integer.
    public static long ipString2Long(String ip) {
        String[] ss = ip.split("\\.");
        if (ss.length != 4) {
            return -1L;
        }
        long ret = 0;
        for (int i=0; i<4; i++) {
            ret = ret << 8 | Integer.parseInt(ss[i]);
        }
        return ret;
    }

    // Convert 32-bit integer to dotted IPv4 address.
    public static String ipLong2String(long lip) {
        String s1 = String.valueOf(lip >> 24 & 0xFF);
        String s2 = String.valueOf(lip >> 16 & 0xFF);
        String s3 = String.valueOf(lip >> 8 & 0xFF);
        String s4 = String.valueOf(lip >> 0 & 0xFF);
        String ip = s1 + "." + s2 + "." + s3 + "." + s4;
        return ip;
    }

    public static String convertIntIpToString(Integer ip) {
        StringBuffer sb = new StringBuffer(15);
        for (int shift = 24; shift > 0; shift -= 8) {
            // process 3 bytes, from high order byte down.
            sb.append(Integer.toString((ip >>> shift) & 0xff));
            sb.append('.');
        }
        sb.append(Integer.toString(ip & 0xff));
        return sb.toString();
    }

    public static int convertStringIpToInt(String ip) {
//        String[] ss = ip.split("\\.");
//        if (ss.length != 4) {
//            return -1;
//        }
//        int ret = 0;
//        for (int i=0; i<4; i++) {
//            ret = ret << 8 | Integer.parseInt(ss[i]);
//        }
//        return ret;
        String[] ss = ip.split("\\.");
        if (ss.length != 4) {
            return -1;
        }
        long ret = 0;
        for (int i=0; i<4; i++) {
            ret = ret << 8 | Integer.parseInt(ss[i]);
        }
        return (int)ret;
    }

    public static String getRandomIp(long netWork) {
        int tmp = random.nextInt(255);
        return ipLong2String(netWork + tmp);
    }

    public static int getAmount(String cidr) {
        int masklen = Integer.parseInt(cidr.split("/")[1]);
        return 1<<(32-masklen);
    }

    public static int getPrefixlen(String cidr) {
        int masklen = Integer.parseInt(cidr.split("/")[1]);
        return masklen;
    }

    public static String getRandomIp(String baseIP, int masklen) {
        int tmp = Math.abs(random.nextInt((1<<(32-masklen)) -1));
        return ipLong2String(ipString2Long(baseIP) + tmp);
    }

    public static int getSmallestMasklen(int amount) {
//        int i = 0;
//        while (Math.pow(2, i) != amount) {
//            if (Math.pow(2, i) < amount) {
//                i++;
//            } else {
//                amount = amount - (int)Math.pow(2, i-1);
//                i = 0;
//            }
//        }
//        return 32 - i;
        int i = 1;
        int ret = 0;
        while ((i&amount) == 0) {
            i = i<<1;
            ret++;
        }
        return 32 - ret;
    }

    public static LinkedList<String> mergeCidrs(List<String> cidrs) {
        TreeSet<IPRange> rangesSet = new TreeSet<IPRange>();
        for (String cidr : cidrs) {
            rangesSet.add(new IPv4Network(cidr).getIPRange());
        }
        LinkedList<IPRange> ranges = new LinkedList<IPRange>(rangesSet);
        int i = 1;
        while (i < ranges.size()) {
            IPRange pRange = ranges.get(i-1);
            IPRange range = ranges.get(i);
            if (range.start - 1 <= pRange.end) {
                IPRange newRange = new IPRange(pRange.start, Math.max(range.end, pRange.end));
                newRange.prefixlen = getPrefixlen(range.cidr);
                ranges.set(i-1, newRange);
                ranges.remove(i);
            } else {
                i++;
            }
        }
        LinkedList<String> result = new LinkedList<String>();
        for (IPRange range : ranges) {
            if (range.cidr != null) {
                result.add(range.cidr);
            } else {
                for (IPv4Network network : iprangeToCidrs(range)) {
                    result.add(network.getCIDR());
                }
            }
        }
        return result;
    }

    public static LinkedList<IPv4Network> iprangeToCidrs(IPRange range) {
        if (range.prefixlen < 8 || range.prefixlen > 32) {
            range.prefixlen = 24;
        }
        LinkedList<IPv4Network> cidrList = new LinkedList<IPv4Network>();
        IPv4Network spanCidr = spanningCidr(range.start, range.end,
            range.prefixlen);
        if (spanCidr.getIPRange().start < range.start) {
            IPv4Network exclude = new IPv4Network(range.start, range.prefixlen);
            cidrList = cidrPartition(spanCidr, exclude).get(2);
            spanCidr = cidrList.pollLast();
        }
        if (spanCidr.getIPRange().end > range.end) {
            IPv4Network exclude = new IPv4Network(range.end, range.prefixlen);
            cidrList.addAll(cidrPartition(spanCidr, exclude).get(0));
        } else {
            cidrList.addLast(spanCidr);
        }
        return cidrList;
    }

    public static IPv4Network spanningCidr(long start, long end, int prefixlen) {
        if (start == 704380928) {
            System.out.println("debug");
        }
        //int prefixlen = 24;
//        int ipnum = end; //- (1<<(32 - prefixlen));
//        while (prefixlen > 0 && ipnum > start) {
//            prefixlen--;
//            ipnum &= -(1<<(32-prefixlen));
//            //ipnum = ipnum - (1<<(32 - prefixlen));
//        }
//        if (prefixlen < 8 || prefixlen > 32) {
//            System.out.println("debug");
//        }
        while (end - start > (1<<(32-prefixlen))) {
            prefixlen--;
        }
        return new IPv4Network(end - (1<<(32-prefixlen)), prefixlen);
    }

    public static LinkedList<LinkedList<IPv4Network>> cidrPartition(IPv4Network target, IPv4Network exclude) {
        LinkedList<LinkedList<IPv4Network>> ret = new LinkedList<LinkedList<IPv4Network>>();

        LinkedList<IPv4Network> left = new LinkedList<IPv4Network>();
        LinkedList<IPv4Network> middle = new LinkedList<IPv4Network>();
        LinkedList<IPv4Network> right = new LinkedList<IPv4Network>();
        if (exclude.getIPRange().end < target.getIPRange().start) {
            right.add(target);
            ret.addLast(left);
            ret.addLast(middle);
            ret.addLast(right);
            return ret;
        }
        if (target.getIPRange().end < exclude.getIPRange().start) {
            left.add(target);
            ret.addLast(left);
            ret.addLast(middle);
            ret.addLast(right);
            return ret;
        }
        if (target.getMasklen() >= exclude.getMasklen()) {
            middle.add(target);
            ret.addLast(left);
            ret.addLast(middle);
            ret.addLast(right);
            return ret;
        }

        int newPrefixlen = target.getMasklen() + 1;
        long targetStart = target.getIPRange().start;
        long i_lower = targetStart;
        long i_upper = targetStart + (1 << (32 - newPrefixlen));
        long matched;
        while (exclude.getMasklen() >= newPrefixlen) {
            if (exclude.getIPRange().start > i_upper) {
                left.add(new IPv4Network(i_lower, newPrefixlen));
                matched = i_upper;
            } else {
                right.add(new IPv4Network(i_upper, newPrefixlen));
                matched = i_lower;
            }
            newPrefixlen++;

            if (newPrefixlen > 32)
                break;

            i_lower = matched;
            i_upper = matched + (1 << (32 - newPrefixlen));
        }
        middle.add(exclude);
        Collections.reverse(right);

        ret.addLast(left);
        ret.addLast(middle);
        ret.addLast(right);
        return ret;
    }

    public static void main(String[] args) throws Exception {
//        BufferedReader reader = new BufferedReader(new FileReader(new File("/Users/lc/github/ipdb_creator/output/cn-original")));
//        List<String> cidrs = new ArrayList<String>();
//        String line;
//        while((line = reader.readLine()) != null) {
//            cidrs.add(line.split(";")[0]);
//        }
//        System.out.println(mergeCidrs(cidrs));
        //System.out.println(getSmallestMasklen(1024));
        //System.out.println(ipLong2String(0x80000000L));
        System.out.println(ipString2Long("255.255.255.255"));
//        int ip = 704643072;
//        ip &= -(1<<(32-19));
//        System.out.println(ip);
    }
}
