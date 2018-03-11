package com.paritytrading.parity.wsreporter;

import java.util.HashMap;
import java.util.Map;

public class PMREvent {

    private Trade val;

    public void set(Trade right) {
        this.val = right;
    }

    public Trade get() {
        return this.val;
    }

    public void clear() {
        val = null;
    }

    public Map<String, Object> getMap() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("timestamp", val.timestamp);
        data.put("instrument", val.instrument);

        return data;
    }
}
