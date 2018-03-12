package com.paritytrading.parity.wsticker;

import org.json.simple.JSONObject;

public class Tick {

    public String timestamp;
    public String instrument;
    public long   bidPrice;
    public long   bidSize;
    public long   askPrice;
    public long   askSize;
    public long   lastPrice;
    public long   lastSize;

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("timestamp", this.timestamp);
        obj.put("instrument", this.instrument);
        obj.put("bidPrice", this.bidPrice);
        obj.put("bidSize", this.bidSize);
        obj.put("askPrice", this.askPrice);
        obj.put("askSize", this.askSize);
        obj.put("lastPrice", this.lastPrice);
        obj.put("lastSize", this.lastSize);

        return obj;
    }
}
