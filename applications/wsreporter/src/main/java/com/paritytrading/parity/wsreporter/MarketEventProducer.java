package com.paritytrading.parity.wsreporter;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.dsl.Disruptor;
import com.paritytrading.parity.util.Instrument;
import com.paritytrading.parity.util.Instruments;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import com.savoirtech.logging.slf4j.json.LoggerFactory;

public class MarketEventProducer extends TradeListener {
    private final Disruptor<MarketEvent> disruptor;
    private Instruments instruments;
    private static final Logger logger = LogManager.getLogger("MarketEventProducer");

    public MarketEventProducer(Instruments instruments, Disruptor<MarketEvent> disruptor) {
        this.instruments = instruments;
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
//        logger.debug(event.toJSON());
        Instrument instrument = instruments.get(event.instrument);

        printf(event.toJSON() + "\n");

//        try {
//            if (instrument != null) {
//                printf("%12s %-8s ", event.timestamp, event.instrument);
//                printf(instrument.getSizeFormat(), event.quantity / instrument.getSizeFactor());
//                printf(" ");
//                printf(instrument.getPriceFormat(), event.price / instrument.getPriceFactor());
//                printf(" %-8s %-8s\n", event.buyer, event.seller);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            if (instrument != null) {
//                printf(instrument.toString());
//            }
//            printf(event.toString());
//        }

    }
}
