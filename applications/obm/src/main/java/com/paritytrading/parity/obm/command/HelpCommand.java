package com.paritytrading.parity.obm.command;

import com.paritytrading.parity.obm.OrderManager;

import java.util.List;
import java.util.Scanner;

class HelpCommand implements Command {

    @Override
    public void execute(OrderManager client, List<Object> arguments) throws CommandException {
//        if (arguments.hasNext()) {
//            Command command = Commands.find(arguments.next());
//
////            if (arguments.hasNext())
////                throw new CommandException();
//
//            if (command != null)
//                displayCommandHelp(client, command);
//            else
//                displayGeneralHelp(client);
//        } else {
//            displayGeneralHelp(client);
//        }

        displayGeneralHelp(client);
    }

    private void displayGeneralHelp(OrderManager client) {
        client.printf("Commands:\n");

        int maxCommandNameLength = calculateMaxCommandNameLength();

        for (Command command : Commands.all())
            client.printf("  %-" + maxCommandNameLength + "s  %s\n", command.getName(), command.getDescription());

        client.printf("\nType 'help <command>' for command specific help.\n");
    }

    private void displayCommandHelp(OrderManager client, Command command) {
        client.printf("Usage: %s\n\n  %s\n\n", command.getUsage(), command.getDescription());
    }

    private int calculateMaxCommandNameLength() {
        return Commands.all().collectInt(c -> c.getName().length()).max();
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Display the help";
    }

    @Override
    public String getUsage() {
        return "help [command]";
    }

}