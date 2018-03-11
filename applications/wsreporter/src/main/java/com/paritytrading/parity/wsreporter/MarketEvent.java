package com.paritytrading.parity.wsreporter;

import com.lmax.disruptor.EventFactory;

import java.util.HashMap;
import java.util.Map;

public class MarketEvent {

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

    public final static EventFactory<MarketEvent> EVENT_FACTORY = new EventFactory<MarketEvent>() {
        public MarketEvent newInstance() {
            return new MarketEvent();
        }
    };
}
