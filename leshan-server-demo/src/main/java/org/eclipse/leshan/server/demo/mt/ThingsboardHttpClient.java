package org.eclipse.leshan.server.demo.mt;

import java.io.IOException;
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

public class ThingsboardHttpClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(OnConnectAction.class);
    
    private final String mHost;
    private final Integer mPort;
    private static final String mLink1 = "/api/v1/";
    private static final String mLink2 = "/telemetry";
    public ThingsboardHttpClient(String host, Integer port) throws URISyntaxException {
        LOG.warn("Created ThingsboardHttpClient at {} : {} : {}", System.currentTimeMillis(), host, port);
        this.mHost = host;
        this.mPort = port;
    }
    public void post2ThingsBoard(String cleanEndpoint,  ArrayList<String> payLoadList) throws IOException, InterruptedException {
        //"http://123.123.123.123:8080/api/v1/" + cleanEndpoint + "/telemetry"
        ArrayList<HttpPost> httpPostList = new ArrayList<HttpPost>();
        for(String payload: payLoadList) {
            HttpPost request = new HttpPost("http://" + mHost + ":" + String.valueOf(mPort) + mLink1 + cleanEndpoint + mLink2);
            StringEntity params = new StringEntity(payload);
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            httpPostList.add(request);
        }   
       
        RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(3000)
            .setConnectTimeout(3000).build();
        CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .build();
        try {
            httpClient.start();
            final CountDownLatch latch = new CountDownLatch(payLoadList.size());
            for (final HttpPost request: httpPostList) {
                httpClient.execute(request, new FutureCallback<HttpResponse>() {

                    @Override
                    public void completed(final HttpResponse response) {
                        latch.countDown();
                        LOG.warn("Http completed at {} : {}", System.currentTimeMillis(), response);
                    }

                    @Override
                    public void failed(final Exception ex) {
                        latch.countDown();
                        LOG.warn("Http failed at {} : {}", System.currentTimeMillis(), ex);
                    }

                    @Override
                    public void cancelled() {
                        latch.countDown();
                        LOG.warn("Http cancelled at {} : {}", System.currentTimeMillis(), request.getRequestLine());
                    }
                });
            }
            latch.await();
            LOG.warn("Http Shutting down at {} : {}", System.currentTimeMillis(), cleanEndpoint);
        } finally {
            httpClient.close();
        }
        LOG.warn("Http done at {} : {}", System.currentTimeMillis(), cleanEndpoint);
    }
}