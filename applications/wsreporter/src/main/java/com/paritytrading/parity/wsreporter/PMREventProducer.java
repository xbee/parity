package com.paritytrading.parity.wsreporter;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

public class PMREventProducer {
    private final Disruptor<PMREvent> disruptor;

    public PMREventProducer(Disruptor<PMREvent> disruptor) {
        this.disruptor = disruptor;
    }

    private static final EventTranslatorOneArg<PMREvent, Trade> TRANSLATOR_ONE_ARG =
            new EventTranslatorOneArg<PMREvent, Trade>() {
                public void translateTo(PMREvent evt, long sequence, Trade message) {
                    evt.set(message);
                }
            };

    public void onData(Trade message) {
        // publish the message to disruptor
        disruptor.publishEvent(TRANSLATOR_ONE_ARG, message);
    }
}
