package com.paritytrading.parity.obm.command;

import com.paritytrading.parity.obm.OrderManager;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public interface Command {

    void execute(OrderManager client, List<Object> args) throws CommandException, IOException;

    String getName();

    String getDescription();

    String getUsage();

}
