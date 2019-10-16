package org.eclipse.leshan.server.demo.mt;

import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.interceptors.MessageInterceptor;

public class MyCoapMessageTracer implements MessageInterceptor {

    @Override
    public void sendRequest(Request request) {
        MyCoapMessage msg = new MyCoapMessage(request, false);
    }

    @Override
    public void sendResponse(Response response) {   
        MyCoapMessage msg = new MyCoapMessage(response, false);
    }

    @Override
    public void sendEmptyMessage(EmptyMessage message) {
        MyCoapMessage msg = new MyCoapMessage(message, false);
    }

    @Override
    public void receiveRequest(Request request) {
        MyCoapMessage msg = new MyCoapMessage(request, true);
    }

    @Override
    public void receiveResponse(Response response) {      
        MyCoapMessage msg = new MyCoapMessage(response, true);

    }

    @Override
    public void receiveEmptyMessage(EmptyMessage message) {
        MyCoapMessage msg = new MyCoapMessage(message, true);
    }
}
