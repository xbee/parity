package com.paritytrading.parity.obm;

import static org.jvirtanen.util.Applications.*;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.nassau.soupbintcp.SoupBinTCP;
import com.paritytrading.parity.obm.command.Command;
import com.paritytrading.parity.obm.command.CommandException;
import com.paritytrading.parity.obm.command.Commands;
import com.paritytrading.parity.obm.event.Events;
import com.paritytrading.parity.util.Instruments;
import com.paritytrading.parity.util.OrderIDGenerator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import org.jvirtanen.config.Configs;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import ws.wamp.jawampa.ApplicationError;
import ws.wamp.jawampa.Request;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;

public class TerminalClient implements Closeable {

    public static final Locale LOCALE = Locale.US;
    private static WampClient wampclt;
    private static Subscription addProcSubscription;

    public static final long NANOS_PER_MILLI = 1_000_000;

    private Events events;

    private OrderEntry orderEntry;

    private Instruments instruments;

    private OrderIDGenerator orderIdGenerator;

    private boolean closed;

    private static void initWampClient(String url, String realm) {
        try {
            IWampConnectorProvider connectorProvider = new NettyWampClientConnectorProvider();
            WampClientBuilder builder = new WampClientBuilder();

            builder.withConnectorProvider(connectorProvider)
                    .withUri(url)
                    .withRealm(realm)
                    .withInfiniteReconnects()
                    .withReconnectInterval(3, TimeUnit.SECONDS);

            // Create a client through the builder. This will not immediatly start
            // a connection attempt
            wampclt = builder.build();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        wampclt.statusChanged().subscribe(new Action1<WampClient.State>() {
            @Override
            public void call(WampClient.State t1) {
                System.out.println("Session1 status changed to " + t1);

                if (t1 instanceof WampClient.ConnectedState) {
                    // Register a procedure
                    addProcSubscription = wampclt.registerProcedure("order.place").subscribe(new Action1<Request>() {
                        @Override
                        public void call(Request request) {
                            if (request.arguments() == null || request.arguments().size() != 2
                                    || !request.arguments().get(0).canConvertToLong()
                                    || !request.arguments().get(1).canConvertToLong())
                            {
                                try {
                                    request.replyError(new ApplicationError(ApplicationError.INVALID_PARAMETER));
                                } catch (ApplicationError e) {
                                    e.printStackTrace();
                                }
                            }
                            else {
                                long a = request.arguments().get(0).asLong();
                                long b = request.arguments().get(1).asLong();
                                request.reply(a + b);
                            }
                        }
                    });
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                System.out.println("Session1 ended with error " + t);
            }
        }, new Action0() {
            @Override
            public void call() {
                System.out.println("Session1 ended normally");
            }
        });

        wampclt.open();
    }

    private TerminalClient(Events events, OrderEntry orderEntry, Instruments instruments) {
        this.events      = events;
        this.orderEntry  = orderEntry;
        this.instruments = instruments;

        this.orderIdGenerator = new OrderIDGenerator();
    }

    public static TerminalClient open(InetSocketAddress address, String username,
            String password, Instruments instruments) throws IOException {
        Events events = new Events();

        OrderEntry orderEntry = OrderEntry.open(address, events);

        SoupBinTCP.LoginRequest loginRequest = new SoupBinTCP.LoginRequest();

        ASCII.putLeft(loginRequest.username, username);
        ASCII.putLeft(loginRequest.password, password);
        ASCII.putRight(loginRequest.requestedSession, "");
        ASCII.putLongRight(loginRequest.requestedSequenceNumber, 0);

        orderEntry.getTransport().login(loginRequest);

        return new TerminalClient(events, orderEntry, instruments);
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

    public Events getEvents() {
        return events;
    }

    public void run() throws IOException {
        ConsoleReader reader = new ConsoleReader();

        reader.addCompleter(new StringsCompleter(Commands.names().castToList()));

        printf("Type 'help' for help.\n");

        while (!closed) {
            String line = reader.readLine("> ");
            if (line == null)
                break;

            Scanner scanner = scan(line);

            if (!scanner.hasNext())
                continue;

            Command command = Commands.find(scanner.next());
            if (command == null) {
                printf("error: Unknown command\n");
                continue;
            }

            try {
                command.execute(this, scanner);
            } catch (CommandException e) {
                printf("Usage: %s\n", command.getUsage());
            } catch (ClosedChannelException e) {
                printf("error: Connection closed\n");
            }
        }

        close();
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
        // init WAMP Client: connect to WAMP router
        initWampClient(routerUrl, routerRealm);

        TerminalClient.open(new InetSocketAddress(orderEntryAddress, orderEntryPort),
                orderEntryUsername, orderEntryPassword, instruments).run();
    }

}
