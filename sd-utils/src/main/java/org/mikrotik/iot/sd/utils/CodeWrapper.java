package org.mikrotik.iot.sd.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class CodeWrapper {

    private EventCode mCode;  

    public static enum OutputTriggerType {
        EQUAL_OR_GREATER, LOWER
    }

    public static enum OutputLogic {
        OR, AND
    }

    public static enum OutputPolarity {
        HIGH, LOW
    }

    public static enum EventTriggerType {
        UP, DOWN, BOTH
    }

    public static enum EventCode {
        NO_EVENT, TEMP_EVENT, HUMID_EVENT, PRESS_EVENT, CO2_EVENT, IAQ_EVEN,
        CRIT_BAT, BATTERY_PROB, NOT_REACHABLE, DAMPER, EXT_PWR, LOW_BT_SIGNAL, LOW_CEL_SIGNAL, ALARM
    }

    public static final Map<OutputLogic, String> LOGIC_BIT_LEVEL;
    public static final Map<String, OutputLogic> LOGIC_BIT_LEVEL_VALUE;

    public static final Map<OutputPolarity, String> POLARITY_BIT_LEVEL;
    public static final Map<String, OutputPolarity> POLARITY_BIT_LEVEL_VALUE;

    public static final Map<OutputTriggerType, String> TYPE_BIT_LEVEL;
    public static final Map<String, OutputTriggerType> TYPE_BIT_LEVEL_VALUE;

    public static final Map<EventCode, Integer> EVENT_CODE;
    public static final Map<Integer, EventCode> EVENT_CODE_VALUE;
    
    public static final Map<EventCode, Boolean> IMMEDIATE_NOTIFY;

    public static final Map<Integer, EventTriggerType> EVENT_TRIGGER_VALUE;
    public static final Map<EventTriggerType, Integer> EVENT_TRIGGER;

    static {
        Map<EventCode, Integer> ev = new HashMap<EventCode, Integer>();
        ev.put(EventCode.NO_EVENT, 0);
        ev.put(EventCode.CRIT_BAT, 1);
        ev.put(EventCode.BATTERY_PROB, 2);
        ev.put(EventCode.NOT_REACHABLE, 3);
        ev.put(EventCode.DAMPER, 4);
        ev.put(EventCode.EXT_PWR, 5);
        ev.put(EventCode.LOW_BT_SIGNAL, 6);
        ev.put(EventCode.LOW_CEL_SIGNAL, 7);
        ev.put(EventCode.ALARM, 8);

        ev.put(EventCode.TEMP_EVENT, 128); 
        ev.put(EventCode.HUMID_EVENT, 129); 
        ev.put(EventCode.PRESS_EVENT, 130); 
        ev.put(EventCode.CO2_EVENT, 131); 
        ev.put(EventCode.IAQ_EVEN, 132); 
        EVENT_CODE = Collections.unmodifiableMap(ev);

        Map<Integer, EventCode> predefinedEvents = new HashMap<Integer, EventCode>();
        for(Entry<EventCode, Integer> entry : EVENT_CODE.entrySet()) {
            predefinedEvents.put(entry.getValue(), entry.getKey());
        }
        EVENT_CODE_VALUE = Collections.unmodifiableMap(predefinedEvents);

        Map<EventCode, Boolean> imm = new HashMap<EventCode, Boolean>();
        imm.put(EventCode.NO_EVENT, false);
        imm.put(EventCode.CRIT_BAT, true);
        imm.put(EventCode.BATTERY_PROB, true);
        imm.put(EventCode.NOT_REACHABLE, true);
        imm.put(EventCode.DAMPER, true);
        imm.put(EventCode.EXT_PWR, false);
        imm.put(EventCode.LOW_BT_SIGNAL, true);
        imm.put(EventCode.LOW_CEL_SIGNAL, false);
        imm.put(EventCode.ALARM, true);

        // imm.put(EventCode.TEMP_EVENT, true); 
        // imm.put(EventCode.HUMID_EVENT, true); 
        // imm.put(EventCode.PRESS_EVENT, true); 
        // imm.put(EventCode.CO2_EVENT, true); 
        // imm.put(EventCode.IAQ_EVEN, true); 
        IMMEDIATE_NOTIFY = Collections.unmodifiableMap(imm);

        Map<EventTriggerType, Integer> eventTriggerType = new HashMap<EventTriggerType, Integer>();
        eventTriggerType.put(EventTriggerType.UP, 0); 
        eventTriggerType.put(EventTriggerType.DOWN, 1); 
        eventTriggerType.put(EventTriggerType.BOTH, 2); 
        EVENT_TRIGGER = Collections.unmodifiableMap(eventTriggerType);
        Map<Integer, EventTriggerType> eventTriggerValue = new HashMap<Integer, EventTriggerType>();
        for(Entry<EventTriggerType, Integer> entry : EVENT_TRIGGER.entrySet()) {
            eventTriggerValue.put(entry.getValue(), entry.getKey());
        }
        EVENT_TRIGGER_VALUE = Collections.unmodifiableMap(eventTriggerValue);

        Map<OutputTriggerType, String> bitLevel = new HashMap<OutputTriggerType, String>();
        bitLevel.put(OutputTriggerType.EQUAL_OR_GREATER, "1"); 
        bitLevel.put(OutputTriggerType.LOWER, "0"); 
        TYPE_BIT_LEVEL = Collections.unmodifiableMap(bitLevel);
        Map<String, OutputTriggerType> bitLevelValue = new HashMap<String, OutputTriggerType>();
        for(Entry<OutputTriggerType, String> entry : TYPE_BIT_LEVEL.entrySet()) {
            bitLevelValue.put(entry.getValue(), entry.getKey());
        }
        TYPE_BIT_LEVEL_VALUE = Collections.unmodifiableMap(bitLevelValue);

        Map<OutputLogic, String> logicLevel = new HashMap<OutputLogic, String>();
        logicLevel.put(OutputLogic.AND, "1"); 
        logicLevel.put(OutputLogic.OR, "0"); 
        LOGIC_BIT_LEVEL = Collections.unmodifiableMap(logicLevel);
        Map<String, OutputLogic> logicLevelValue = new HashMap<String, OutputLogic>();
        for(Entry<OutputLogic, String> entry : LOGIC_BIT_LEVEL.entrySet()) {
            logicLevelValue.put(entry.getValue(), entry.getKey());
        }
        LOGIC_BIT_LEVEL_VALUE = Collections.unmodifiableMap(logicLevelValue);

        Map<OutputPolarity, String> polarityLevel = new HashMap<OutputPolarity, String>();
        polarityLevel.put(OutputPolarity.HIGH, "1"); 
        polarityLevel.put(OutputPolarity.LOW, "0"); 
        POLARITY_BIT_LEVEL = Collections.unmodifiableMap(polarityLevel);
        Map<String, OutputPolarity> polarityLevelValue = new HashMap<String, OutputPolarity>();
        for(Entry<OutputPolarity, String> entry : POLARITY_BIT_LEVEL.entrySet()) {
            polarityLevelValue.put(entry.getValue(), entry.getKey());
        }
        POLARITY_BIT_LEVEL_VALUE = Collections.unmodifiableMap(polarityLevelValue);
    }

    public CodeWrapper(EventCode code) {
        this.mCode = code;
    }

    public int getId() {
        return EVENT_CODE.get(this.mCode);
    }

    public EventCode getEventCode() {
        return this.mCode;
    }

    public void setEventCode(EventCode ev) {
        this.mCode = ev;
    }

    public void setEventCode(int code) {
        setEventCode(EVENT_CODE_VALUE.get(code));
    }

    public boolean isImmediateNotify() {
        Boolean b = IMMEDIATE_NOTIFY.get(this.mCode);
        return b != null ? b :false; // for custom events in predefined event list!
    }

    //todo: improve performance
    public int[] getInstance(int instances) {
        String arrStr = ByteUtil.byteToString(ByteUtil.intToByte(instances));
        int[] result = new int[arrStr.length() - arrStr.replace("1", "").length()];
        int pos = 0;
        for (int i = -1; (i = arrStr.indexOf("1", i)) != -1; i++) {
            result[pos] = arrStr.length() - 1 - i;
            pos++;
        }
        return result;
    }

    public boolean isValid() {
        return this.mCode != null;
    }
}