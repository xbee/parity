package com.paritytrading.parity.obm.command;

import com.paritytrading.parity.obm.TerminalClient;
import java.io.IOException;
import java.util.Scanner;

public interface Command {

    void execute(TerminalClient client, Scanner arguments) throws CommandException, IOException;

    String getName();

    String getDescription();

    String getUsage();

}
