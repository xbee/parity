package com.paritytrading.parity.obm.command;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.parity.obm.OrderManager;
import com.paritytrading.parity.net.poe.POE;
import com.paritytrading.parity.util.Instrument;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

class EnterCommand implements Command {

    private POE.EnterOrder message;

    public EnterCommand(byte side) {
        this.message = new POE.EnterOrder();

        this.message.side = side;
    }

    public EnterCommand() {
        this.message = new POE.EnterOrder();
    }

    // arg 0: account | text
    // arg 1: side | int
    // arg 2: symbol | text
    // arg 3: amount | number
    // arg 4: price | number
    @Override
    public void execute(OrderManager client, List<Object> arguments) throws CommandException, IOException {
        try {
            if (arguments.size() != 5)
                throw new CommandException("Wrong size of args!");

//            String account = (String) arguments.get(0);
            double quantity   = (double) arguments.get(3);
            long   instrument = (long)arguments.get(2);
            double price      = (double) arguments.get(4);
            boolean isbuy  = (boolean) arguments.get(1);

            Instrument config = client.getInstruments().get(instrument);
            if (config == null)
                throw new CommandException("Wrong instrument id!");

            execute(client, isbuy, Math.round(quantity * config.getSizeFactor()), instrument, Math.round(price * config.getPriceFactor()));
        } catch (NoSuchElementException e) {
            throw new CommandException();
        }
    }

    private void execute(OrderManager client, boolean isbuy, long quantity, long instrument, long price) throws IOException {
        // generate order id
        String orderId = client.getOrderIdGenerator().next();

        ASCII.putLeft(message.orderId, orderId);
        message.quantity   = quantity;
        message.instrument = instrument;
        message.price      = price;
        message.side       = isbuy ? POE.BUY : POE.SELL;

        client.getOrderEntry().send(message);

        client.printf("\nOrder ID\n----------------\n%s\n\n", orderId);
    }

    @Override
    public String getName() {
        return message.side == POE.BUY ? "buy" : "sell";
    }

    @Override
    public String getDescription() {
        return "Enter a " + getName() + " order";
    }

    @Override
    public String getUsage() {
        return getName() + " <quantity> <instrument> <price>";
    }

}
