package com.paritytrading.parity.wsreporter;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.dsl.Disruptor;

public class MarketEventProducer extends TradeListener {
    private final Disruptor<MarketEvent> disruptor;

    public MarketEventProducer(Disruptor<MarketEvent> disruptor) {
        this.disruptor = disruptor;
    }

    private static final EventTranslatorOneArg<MarketEvent, Trade> TRANSLATOR_ONE_ARG =
            new EventTranslatorOneArg<MarketEvent, Trade>() {
                public void translateTo(MarketEvent evt, long sequence, Trade message) {
                    evt.set(message);
                }
            };

    public void onData(Trade message) {
        // publish the message to disruptor
        disruptor.publishEvent(TRANSLATOR_ONE_ARG, message);
    }

    @Override
    public void trade(Trade event) {
        // insert into circle buffer
        // publish the messages via event producer
        this.onData(event);
    }
}
