package org.eclipse.leshan.server.demo.mt.scheduler;

public class RequestPayload implements Comparable<RequestPayload> {
    public final String mLink;
    public final String mPayload;
    public final long mTimeMs;
    
    public RequestPayload(String Link, String payload, long timMs) {
        this.mLink = Link;
        this.mPayload = payload;
        this.mTimeMs = timMs;
    }

    @Override
    public int compareTo(RequestPayload o) {
        if(o.mTimeMs > this.mTimeMs) {
            return -1;    
        } else if(o.mTimeMs < this.mTimeMs) {
            return 1;    
        } 
        return 0;
    }
} 