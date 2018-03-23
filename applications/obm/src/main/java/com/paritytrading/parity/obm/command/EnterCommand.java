package com.paritytrading.parity.obm.command;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.parity.obm.OrderManager;
import com.paritytrading.parity.net.poe.POE;
import com.paritytrading.parity.util.Instrument;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Logger;

public class EnterCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(OrderManager.class.getName());
    private POE.EnterOrder message;

    public EnterCommand(byte side) {
        this.message = new POE.EnterOrder();

        this.message.side = side;
    }

    public EnterCommand() {
        this.message = new POE.EnterOrder();
    }

    // arg 0: account | text
    // arg 1: clordid | text
    // arg 2: side | int
    // arg 3: amount | int
    // arg 4: symbol | text
    // arg 5: price | int
    @Override
    public void execute(OrderManager client, List<Object> arguments) throws CommandException, IOException {
        try {
            if (arguments.size() != 6) {
                LOGGER.warning("Wrong size of args!");
                throw new CommandException("Wrong size of args!");
            }

            String orderid = (String) arguments.get(1);
            long quantity   = (long) arguments.get(3);
            long instrument = (long)arguments.get(4);
            long price      = (long) arguments.get(5);
            int s   = (int) arguments.get(2);
            boolean isbuy = (s == 1) ? true : false;

            Instrument config = client.getInstruments().get(instrument);
            if (config == null) {
                LOGGER.warning("Wrong instrument id!");
                throw new CommandException("Wrong instrument id!");
            }

            execute(client, orderid, isbuy, Math.round(quantity * config.getSizeFactor()), instrument, Math.round(price * config.getPriceFactor()));
        } catch (NoSuchElementException e) {
            LOGGER.warning("No such element!");
            throw new CommandException();
        }
    }

    private void execute(OrderManager client, String ordid, boolean isbuy, long quantity, long instrument, long price) throws IOException {
        // generate order id
//        String orderId = client.getOrderIdGenerator().next();

        ASCII.putLeft(message.orderId, ordid);
        message.quantity   = quantity;
        message.instrument = instrument;
        message.price      = price;
        message.side       = isbuy ? POE.BUY : POE.SELL;

        client.getOrderEntry().send(message);

        client.printf("\nOrder ID\n----------------\n%s\n\n", ordid);
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
