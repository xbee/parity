package com.paritytrading.parity.obm.command;

import com.paritytrading.parity.obm.OrderManager;
import com.paritytrading.parity.obm.event.Error;
import com.paritytrading.parity.obm.event.Errors;

import java.util.List;
import java.util.Scanner;

class ErrorsCommand implements Command {

    @Override
    public void execute(OrderManager client, List<Object> arguments) throws CommandException {
//        if (arguments.hasNext())
//            throw new CommandException();

        client.printf("\n%s\n", Error.HEADER);
        for (Error error : Errors.collect(client.getEvents()))
            client.printf("%s\n", error.format());
        client.printf("\n");
    }

    @Override
    public String getName() {
        return "errors";
    }

    @Override
    public String getDescription() {
        return "Display occurred errors";
    }

    @Override
    public String getUsage() {
        return "errors";
    }

}
