package com.paritytrading.parity.wsticker;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.dsl.Disruptor;
import com.paritytrading.parity.book.OrderBook;
import com.paritytrading.parity.book.Side;
import com.paritytrading.parity.util.Instrument;
import com.paritytrading.parity.util.Instruments;
import com.paritytrading.parity.util.Timestamps;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;

public class TickEventProducer extends MarketDataListener {

    private final Disruptor<TickEvent> disruptor;
    private Instruments instruments;

    private Long2ObjectArrayMap<TickEventProducer.Trade> trades;

    private long timestamp;

    private int counter;
    private Tick tick;

    public TickEventProducer(Instruments instruments, Disruptor<TickEvent> disruptor) {

        this.trades = new Long2ObjectArrayMap<>();

        for (Instrument instrument : instruments)
            trades.put(instrument.asLong(), new TickEventProducer.Trade());

        this.disruptor = disruptor;
        this.instruments = instruments;
        this.tick = new Tick();
    }

    private static final EventTranslatorOneArg<TickEvent, Tick> TRANSLATOR_ONE_ARG =
            new EventTranslatorOneArg<TickEvent, Tick>() {
                public void translateTo(TickEvent evt, long sequence, Tick message) {
                    evt.set(message);
                }
            };

    public void onData(Tick message) {
        // publish the message to disruptor
        disruptor.publishEvent(TRANSLATOR_ONE_ARG, message);
    }

    @Override
    public void update(OrderBook book, boolean bbo) {
        if (!bbo)
            return;

        Instrument instrument = instruments.get(book.getInstrument());

        long bidPrice = book.getBestBidPrice();
        long bidSize  = book.getBidSize(bidPrice);

        long askPrice = book.getBestAskPrice();
        long askSize  = book.getAskSize(askPrice);

        String priceFormat = instrument.getPriceFormat();
        String sizeFormat  = instrument.getSizeFormat();

        double priceFactor = instrument.getPriceFactor();
        double sizeFactor  = instrument.getSizeFactor();

        tick.instrument = instrument.asString();
        tick.timestamp = String.format("%12s", Timestamps.format(timestampMillis()));

        tick.bidPrice = bidPrice;
        tick.bidSize  = bidSize;

        tick.askPrice = askPrice;
        tick.askSize  = askSize;

        TickEventProducer.Trade trade = trades.get(instrument.asLong());

        tick.lastPrice = trade.price;
        tick.lastSize  = trade.size;

        // create tickevent
        this.onData(tick);

    }

    @Override
    public void trade(OrderBook book, Side side, long price, long size) {
        // get info of last trade
        TickEventProducer.Trade trade = trades.get(book.getInstrument());

        trade.price = price;
        trade.size  = size;
    }

    private static class Trade {
        public long price;
        public long size;
    }
}
