package org.lic.ip.crawler;

/**
 * Created by lc on 15/1/14.
 */
public class IPRange implements Comparable<IPRange> {

    public IPRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public IPRange(int start, int end, String cidr) {
        this.start = start;
        this.end = end;
        this.cidr = cidr;
    }

    public int start;

    public int end;

    public String cidr;

    public int prefixlen;

    @Override public int compareTo(IPRange ipRange) {
        int ret = start - ipRange.start;
        if (ret != 0)
            return ret;
        else
            return ipRange.end - end;
    }
}
