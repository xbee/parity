package com.paritytrading.parity.obm;

import static org.jvirtanen.util.Applications.*;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.nassau.soupbintcp.SoupBinTCP;
import com.paritytrading.parity.obm.event.POEListener;
import com.paritytrading.parity.util.Instruments;
import com.paritytrading.parity.util.OrderIDGenerator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Logger;



import jline.console.ConsoleReader;
import org.jvirtanen.config.Configs;

public class OrderManager implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(OrderManager.class.getName());
    public static final Locale LOCALE = Locale.US;
    private static WampClient wampclt;
//    private static Subscription addProcSubscription;

    public static final long NANOS_PER_MILLI = 1_000_000;

    private POEListener evtListener;

    private OrderEntry orderEntry;
    private WampClient wampClient;

    private Instruments instruments;

    private OrderIDGenerator orderIdGenerator;

    private boolean closed;

    private OrderManager(POEListener listener, OrderEntry orderEntry, Instruments instruments) {
        this.evtListener = listener;
        this.orderEntry  = orderEntry;
        this.instruments = instruments;

        this.orderIdGenerator = new OrderIDGenerator();
        this.wampClient = new WampClient(orderEntry);
    }

    public static OrderManager open(InetSocketAddress address, String username,
                                    String password, Instruments instruments) throws IOException {
        POEListener listener = new POEListener();

        OrderEntry orderEntry = OrderEntry.open(address, listener);

        SoupBinTCP.LoginRequest loginRequest = new SoupBinTCP.LoginRequest();

        ASCII.putLeft(loginRequest.username, username);
        ASCII.putLeft(loginRequest.password, password);
        ASCII.putRight(loginRequest.requestedSession, "");
        ASCII.putLongRight(loginRequest.requestedSequenceNumber, 0);

        orderEntry.getTransport().login(loginRequest);

        return new OrderManager(listener, orderEntry, instruments);
    }

    public OrderEntry getOrderEntry() {
        return orderEntry;
    }

    public Instruments getInstruments() {
        return instruments;
    }

    public OrderIDGenerator getOrderIdGenerator() {
        return orderIdGenerator;
    }

    public POEListener getEvents() {
        return evtListener;
    }

    public void run() throws IOException {
//        this.wampClient.open()
//        ConsoleReader reader = new ConsoleReader();

//        reader.addCompleter(new StringsCompleter(Commands.names().castToList()));

//        printf("Type 'help' for help.\n");

//        while (!closed) {
//            String line = reader.readLine("> ");
//            if (line == null)
//                break;
//
//            Scanner scanner = scan(line);
//
//            if (!scanner.hasNext())
//                continue;
//
//            Command command = Commands.find(scanner.next());
//            if (command == null) {
//                printf("error: Unknown command\n");
//                continue;
//            }
//
//            try {
//                command.execute(this, scanner);
//            } catch (CommandException e) {
//                printf("Usage: %s\n", command.getUsage());
//            } catch (ClosedChannelException e) {
//                printf("error: Connection closed\n");
//            }
//        }

//        close();
    }

    @Override
    public void close() {
        orderEntry.close();

        closed = true;
    }

    public void printf(String format, Object... args) {
        System.out.printf(LOCALE, format, args);
    }

    private Scanner scan(String text) {
        Scanner scanner = new Scanner(text);
        scanner.useLocale(LOCALE);

        return scanner;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1)
            usage("parity-client <configuration-file>");

        try {
            main(config(args[0]));
        } catch (ConfigException | FileNotFoundException e) {
            error(e);
        }
    }

    private static void main(Config config) throws IOException {
        InetAddress orderEntryAddress  = Configs.getInetAddress(config, "order-entry.address");
        int         orderEntryPort     = Configs.getPort(config, "order-entry.port");
        String      orderEntryUsername = config.getString("order-entry.username");
        String      orderEntryPassword = config.getString("order-entry.password");

        Instruments instruments = Instruments.fromConfig(config, "instruments");

        String routerUrl = config.getString("wamp-router.url");
        String routerRealm = config.getString("wamp-router.realm");

        OrderManager om = OrderManager.open(new InetSocketAddress(orderEntryAddress, orderEntryPort),
                orderEntryUsername, orderEntryPassword, instruments);
        // init WAMP Client: connect to WAMP router
        om.wampClient.open(routerUrl, routerRealm);

    }

}
