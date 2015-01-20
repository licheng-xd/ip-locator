package org.lic.ip.crawler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.lic.ip.util.HttpClientPool;
import org.lic.ip.util.IPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by lc on 15/1/8.
 */
public class Crawler {
    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    private static final String TAOBAO_URL = "http://ip.taobao.com/service/getIpInfo.php";

    private LimitRate limitRate = new LimitRate(1000L, 10);

    private static String cn_delegated = "delegated-apnic-test";

    private static String CN_OUT_ORIGINAL = "delegated-cn-original";

    private static String FN_OUT_ORIGINAL = "delegated-fn-original";

    private static String CN_OUT_MERGED = "delegated-cn-merged";

    private static String FN_OUT_MERGED = "delegated-fn-merged";

    private static String IN_PATH = "input/";

    private static String OUT_PATH = "output/";

    private static String[] all_delegated = {"delegated-afrinic-latest","delegated-apnic-latest",
        "delegated-arin-latest", "delegated-lacnic-latest", "delegated-ripencc-latest"};

    private static Map<String, String> countryCode = new HashMap<String, String>();

    private static Map<String, LinkedList<IPv4Network>> dict = new HashMap<String, LinkedList<IPv4Network>>();

    private static LinkedList<IPv4Network> availableIPs = new LinkedList<IPv4Network>();

    public IPv4RadixTree scanFNIP() {
        IPv4RadixTree fnTree = new IPv4RadixTree();
        BufferedReader reader = null;
        try {
            // load country code
            reader = new BufferedReader(
                new FileReader(new File(IN_PATH + "country_code")));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] sp = line.split(" ");
                countryCode.put(sp[0].trim(), sp[1].trim());
            }
            reader.close();

            for (String file : all_delegated) {
                reader = new BufferedReader(
                    new FileReader(new File(IN_PATH + file)));
                while ((line = reader.readLine()) != null) {
                    String[] params = line.split("\\|");

                    if (params.length >= 4
                        && params[2].equals("ipv4")
                        && !params[3].equals("*")
                        && !params[1].equals("CN")) {
                        long startIP = IPUtil.ipString2Long(params[3]);
                        long endIP = startIP + Integer.parseInt(params[4]);
                        logger.info(startIP + " " + endIP + " " + Integer
                            .parseInt(params[4]));
                        IPRange ipRange = new IPRange(startIP, endIP);
                        ipRange.prefixlen = IPUtil.getSmallestMasklen(Integer.parseInt(params[4]));
                        if (params[1].equals("")) {
                            availableIPs.addAll(IPUtil.iprangeToCidrs(ipRange));
                        } else {
                            if (dict.containsKey(params[1])) {
                                LinkedList<IPv4Network> lst = dict.get(params[1]);
                                lst.addAll(IPUtil.iprangeToCidrs(ipRange));
                                dict.put(params[1], lst);
                            } else {
                                dict.put(params[1], IPUtil.iprangeToCidrs(ipRange));
                            }
                        }
                    }
                }
            }
            System.out.println("finish read");

            for (String key : dict.keySet()) {
                IPv4Network net = dict.get(key).getLast();
                String randomIP = IPUtil
                    .getRandomIp(net.getCIDR().split("/")[0], net.getMasklen());
                for (IPv4Network network : dict.get(key)) {
                    IpData ipData = new IpData();
                    ipData.setNetwork(network.getCIDR());
                    ipData.setCountry(countryCode.get(key));
                    ipData.setProvince("");
                    ipData.setCity("");
                    ipData.setIsp("");
                    ipData.setIp(randomIP);
                    ipData.setIpAmount(IPUtil.getAmount(network.getCIDR()));
                    fnTree.put(network.getCIDR(), ipData);
                }
            }

