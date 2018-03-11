package com.paritytrading.parity.wsreporter;

import com.lmax.disruptor.EventFactory;

public class PMREventFactory implements EventFactory<PMREvent> {

    public PMREvent newInstance() {
        return new PMREvent();
    }
}
