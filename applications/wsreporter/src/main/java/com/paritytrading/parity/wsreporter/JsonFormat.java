package com.paritytrading.parity.wsreporter;

import com.paritytrading.parity.util.Instrument;
import com.paritytrading.parity.util.Instruments;
import org.json.simple.JSONObject;
import ws.wamp.jawampa.WampClient;

public class JsonFormat extends TradeListener {

    private Instruments instruments;
    private PMREventProducer producer;

    public JsonFormat(Instruments instruments, PMREventProducer producer) {
        this.instruments = instruments;
        this.producer = producer;
    }

    @Override
    public void trade(Trade event) {
        Instrument instrument = instruments.get(event.instrument);

        // insert into circle buffer
        if (producer != null ) {
            // publish the messages via event producer
            producer.onData(event);
        }
    }
}
