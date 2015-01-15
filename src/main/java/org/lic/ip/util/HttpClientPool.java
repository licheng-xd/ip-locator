package org.lic.ip.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.CodingErrorAction;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpClientPool {
    private static Logger logger = LoggerFactory
        .getLogger(HttpClientPool.class);

    private static int defaultHttpTimeoutThreshold = 1000;

    private static class HttpClientPoolHolder {
        static final HttpClientPool INSTANCE = new HttpClientPool();
    }

    private CloseableHttpClient client;

    MessageConstraints messageConstraints = MessageConstraints.custom()
        .setMaxHeaderCount(200).setMaxLineLength(2000).build();

    // connection config
    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setMalformedInputAction(CodingErrorAction.IGNORE)
        .setUnmappableInputAction(CodingErrorAction.IGNORE)
        .setCharset(Consts.UTF_8).setMessageConstraints(messageConstraints)
        .build();

    // request config
    RequestConfig defaultRequestConfig = RequestConfig.custom()
        .setSocketTimeout(defaultHttpTimeoutThreshold)
        .setConnectTimeout(defaultHttpTimeoutThreshold)
        .setConnectionRequestTimeout(defaultHttpTimeoutThreshold).build();

    private HttpClientPool() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(50);
        cm.setMaxTotal(200);

        cm.setDefaultConnectionConfig(connectionConfig);

        client = HttpClients.custom().setConnectionManager(cm)
            .setDefaultRequestConfig(defaultRequestConfig).build();

    }

    static public HttpClientPool getInstance() {
        return HttpClientPoolHolder.INSTANCE;
    }

    public StatusLine getMethod(String url) throws ClientProtocolException,
        IOException {
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = client.execute(get);
            StatusLine sl = response.getStatusLine();
            EntityUtils.consume(response.getEntity());
            return sl;
        } finally {
            get.releaseConnection();
        }

    }

    public String getMethod(String url, int timeout)
        throws ClientProtocolException, IOException {
        HttpGet get = new HttpGet(url);
        try {
            RequestConfig requestConfig = RequestConfig
                .copy(defaultRequestConfig).setSocketTimeout(timeout).build();
            get.setConfig(requestConfig);
            HttpResponse response = client.execute(get);
            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, "utf-8");
            } else {
                logger.error("req url failed, url: {} ,retcode: {}", url,
                    sl.getStatusCode());
                EntityUtils.consume(response.getEntity());
            }

        } finally {
            get.releaseConnection();
        }
        return null;
    }

    public String getMethod(String url, int timeout, int retryTimes) {
        String ret = null;
        for (int i = 0; i < retryTimes; i++) {
            try {
                ret = getMethod(url, timeout);
                if (ret == null)
                    continue;
            } catch (Exception e) {
                logger.warn("getMethod fail, left retry times: "
                    + (retryTimes - i - 1), e);
            }

            if (ret != null)
                break;
        }
        return ret;
    }

    /**
     * 邮箱插件相关post请求
     * 
     * @param url
     *            要请求的URL
     * @param params
     *            对应的请求参数
     * @return
     * @throws ClientProtocolException
     * @throws java.io.IOException
     */
    public String postMail(String url, Map<String, String> params)
        throws ClientProtocolException, IOException {
        return postMail(url, params.entrySet(), null, false);
    }

    /**
     * 邮箱插件相关post请求
     *
     * @param url
     *            要请求的URL
     * @param params
     *            对应的请求参数
     * @param timeout
     *            请求超时时间
     * @return
     * @throws ClientProtocolException
     * @throws java.io.IOException
     */
    public String postMail(String url, Map<String, String> params, int timeout)
        throws ClientProtocolException, IOException {
        return postMail(url, params.entrySet(), timeout, false);
    }

    public String postMail(String url, Map<String, String> params, int timeout,
        boolean setContentType) throws ClientProtocolException, IOException {
        return postMail(url, params.entrySet(), timeout, setContentType);
    }

    public String postMail(String url, Set<Map.Entry<String, String>> params,
        Integer timeout) throws ClientProtocolException, IOException {
        return postMail(url, params, timeout, false);
    }

    /**
     * 邮箱插件相关post请求
     *
     * @param url
     *            要请求的URL
     * @param params
     *            对应的请求参数
     * @param timeout
     *            请求超时时间
     * @return
     * @throws ClientProtocolException
     * @throws java.io.IOException
     */
    public String postMail(String url, Set<Map.Entry<String, String>> params,
        Integer timeout, boolean setContentType)
        throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(url);
        try {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> entry: params) {
                nvps.add(new BasicNameValuePair(entry.getKey(), entry
                    .getValue()));
            }
            // 超时判断
            if (timeout == null) {
                timeout = defaultHttpTimeoutThreshold;
            }

            RequestConfig requestConfig = RequestConfig
                .copy(defaultRequestConfig).setSocketTimeout(timeout).build();
            post.setConfig(requestConfig);
            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            if (setContentType) {
                //有些URL不支持。
                post.addHeader("content-type",
                    "application/x-www-form-urlencoded");
            }
            HttpResponse response = client.execute(post);

            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, "utf-8");
            } else {
                logger.error("req url failed, url: {} ,retcode: {}", url,
                    sl.getStatusCode());
                EntityUtils.consume(response.getEntity());
            }

            return null;
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * cookies中解析出的
     *
     * @param headers
     * @return
     */
    private String getMainAccount(Header[] headers) {
        for (Header h: headers) {
            String value = h.getValue();
            if (value.contains("P_INFO")) {

                int index = value.indexOf("=");
                int len = value.indexOf("|");
                if (len > index) {
                    return value.substring(index + 1, len);
                }
            }
        }
        return null;
    }

    /**
     * 提交一个post请求，类型是application/json
     *
     * @param url
     * @param jsonContent
     *            json格式的字符串
     * @return
     * @throws ClientProtocolException
     * @throws java.io.IOException
     */
    public String postJson(String url, String jsonContent)
        throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(url);
        try {
            StringEntity requestEntity = new StringEntity(jsonContent, "utf-8");
            requestEntity.setContentType("application/json");
            post.setEntity(requestEntity);

            HttpResponse response = client.execute(post);
            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, "utf-8");
            } else {
                logger.error("req url failed, url: {} ,retcode: {}", url,
                    sl.getStatusCode());
                EntityUtils.consume(response.getEntity());
            }
            return null;
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * @param url
     * @param params
     * @return
     * @throws ClientProtocolException
     * @throws java.io.IOException
     */
    public String postMethod(String url, Map<String, Object> params)
        throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(url);
        try {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for (Map.Entry<String, Object> entry: params.entrySet()) {
                nvps.add(new BasicNameValuePair(entry.getKey(), entry
                    .getValue().toString()));
            }

            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            HttpResponse response = client.execute(post);
            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, "utf-8");
            } else {
                logger.error("req url failed, url: {} ,retcode: {}", url,
                    sl.getStatusCode());
                EntityUtils.consume(response.getEntity());
            }

            return null;
        } finally {
            post.releaseConnection();
        }
    }

    public String postMethod(String url, Map<String, Object> params, int timeout)
        throws ClientProtocolException, IOException {
        return postMethod(url, params, timeout, false);
    }

    /**
     * @param url
     * @param params
     * @param timeout
     * @param useContentType
     *            有些URL接口不支持contenttype不设置的请求
     * @return
     * @throws ClientProtocolException
     * @throws java.io.IOException
     */
    public String postMethod(String url, Map<String, Object> params,
        int timeout, boolean useContentType) throws ClientProtocolException,
        IOException {
        HttpPost post = new HttpPost(url);
        try {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for (Map.Entry<String, Object> entry: params.entrySet()) {
                nvps.add(new BasicNameValuePair(entry.getKey(), entry
                    .getValue().toString()));
            }
            RequestConfig requestConfig = RequestConfig
                .copy(defaultRequestConfig).setSocketTimeout(timeout).build();
            post.setConfig(requestConfig);

            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            if (useContentType) {
                post.addHeader("content-type",
                    "application/x-www-form-urlencoded");
            }
            HttpResponse response = client.execute(post);
            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, "utf-8");
            } else {
                logger.error("req url failed, url: {} ,retcode: {}", url,
                    sl.getStatusCode());

                EntityUtils.consume(response.getEntity());

            }
            return null;
        } finally {
            post.releaseConnection();
        }
    }

    public String antiSpamPost(String url, Map<String, Object> params)
        throws ClientProtocolException, IOException {
        // HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(url);
        try {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for (Map.Entry<String, Object> entry: params.entrySet()) {
                nvps.add(new BasicNameValuePair(entry.getKey(), entry
                    .getValue().toString()));
            }
            // todo 临时代码, 先强制把超时时间设短
            RequestConfig requestConfig = RequestConfig
                .copy(defaultRequestConfig).setSocketTimeout(200)
                .setConnectTimeout(200).build();
            post.setConfig(requestConfig);
            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            HttpResponse response = client.execute(post);
            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, "utf-8");
            } else {
                logger.error("req url failed, url: {} ,retcode: {}", url,
                    sl.getStatusCode());
                EntityUtils.consume(response.getEntity());
            }

            return null;
        } finally {
            post.releaseConnection();
        }

    }

    public String postMethod(String url, String entity)
        throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(url);
        try {
            HttpEntity str = new StringEntity(entity, "UTF-8");
            post.setEntity(str);

            HttpResponse response = client.execute(post);
            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity respEntity = response.getEntity();
                return EntityUtils.toString(respEntity, "utf-8");
            } else {
                logger.error("req url failed, url: {} ,retcode: {}", url,
                    sl.getStatusCode());

                EntityUtils.consume(response.getEntity());

            }

            return null;
        } finally {
            post.releaseConnection();
        }
    }

    public String postMethod(String url, String entity, int timeout)
        throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(url);
        try {
            RequestConfig requestConfig = RequestConfig
                .copy(defaultRequestConfig).setSocketTimeout(timeout).build();
            post.setConfig(requestConfig);

            HttpEntity str = new StringEntity(entity, "UTF-8");
            post.setEntity(str);

            HttpResponse response = client.execute(post);
            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity respEntity = response.getEntity();
                return EntityUtils.toString(respEntity, "utf-8");
            } else {
                logger.error("req url failed, url: {} ,retcode: {}", url,
                    sl.getStatusCode());

                EntityUtils.consume(response.getEntity());

            }

            return null;
        } finally {
            post.releaseConnection();
        }
    }

    public String execute(HttpRequestBase post) throws ClientProtocolException,
        IOException {
        try {

            HttpResponse response = client.execute(post);
            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                logger.info("Http execute success");
                HttpEntity respEntity = response.getEntity();
                return EntityUtils.toString(respEntity, "utf-8");
            } else {
                logger.error("req url failed, url: " + post.getURI().toString()
                    + ",retcode: " + sl.getStatusCode());
                EntityUtils.consume(response.getEntity());
            }

            return null;
        } finally {
            post.releaseConnection();
        }
    }

    public String execute(HttpRequestBase post, int timeout)
        throws ClientProtocolException, IOException {
        try {
            RequestConfig requestConfig = RequestConfig
                .copy(defaultRequestConfig).setSocketTimeout(timeout).build();
            post.setConfig(requestConfig);
            HttpResponse response = client.execute(post);
            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                logger.info("Http execute success");
                HttpEntity respEntity = response.getEntity();
                return EntityUtils.toString(respEntity, "utf-8");
            } else {
                logger.error("req url failed, url: " + post.getURI().toString()
                    + ",retcode: " + sl.getStatusCode());
                EntityUtils.consume(response.getEntity());
            }
            return null;
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * httpsync特有execute,需要特殊处理下返回码code.
     *
     * @param post
     * @return
     * @throws ClientProtocolException
     * @throws java.io.IOException
     */
    public String httpSyncExecute(HttpRequestBase post)
        throws ClientProtocolException, IOException {
        try {

            HttpResponse response = client.execute(post);
            StatusLine sl = response.getStatusLine();
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                logger.info("Http execute success");
                HttpEntity respEntity = response.getEntity();
                return EntityUtils.toString(respEntity, "utf-8");
            } else {
                logger.info("req url failed, url: " + post.getURI().toString()
                    + ",retcode: " + sl.getStatusCode());
                // httpsync访问内部应用时，直接HttpStatus.400了，对应的code放在了entity了。
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, "utf-8");
            }
        } finally {
            post.releaseConnection();
        }
    }

    public static void main(final String[] args)
        throws ClientProtocolException, IOException {
        final String[] uris = { "http://hc.apache.org/",
            "http://hc.apache.org/httpcomponents-core-ga/",
            "http://hc.apache.org/httpcomponents-client-ga/",
            "http://svn.apache.org/viewvc/httpcomponents/",
            "http://www.baidu.com", "http://www.google.com",
            "http://www.youdao.com", "http://www.soso.com",
            "http://www.sogou.com", "http://www.badurltest.com",
            "http://192.168.164.95", };
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("1", "2");
        ExecutorService es = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            es.execute(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        String url = uris[(int) (Thread.currentThread().getId() % uris.length)];
                        logger.debug("begin post {}", url);
                        String ret = HttpClientPool.getInstance().postMethod(
                            url, params, 1000);
                        System.out.println(ret);
                    } catch (ClientProtocolException e) {
                        // TODO Auto-generated catch block
                        logger.debug(e.getMessage(), e);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        logger.debug(e.getMessage(), e);
                    }
                }
            });
        }

    }
}
