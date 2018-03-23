package com.paritytrading.parity.obm.command;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.parity.obm.OrderManager;
import com.paritytrading.parity.net.poe.POE;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class CancelCommand implements Command {

    private POE.CancelOrder message;

    public CancelCommand() {
        message = new POE.CancelOrder();
    }

    @Override
    public void execute(OrderManager client, List<Object> arguments) throws CommandException, IOException {
        try {
            if (arguments.size() != 2)
                throw new CommandException("Invalid number of arguments!");

            String orderId = (String) arguments.get(0);

            execute(client, orderId);
        } catch (NoSuchElementException e) {
            throw new CommandException();
        }
    }

    private void execute(OrderManager client, String orderId) throws IOException {
        ASCII.putLeft(message.orderId, orderId);
        message.quantity = 0;

        client.getOrderEntry().send(message);
    }

    private void execute(OrderManager client, String orderId, long qty) throws IOException {
        ASCII.putLeft(message.orderId, orderId);
        message.quantity = qty;

        client.getOrderEntry().send(message);
    }

    @Override
    public String getName() {
        return "cancel";
    }

    @Override
    public String getDescription() {
        return "Cancel an order";
    }

    @Override
    public String getUsage() {
        return "cancel <order-id>";
    }

}
