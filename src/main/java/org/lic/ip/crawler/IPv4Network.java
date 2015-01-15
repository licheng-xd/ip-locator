package org.lic.ip.crawler;

import org.lic.ip.util.IPUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lc on 15/1/9.
 */
public class IPv4Network {
    int baseIPnumeric;

    int netmaskNumeric;

    int numericCIDR;

    public IPv4Network(int ip, int prefixlen) throws NumberFormatException {
        String IPinCIDRFormat = IPUtil.convertIntIpToString(ip) + "/" + prefixlen;
        String[] st = IPinCIDRFormat.split("\\/");
        if (st.length != 2)

            throw new NumberFormatException("Invalid CIDR format '"
                + IPinCIDRFormat + "', should be: xx.xx.xx.xx/xx");

        String symbolicIP = st[0];
        String symbolicCIDR = st[1];

        Integer numericCIDR = new Integer(symbolicCIDR);
        if (numericCIDR > 32)

            throw new NumberFormatException("CIDR can not be greater than 32");

        /* IP */
        st = symbolicIP.split("\\.");

        if (st.length != 4)
            throw new NumberFormatException("Invalid IP address: " + symbolicIP);

        int i = 24;
        baseIPnumeric = 0;

        for (int n = 0; n < st.length; n++) {

            int value = Integer.parseInt(st[n]);

            if (value != (value & 0xff)) {

                throw new NumberFormatException("Invalid IP address: "
                    + symbolicIP);
            }

            baseIPnumeric += value << i;
            i -= 8;

        }

        /* netmask from CIDR */
        if (numericCIDR < 8)
            throw new NumberFormatException(
                "Netmask CIDR can not be less than 8");
        netmaskNumeric = 0xffffffff;
        netmaskNumeric = netmaskNumeric << (32 - numericCIDR);
        this.numericCIDR = numericCIDR;
    }

    /**
     * Specify IP in CIDR format like: new IPv4("10.1.0.25/16");
     *
     * @param IPinCIDRFormat
     */
    public IPv4Network(String IPinCIDRFormat) throws NumberFormatException {

        String[] st = IPinCIDRFormat.split("\\/");
        if (st.length != 2)

            throw new NumberFormatException("Invalid CIDR format '"
                + IPinCIDRFormat + "', should be: xx.xx.xx.xx/xx");

        String symbolicIP = st[0];
        String symbolicCIDR = st[1];

        Integer numericCIDR = new Integer(symbolicCIDR);
        if (numericCIDR > 32)

            throw new NumberFormatException("CIDR can not be greater than 32");

        /* IP */
        st = symbolicIP.split("\\.");

        if (st.length != 4)
            throw new NumberFormatException("Invalid IP address: " + symbolicIP);

        int i = 24;
        baseIPnumeric = 0;

        for (int n = 0; n < st.length; n++) {

            int value = Integer.parseInt(st[n]);

            if (value != (value & 0xff)) {

                throw new NumberFormatException("Invalid IP address: "
                    + symbolicIP);
            }

            baseIPnumeric += value << i;
            i -= 8;

        }

        /* netmask from CIDR */
        if (numericCIDR < 8)
            throw new NumberFormatException(
                "Netmask CIDR can not be less than 8");
        netmaskNumeric = 0xffffffff;
        netmaskNumeric = netmaskNumeric << (32 - numericCIDR);
        this.numericCIDR = numericCIDR;
    }

    /**
     * Get the IP in symbolic form, i.e. xxx.xxx.xxx.xxx
     *
     * @return
     */
    public String getIP() {
        return convertNumericIpToSymbolic(baseIPnumeric);

    }

    private String convertNumericIpToSymbolic(Integer ip) {
        StringBuffer sb = new StringBuffer(15);

        for (int shift = 24; shift > 0; shift -= 8) {

            // process 3 bytes, from high order byte down.
            sb.append(Integer.toString((ip >>> shift) & 0xff));

            sb.append('.');
        }
        sb.append(Integer.toString(ip & 0xff));

        return sb.toString();
    }

