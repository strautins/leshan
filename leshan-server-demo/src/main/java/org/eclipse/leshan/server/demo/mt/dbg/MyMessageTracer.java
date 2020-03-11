package org.eclipse.leshan.server.demo.mt.dbg;

import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.interceptors.MessageInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyMessageTracer implements MessageInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(MyMessageTracer.class);

    @Override
    public void sendRequest(Request request) {
        LOG.warn("sendRequest at: {}", new MyCoapMessage(request, false).toString());
    }

    @Override
    public void sendResponse(Response response) {
        LOG.warn("sendResponse at: {}", new MyCoapMessage(response, false).toString());
    }

    @Override
    public void sendEmptyMessage(EmptyMessage message) {
        LOG.warn("sendEmptyMessage at: {}", new MyCoapMessage(message, false).toString());
    }

    @Override
    public void receiveRequest(Request request) {
        LOG.warn("receiveRequest at: {}", new MyCoapMessage(request, true).toString());

    }

    @Override
    public void receiveResponse(Response response) {
        LOG.warn("receiveResponse at: {}", new MyCoapMessage(response, true).toString());
    }

    @Override
    public void receiveEmptyMessage(EmptyMessage message) {
        LOG.warn("receiveEmptyMessage at: {}", new MyCoapMessage(message, true).toString());
    }
}
