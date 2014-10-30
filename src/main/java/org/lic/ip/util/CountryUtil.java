package org.lic.ip.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CountryUtil {

    private static final Logger logger = LoggerFactory
        .getLogger(CountryUtil.class);

    private static final Set<String> CHINA = new HashSet<String>(Arrays.asList(
        "北京市", "天津市",
        "上海市",
        "重庆市",
        // 5个自治区
        "内蒙古自治区",
        "新疆维吾尔自治区",
        "西藏自治区",
        "宁夏回族自治区",
        "广西壮族自治区",
        // 香港特别行政区（港）
        // 澳门特别行政区（澳）
        // 23个省
        "黑龙江省", "吉林省", "辽宁省", "河北省", "山西省", "青海省", "山东省", "河南省", "江苏省", "安徽省",
        "浙江省", "福建省", "江西省", "湖南省", "湖北省", "广东省", "台湾省", "海南省", "甘肃省", "陕西省",
        "四川省", "贵州省", "云南省",

        "北京", "天津",
        "上海",
        "重庆",
        // 5个自治区
        "内蒙古",
        "新疆",
        "西藏",
        "宁夏",
        "广西",
        // 香港特别行政区（港）
        // 澳门特别行政区（澳）
        // 23个省
        "黑龙江", "吉林", "辽宁", "河北", "山西", "青海", "山东", "河南", "江苏", "安徽",
        "浙江", "福建", "江西", "湖南", "湖北", "广东", "台湾", "海南", "甘肃", "陕西",
        "四川", "贵州", "云南",

        "unknown_country"));

    private static Set<String> SPECIFIED_COUNTRY_SET = new HashSet<String>();

    public static boolean isChina(String input) {
        return CHINA.contains(input);
    }

    public static void reloadSpecifiedCountry(String countryList) {
        String[] countryArray = countryList.split(",");
        Set<String> newCountrySet = new HashSet<String>();
        for (String ip: countryArray) {
            if (!ip.isEmpty())
                newCountrySet.add(ip);
        }
        // 配置发生变化时才更新
        if (!newCountrySet.equals(SPECIFIED_COUNTRY_SET)) {
            logger.info("reloadSpecifiedCountry: {} -> {}",
                SPECIFIED_COUNTRY_SET, newCountrySet);
            SPECIFIED_COUNTRY_SET = newCountrySet;
        }
    }

    /**
     * 判断是否属于指定的国家
     */
    public static boolean isInSpecifiedCountry(String input) {
        return SPECIFIED_COUNTRY_SET.contains(input);
    }
}