    /**
     * Get the net mask in symbolic form, i.e. xxx.xxx.xxx.xxx
     *
     * @return
     */

    public String getNetmask() {
        StringBuffer sb = new StringBuffer(15);

        for (int shift = 24; shift > 0; shift -= 8) {

            // process 3 bytes, from high order byte down.
            sb.append(Integer.toString((netmaskNumeric >>> shift) & 0xff));

            sb.append('.');
        }
        sb.append(Integer.toString(netmaskNumeric & 0xff));

        return sb.toString();
    }

    /**
     * Get the IP and netmask in CIDR form, i.e. xxx.xxx.xxx.xxx/xx
     *
     * @return
     */

    public String getCIDR() {
        int i;
        for (i = 0; i < 32; i++) {

            if ((netmaskNumeric << i) == 0)
                break;

        }
        return convertNumericIpToSymbolic(baseIPnumeric & netmaskNumeric) + "/"
            + i;
    }

    public int getMasklen() {
        return this.numericCIDR;
    }

    /**
     * Get an arry of all the IP addresses available for the IP and netmask/CIDR
     * given at initialization
     *
     * @return
     */
    public List<String> getAvailableIPs(Integer numberofIPs) {

        ArrayList<String> result = new ArrayList<String>();
        int numberOfBits;

        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {

            if ((netmaskNumeric << numberOfBits) == 0)
                break;

        }
        Integer numberOfIPs = 0;
        for (int n = 0; n < (32 - numberOfBits); n++) {

            numberOfIPs = numberOfIPs << 1;
            numberOfIPs = numberOfIPs | 0x01;

        }

        Integer baseIP = baseIPnumeric & netmaskNumeric;

        for (int i = 1; i < (numberOfIPs) && i < numberofIPs; i++) {

            Integer ourIP = baseIP + i;

            String ip = convertNumericIpToSymbolic(ourIP);

            result.add(ip);
        }
        return result;
    }

    /**
     * Range of hosts
     *
     * @return
     */
    public String getHostAddressRange() {

        int numberOfBits;
        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {

            if ((netmaskNumeric << numberOfBits) == 0)
                break;
        }
        Integer numberOfIPs = 0;
        for (int n = 0; n < (32 - numberOfBits); n++) {

            numberOfIPs = numberOfIPs << 1;
            numberOfIPs = numberOfIPs | 0x01;

        }

        Integer baseIP = baseIPnumeric & netmaskNumeric;
        String firstIP = convertNumericIpToSymbolic(baseIP + 1);
        String lastIP = convertNumericIpToSymbolic(baseIP + numberOfIPs - 1);
        return firstIP + " - " + lastIP;
    }

    public IPRange getIPRange() {
        int numberOfBits;
        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {

            if ((netmaskNumeric << numberOfBits) == 0)
                break;
        }
        Integer numberOfIPs = 0;
        for (int n = 0; n < (32 - numberOfBits); n++) {

            numberOfIPs = numberOfIPs << 1;
            numberOfIPs = numberOfIPs | 0x01;

        }

        Integer baseIP = baseIPnumeric & netmaskNumeric;
        return new IPRange(baseIP, baseIP + numberOfIPs, getCIDR());
    }

    public List<String> getSubnet(int cdir) {
        if (cdir > 32 || cdir < 8 || cdir < numericCIDR) {
            throw new NumberFormatException("CIDR can not be greater than 32");
        }
        int numberOfIPs = (int) Math.pow(2, 32 - cdir);
        Integer baseIP = baseIPnumeric & netmaskNumeric;
        List<String> list = new ArrayList<String>();
        for (int i=0; i<Math.pow(2, cdir-numericCIDR); i++) {
            String subnet = convertNumericIpToSymbolic(baseIP) + "/" + cdir;
            baseIP += numberOfIPs;
            list.add(subnet);
        }
        return list;
    }

