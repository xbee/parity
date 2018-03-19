package com.paritytrading.parity.obm.command;

import com.paritytrading.parity.obm.OrderManager;

import java.util.List;
import java.util.Scanner;

class ExitCommand implements Command {

    @Override
    public void execute(OrderManager client, List<Object> arguments) throws CommandException {
//        if (arguments.hasNext())
//            throw new CommandException();

        client.close();
    }

    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public String getDescription() {
        return "Exit the client";
    }

    @Override
    public String getUsage() {
        return "exit";
    }

}
