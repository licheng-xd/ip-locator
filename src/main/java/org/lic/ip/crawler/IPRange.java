package org.lic.ip.crawler;

/**
 * Created by lc on 15/1/14.
 */
public class IPRange implements Comparable<IPRange> {

    public IPRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public IPRange(long start, long end, String cidr) {
        this.start = start;
        this.end = end;
        this.cidr = cidr;
    }

    public long start;

    public long end;

    public String cidr;

    public int prefixlen;

    @Override public int compareTo(IPRange ipRange) {
        long ret = start - ipRange.start;
        if (ret != 0)
            return (int)ret;
        else
            return (int) (ipRange.end - end);
    }
}