    /**
     * Returns number of hosts available in given range
     *
     * @return number of hosts
     */
    public Long getNumberOfHosts() {
        int numberOfBits;

        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {

            if ((netmaskNumeric << numberOfBits) == 0)
                break;

        }

        Double x = Math.pow(2, (32 - numberOfBits));

        if (x == -1)
            x = 1D;

        return x.longValue();
    }

    /**
     * The XOR of the netmask
     *
     * @return wildcard mask in text form, i.e. 0.0.15.255
     */

    public String getWildcardMask() {
        Integer wildcardMask = netmaskNumeric ^ 0xffffffff;

        StringBuffer sb = new StringBuffer(15);
        for (int shift = 24; shift > 0; shift -= 8) {

            // process 3 bytes, from high order byte down.
            sb.append(Integer.toString((wildcardMask >>> shift) & 0xff));

            sb.append('.');
        }
        sb.append(Integer.toString(wildcardMask & 0xff));

        return sb.toString();

    }

    public String getBroadcastAddress() {

        if (netmaskNumeric == 0xffffffff)
            return "0.0.0.0";

        int numberOfBits;
        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {

            if ((netmaskNumeric << numberOfBits) == 0)
                break;

        }
        Integer numberOfIPs = 0;
        for (int n = 0; n < (32 - numberOfBits); n++) {

            numberOfIPs = numberOfIPs << 1;
            numberOfIPs = numberOfIPs | 0x01;
        }

        Integer baseIP = baseIPnumeric & netmaskNumeric;
        Integer ourIP = baseIP + numberOfIPs;

        String ip = convertNumericIpToSymbolic(ourIP);

        return ip;
    }

    private String getBinary(Integer number) {
        String result = "";

        Integer ourMaskBitPattern = 1;
        for (int i = 1; i <= 32; i++) {

            if ((number & ourMaskBitPattern) != 0) {

                result = "1" + result; // the bit is 1
            } else { // the bit is 0

                result = "0" + result;
            }
            if ((i % 8) == 0 && i != 0 && i != 32)

                result = "." + result;
            ourMaskBitPattern = ourMaskBitPattern << 1;

        }
        return result;
    }

    public String getNetmaskInBinary() {

        return getBinary(netmaskNumeric);
    }

    /**
     * Checks if the given IP address contains in subnet
     *
     * @param IPaddress
     * @return
     */
    public boolean contains(String IPaddress) {

        Integer checkingIP = 0;
        String[] st = IPaddress.split("\\.");

        if (st.length != 4)
            throw new NumberFormatException("Invalid IP address: " + IPaddress);

        int i = 24;
        for (int n = 0; n < st.length; n++) {

            int value = Integer.parseInt(st[n]);

            if (value != (value & 0xff)) {

                throw new NumberFormatException("Invalid IP address: "
                    + IPaddress);
            }

            checkingIP += value << i;
            i -= 8;
        }

        if ((baseIPnumeric & netmaskNumeric) == (checkingIP & netmaskNumeric))

            return true;
        else
            return false;
    }

    public boolean contains(IPv4Network child) {

        Integer subnetID = child.baseIPnumeric;

        Integer subnetMask = child.netmaskNumeric;

        if ((subnetID & this.netmaskNumeric) == (this.baseIPnumeric & this.netmaskNumeric)) {

            if ((this.netmaskNumeric < subnetMask) == true
                && this.baseIPnumeric <= subnetID) {

                return true;
            }

        }
        return false;

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        IPv4Network ipv4 = new IPv4Network("192.72.40.0/21");
        System.out.println(ipv4.getCIDR());
        System.out.println(ipv4.getNetmask());
        System.out.println(ipv4.getNumberOfHosts());
        System.out.println(ipv4.getWildcardMask());
        System.out.println(ipv4.getBroadcastAddress());
        System.out.println(ipv4.getHostAddressRange());
        System.out.println(ipv4.getSubnet(24));
    }
}
