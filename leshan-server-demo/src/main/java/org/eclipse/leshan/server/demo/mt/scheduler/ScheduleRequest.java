package org.eclipse.leshan.server.demo.mt.scheduler;

//import org.eclipse.leshan.core.node.LwM2mMultipleResource;
//import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;

public class ScheduleRequest {

    public static enum ActionType {
        read, write, execute, observe
    }

    private LwM2mPath mLwM2mPath;
    private ActionType mAction;
    private LwM2mResource mLwM2mResource;
    private LwM2mNode mReadResponse;

    public ScheduleRequest(ActionType action, LwM2mPath resourcePath, LwM2mResource value) {
        this.mAction = action;
        this.mLwM2mPath = resourcePath;
        this.mLwM2mResource = value;
    }

    public String getLink() {
        return mAction.name().substring(0, 1) +
            mLwM2mPath.toString().replace("/", ":");
    }

    public boolean isRead() {
        return mAction.equals(ActionType.read);
    }

    public ReadRequest getReadRequest() {
        if(isRead()) {
            return new ReadRequest(this.mLwM2mPath.toString());   
        }  
        return null;
    }

    public boolean isWrite() {
        return mAction.equals(ActionType.write);     
    }

    public WriteRequest getWriteRequest() {
        if(isWrite()) {
            return new WriteRequest(WriteRequest.Mode.UPDATE, this.mLwM2mPath.getObjectId(),
                this.mLwM2mPath.getObjectInstanceId(), this.mLwM2mResource);
        }  
        return null;
    }

    public boolean isExecute() {
        return mAction.equals(ActionType.execute);     
    }

    public ExecuteRequest getExecuteRequest() {
        if(isExecute()) {
            String value = this.mLwM2mResource != null && this.mLwM2mResource.getValue() != null ? this.mLwM2mResource.getValue().toString() : null;
            return new ExecuteRequest(this.mLwM2mPath.toString(), value);
        }  
        return null;
    }

    public boolean isObserve() {
        return mAction.equals(ActionType.observe);     
    }

    public ObserveRequest getObserveRequest() {
        if(isObserve()) {
            return new ObserveRequest(this.mLwM2mPath.toString());
        }  
        return null;
    }

    public ActionType getAction() {
        return this.mAction;
    }

    public LwM2mPath getLwM2mPath() {
        return this.mLwM2mPath;
    }

    public LwM2mResource getLwM2mResource() {
        return this.mLwM2mResource;
    }

    //todo Validate request
    public boolean isValid() {
        return this.mLwM2mPath != null
        && (
            this.mAction.equals(ActionType.read)
            || 
            this.mAction.equals(ActionType.write)
              && this.mLwM2mResource != null
            ||
            this.mAction.equals(ActionType.execute)
            ||
            this.mAction.equals(ActionType.observe)
        );
    }
    
    public void setActionType(ActionType action) {
        this.mAction = action;
    }

    public void setReadResponse(LwM2mNode node) {
        this.mReadResponse = node;
    }

    public LwM2mNode getReadResponse() {
        return this.mReadResponse;
    }

    public static ActionType getActionType(String action) {
        if(action.equals(ScheduleRequest.ActionType.read.name())) {
            return ScheduleRequest.ActionType.read;
        } else if(action.equals(ScheduleRequest.ActionType.write.name())) {
            return ScheduleRequest.ActionType.write;
        } else if(action.equals(ScheduleRequest.ActionType.execute.name())) {
            return ScheduleRequest.ActionType.execute;
        } else {
            return ScheduleRequest.ActionType.observe;
        }
    } 
} 