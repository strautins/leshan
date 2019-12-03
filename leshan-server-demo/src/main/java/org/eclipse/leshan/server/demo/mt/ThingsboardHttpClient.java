package org.eclipse.leshan.server.demo.mt;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThingsboardHttpClient implements ThingsboardSend {

    private static final Logger LOG = LoggerFactory.getLogger(ThingsboardHttpClient.class);

    private final CloseableHttpAsyncClient httpClient;
    private final String HOST;
    private final Integer PORT;
    private final Integer TIMEOUT;

    private static final String mLink1 = "/api/v1/";
    private static final String mLink2 = "/telemetry";

    public ThingsboardHttpClient(String host, Integer port, int timeoutSec) throws URISyntaxException {
        LOG.warn("Created ThingsboardHttpClient at {} : {} : {} : {}", System.currentTimeMillis(), host, port, timeoutSec);
        this.HOST = host;
        this.PORT = port;
        this.TIMEOUT = timeoutSec;

        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(this.TIMEOUT).setConnectTimeout(this.TIMEOUT).build();
        this.httpClient = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).build();
    }
    @Override
    public void start() {
        httpClient.start();
    }

    @Override
    public void stop() {
        try {
            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(String token, ArrayList<String> msg) {
        try {
            localSend(token, msg);
        } catch (InterruptedException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void localSend(String token, ArrayList<String> msg) throws InterruptedException, IOException {
        //"http://123.123.123.123:8080/api/v1/" + token + "/telemetry"
        ArrayList<HttpPost> httpPostList = new ArrayList<HttpPost>();
        for(String payload: msg) {
            HttpPost request = new HttpPost(this.HOST + ":" + String.valueOf(this.PORT) + mLink1 + token + mLink2);
            StringEntity params = new StringEntity(payload);
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            httpPostList.add(request);
        }   
       
        final CountDownLatch latch = new CountDownLatch(msg.size());
        for (final HttpPost request: httpPostList) {
            httpClient.execute(request, new FutureCallback<HttpResponse>() {

                @Override
                public void completed(final HttpResponse response) {
                    LOG.debug("Http completed at {} : {}", System.currentTimeMillis(), response);
                    latch.countDown();
                }

                @Override
                public void failed(final Exception ex) {
                    LOG.debug("Http failed at {} : {}", System.currentTimeMillis(), ex);
                    latch.countDown();
                }

                @Override
                public void cancelled() {
                    LOG.debug("Http cancelled at {} : {}", System.currentTimeMillis(), request.getRequestLine());
                    latch.countDown();
                }
            });
        }
        latch.await();
        LOG.debug("Http done at {} : {}", System.currentTimeMillis(), token);
    }
}