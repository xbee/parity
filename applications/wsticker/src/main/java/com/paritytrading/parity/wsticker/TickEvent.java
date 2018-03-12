package com.paritytrading.parity.wsticker;

import java.util.HashMap;
import java.util.Map;
import com.lmax.disruptor.EventFactory;
import org.json.simple.JSONObject;

public class TickEvent {

    private Tick val;

    public void set(Tick right) {
        this.val = right;
    }

    public Tick get() {
        return this.val;
    }

    public void clear() {
        val = null;
    }

    public final static EventFactory<TickEvent> EVENT_FACTORY = new EventFactory<TickEvent>() {
        public TickEvent newInstance() {
            return new TickEvent();
        }
    };



}
