package com.paritytrading.parity.wsreporter;

import com.lmax.disruptor.EventHandler;
//import org.fluentd.logger.FluentLogger;

public class PMREventHandler implements EventHandler<PMREvent> {

//    private static FluentLogger LOG = FluentLogger.getLogger("app");

    public void onEvent(PMREvent event, long sequence, boolean endOfBatch) {
        System.out.println("Event: " + event);

//        LOG.log("market", event.getMap());
        // send it to wamp router

    }
}
