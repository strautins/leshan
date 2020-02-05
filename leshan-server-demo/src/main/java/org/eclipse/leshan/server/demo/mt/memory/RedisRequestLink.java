package org.eclipse.leshan.server.demo.mt.memory;

import com.eclipsesource.json.JsonObject;
import com.google.gson.Gson;

public class RedisRequestLink {
    private static final String R = "R";
    private static final String W = "W";
    private static final String E = "E";
    private Object mValue;
    private String mAction;
    private String mObjectId;
    private String mInstanceId;
    private String mResourceId;
    private JsonObject mError = null; 
    private JsonObject mRequest;
    private JsonObject mResponse;
    public RedisRequestLink(String link, String info) {
        //R:43000:0:1
        String[] links = link.split(":");
        if(links.length >= 0 && (R + W + E).contains(links[0].toUpperCase())) {
            this.mAction = links[0].toUpperCase();
        } else {
            mError = new JsonObject();
            mError.set("error", "Action unknown " +  links[0].toUpperCase());
        }
        if(links.length > 1) {
            this.mObjectId = links[1];
        }
        if(links.length > 2) {
            this.mInstanceId = links[2];
        }
        if(links.length == 4) {
            this.mResourceId = links[3];
        }else if(links.length > 3) {
            mError = new JsonObject();
            mError.set("error", "Incorrect link separator count! " + links.length);
        }

        if(!isError()) {
            try {
                mRequest = new Gson().fromJson(info == null ? "{}" : info, JsonObject.class);
                // Iterator<String> keys = mRequest.ge
                // while(keys.hasNext()) {
                //     switch (keys.next()) {
                //         case "values":
                //             mValue = mRequest.get("values");
                //             break;
                //         case "value":
                //             mValue = mRequest.get("value");
                //             break;
                //     }
                // } 
            } catch (Exception e) {
                mError = new JsonObject();
                mError.set("error", "JSON parse ERROR:" + info + " / " + e.getMessage());
                //todo return with json parse error
            } 
        }

        if(!isError() && isWrite() && this.mResourceId == null) {
            mError = new JsonObject();
            mError.set("error", "In Write operation full link missing! " + info);   
        }
    }
    public boolean isRead() {
        return mAction.equals(R);     
    }
    public boolean isWrite() {
        return mAction.equals(W);     
    }
    public boolean isExecute() {
        return mAction.equals(E);     
    }
    public int getObjectId() {
        return Integer.valueOf(this.mObjectId);
    }
    public int getInstanceId() {
        return Integer.valueOf(this.mInstanceId);
    }
    public int getResourceId() {
        return Integer.valueOf(this.mResourceId);
    }
    public String getLink() {
        return this.mObjectId + 
            ifNotNull(this.mInstanceId, "/") +
            ifNotNull(this.mResourceId, "/");
    }
    public Object getValue() {
        return this.mValue;
    }
    public boolean isError() {
        return this.mError != null;
    }
    public void setResponse(String response) {
        // setResponse(new JsonObject(response));
    }
    public void setResponse(JsonObject response) {
        mResponse = response;
        mResponse.set("tim", System.currentTimeMillis());
    }
    public String getResponse() {
        if(isError()) {
            return mError.toString();   
        } else {
            JsonObject res = new JsonObject();
            res.set("request", mRequest);
            res.set("response", mResponse);
            return res.toString(); 
        }
    }
    public static int boolToInt(Boolean b) {
        return b ? 1 : 0;
    }
    // public static int countChar(String string, String b) {
    //     return string.length() - string.replace(b, "").length();
    // }
    public static String ifNotNull(String string, String prefix) {
        return string != null ? (prefix + string) : "";
    }
} 