package com.paritytrading.parity.wsreporter;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.json.simple.JSONObject;

class Trade {

    public String timestamp;
    public long   matchNumber;
    public String instrument;
    public long   quantity;
    public long   price;
    public String buyer;
    public long   buyOrderNumber;
    public String seller;
    public long   sellOrderNumber;

    public String toString() {
        return String.format("%s, %s, %d, %d, %5.8ld, %s, %s",
                this.timestamp,
                this.instrument,
                this.matchNumber,
                this.quantity,
                this.price,
                this.buyOrderNumber,
                this.sellOrderNumber);
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("timestamp", this.timestamp);
        obj.put("instrument", this.instrument);
        obj.put("matchNumber", this.matchNumber);
        obj.put("quantity", this.quantity);
        obj.put("price", this.price);
        obj.put("buyOrderNumber", this.buyOrderNumber);
        obj.put("sellOrderNumber", this.sellOrderNumber);
        return obj;
    }

    public static Trade randomTradeEvent() {
        Trade t = new Trade();
        t.timestamp = String.format("%d", System.currentTimeMillis() / 1000);
//        t.matchNumber = UUID.randomUUID()
        t.instrument = "BTC-USD";
        ThreadLocalRandom ran = ThreadLocalRandom.current();
        t.quantity = ran.nextInt(1, 20 + 1);
        t.price = 10000 * ran.nextLong(0, 2);
        t.seller = UUID.randomUUID().toString();
        t.buyer = UUID.randomUUID().toString();
        return t;
    }

}
