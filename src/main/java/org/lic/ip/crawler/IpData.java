package org.lic.ip.crawler;

import org.lic.ip.util.IPUtil;

/**
 * Created by lc on 15/1/9.
 */
public class IpData implements Comparable<IpData> {
    private String network;

    private String country;

    private String province;

    private String city;

    private String isp;

    private String ip;

    private int ipAmount;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getIpAmount() {
        return ipAmount;
    }

    public void setIpAmount(int ipAmount) {
        this.ipAmount = ipAmount;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String toFileString() {
        //["country", "province", "city", "isp", "ip", "ip_amount"]
        return new StringBuilder(network).append(";")
            .append(country).append(";")
            .append(province).append(";")
            .append(city).append(";")
            .append(isp).append(";")
            .append(ip).append(";")
            .append(ipAmount).toString();
    }

    public IpData copy() {
        IpData d = new IpData();
        d.setNetwork(network);
        d.setCountry(country);
        d.setCity(city);
        d.setProvince(province);
        d.setIsp(isp);
        d.setIp(ip);
        d.setIpAmount(ipAmount);
        return d;
    }

    @Override public String toString() {
        return "Data{" +
            "network='" + network + '\'' +
            ", country='" + country + '\'' +
            ", province='" + province + '\'' +
            ", city='" + city + '\'' +
            ", isp='" + isp + '\'' +
            ", ip='" + ip + '\'' +
            ", ipAmount=" + ipAmount +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IpData ipData = (IpData) o;

        if (city != null ? !city.equals(ipData.city) : ipData.city != null) {
            return false;
        }
        if (country != null ?
            !country.equals(ipData.country) :
            ipData.country != null) {
            return false;
        }
        if (isp != null ? !isp.equals(ipData.isp) : ipData.isp != null) {
            return false;
        }
        if (province != null ?
            !province.equals(ipData.province) :
            ipData.province != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = country != null ? country.hashCode() : 0;
        result = 31 * result + (province != null ? province.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (isp != null ? isp.hashCode() : 0);
        return result;
    }

    @Override public int compareTo(IpData ipData) {
        long startIP = IPUtil.ipString2Long(network.split("/")[0]);
        long cIP = IPUtil.ipString2Long(ipData.network.split("/")[0]);
        if (startIP - cIP > 0) return 1;
        else if (startIP - cIP < 0) return -1;
        else return 0;
    }
}
