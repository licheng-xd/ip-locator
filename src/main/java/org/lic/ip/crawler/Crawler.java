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

/**
 * Created by lc on 15/1/8.
 */
public class Crawler {
    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    private static final String TAOBAO_URL = "http://ip.taobao.com/service/getIpInfo.php";

    private LimitRate limitRate = new LimitRate(1000L, 10);

    private IPv4RadixTree oldTree = new IPv4RadixTree();

    private IPv4RadixTree newTree = new IPv4RadixTree();

    private static String INPUT = "/Users/lc/github/ipdb_creator/input/delegated-apnic-test";

    private static String OUTPUT = "/Users/lc/github/ipdb_creator/output/delegated-cn-original";

    public void scanCNIP() {
        try {
            BufferedReader reader = new BufferedReader(
                new FileReader(new File(INPUT)));
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
                        Data data = oldTree.selectValue(randomIP);
                        if (data == null) {
                            while (data == null) {
                                try {
                                    data = queryFromTaobao(randomIP);
                                } catch (Exception e) {
                                    logger.error(
                                        "queryFromTaobao exception: " + e
                                            .getMessage(), e);
                                }
                            }
                        }
                        data.setIpAmount(amount);
                        data.setNetwork(subnet);
                        logger.info(data.toFileString());
                        newTree.put(subnet, data);
                    }
                }
            }
            reader.close();

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                newTree.writeRawToFile(OUTPUT);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private Data queryFromTaobao(String ip) throws Exception {
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
                Data data = new Data();
                data.setCountry(dataJson.getString("country"));
                data.setProvince(dataJson.getString("region"));
                data.setCity(dataJson.getString("city"));
                data.setIsp(dataJson.getString("isp"));
                data.setIp(ip);
                return data;
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
            INPUT = args[0];
            OUTPUT = args[1];
            new Crawler().scanCNIP();
        } else {
            System.out.println("miss param, abandon !!!");
            System.exit(1);
        }
//        Crawler crawler = new Crawler();
//        for (int i=0; i<100; i++) {
//            System.out.println(crawler.queryFromTaobao(i + "."));
//        }
    }
}
