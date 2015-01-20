package org.lic.test;

import org.junit.Test;
import org.lic.ip.iplocator.IPv4RadixIntTree;
import org.lic.ip.ipseeker.IPSeeker;

/**
 * Created by lc on 14-10-9.
 */
public class Test1 {

    //private static final IPSeeker ipSeeker = IPSeeker.getInstance();  // 纯真

    private static final IPv4RadixIntTree ipLoactor = IPv4RadixIntTree.getInstance(); // 文本库

    @Test
    public void test() throws Exception {
        String ip = "172.29.31.1";
        //System.out.println(ipSeeker.getIPLocation(ip));
        System.out.println(ipLoactor.get(ip));
    }
}
