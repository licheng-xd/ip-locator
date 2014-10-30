package org.lic.ip.util;

import java.util.Random;

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

    public static String getRandomIp(long netWork) {
        int tmp = random.nextInt(255);
        return ipLong2String(netWork + tmp);
    }
}
