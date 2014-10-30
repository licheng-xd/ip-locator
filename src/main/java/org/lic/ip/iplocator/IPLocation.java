package org.lic.ip.iplocator;

/**
 * 用来封装ip相关信息，目前只有两个字段，ip所在的国家和地区
 *
 * @author lc
 */
public class IPLocation {

    public static final String UNKNOWN_COUNTRY = "unknown_country";

    public static final String UNKNOWN_AREA = "unknown_area";

    public String country;

    public String area;

    public static IPLocation getNullInstance() {
        IPLocation ipl = new IPLocation();
        ipl.country = UNKNOWN_COUNTRY;
        ipl.area = UNKNOWN_AREA;
        return ipl;
    }

    @Override
    public String toString() {
        return "IPLocation [country=" + country + ", area=" + area + "]";
    }

}