            for (IPv4Network network : availableIPs) {
                String randomIP = IPUtil
                    .getRandomIp(network.getCIDR().split("/")[0],
                        network.getMasklen());
                IpData ipData = null;
                System.out.println("query ip " + network.getCIDR());
                if (ipData == null) {
                    while (ipData == null) {
                        try {
                            ipData = queryFromTaobao(randomIP);
                        } catch (Exception e) {
                            logger.error(
                                "queryFromTaobao exception: " + e
                                    .getMessage(), e);
                        }
                    }
                }
                ipData.setIpAmount(IPUtil.getAmount(network.getCIDR()));
                ipData.setNetwork(network.getCIDR());
                fnTree.put(network.getCIDR(), ipData);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                fnTree.writeRawToFile(OUT_PATH + FN_OUT_ORIGINAL);
                fnTree.merge();
                fnTree.writeRawToFile(OUT_PATH + FN_OUT_MERGED);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return fnTree;
    }

    public IPv4RadixTree scanCNIP() {
        IPv4RadixTree cnTree = new IPv4RadixTree();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                new FileReader(new File(IN_PATH + cn_delegated)));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] params = line.split("\\|");
                if (params.length >= 4 && params[1].equals("CN")
                    && params[2].equals("ipv4") && !params[3].equals("*")) {

                    String baseIP = params[3];
                    int prefixlen =
                        32 - (int) (log(Integer.parseInt(params[4]), 2));
                    String prefix = baseIP + "/" + prefixlen;
                    IPv4Network networks;
                    if (prefixlen > 24) {
                        networks = new IPv4Network(baseIP + "/24");
                    } else {
                        networks = new IPv4Network(prefix);
                    }
                    int amount = 1<<(32-prefixlen);
                    for (String subnet : networks.getSubnet(24)) {
                        String startIP = subnet.split("/")[0];
                        String masklen = subnet.split("/")[1];
                        String randomIP = IPUtil
                            .getRandomIp(startIP, Integer.parseInt(masklen));
                        IpData ipData = null;
                        if (ipData == null) {
                            while (ipData == null) {
                                try {
                                    ipData = queryFromTaobao(randomIP);
                                } catch (Exception e) {
                                    logger.error(
                                        "queryFromTaobao exception: " + e
                                            .getMessage(), e);
                                }
                            }
                        }
                        ipData.setIpAmount(amount);
                        ipData.setNetwork(subnet);
                        logger.info(ipData.toFileString());
                        cnTree.put(subnet, ipData);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                cnTree.writeRawToFile(OUT_PATH + CN_OUT_ORIGINAL);
                cnTree.merge();
                cnTree.writeRawToFile(OUT_PATH + CN_OUT_MERGED);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return cnTree;
    }

    private IpData queryFromTaobao(String ip) throws Exception {
        while (!limitRate.check()) {
            Thread.sleep(100);
        }
        long now = System.currentTimeMillis();
        String ret = HttpClientPool.getInstance().getMethod(TAOBAO_URL + "?ip=" + ip, 5000);
        limitRate.update(now);
        if (ret == null) {
            return null;
        } else {
            JSONObject json = JSON.parseObject(ret);
            if (json.getInteger("code") == 0) {
                JSONObject dataJson = json.getJSONObject("data");
                IpData ipData = new IpData();
                ipData.setCountry(dataJson.getString("country"));
                ipData.setProvince(dataJson.getString("region"));
                ipData.setCity(dataJson.getString("city"));
                ipData.setIsp(dataJson.getString("isp"));
                ipData.setIp(ip);
                return ipData;
            } else {
                return null;
            }
        }
    }

    public static double log(double value, double base) {
        return Math.log(value) / Math.log(base);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            IN_PATH = args[0];
            OUT_PATH = args[1];
            Crawler crawler = new Crawler();
            crawler.scanCNIP();
            crawler.scanFNIP();
            IPv4RadixTree retTree = new IPv4RadixTree();
            retTree.loadFromLocalFile(OUT_PATH + FN_OUT_MERGED);
            retTree.loadFromLocalFile(OUT_PATH + CN_OUT_MERGED);
            retTree.merge();
            retTree.writeRawToFile(OUT_PATH + "ipdb.dat");
            logger.info("finish");
        } else {
            System.out.println("miss param, abandon !!!");
            System.exit(1);
        }
    }
}
