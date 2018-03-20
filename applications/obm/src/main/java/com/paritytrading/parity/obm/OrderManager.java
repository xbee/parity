package com.paritytrading.parity.obm;

import static java.lang.Thread.sleep;
import static org.jvirtanen.util.Applications.*;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.nassau.soupbintcp.SoupBinTCP;
import com.paritytrading.parity.net.poe.POE;
import com.paritytrading.parity.obm.command.CommandException;
import com.paritytrading.parity.obm.command.EnterCommand;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.jvirtanen.config.Configs;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import ws.wamp.jawampa.*;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;
import ws.wamp.jawampa.transport.netty.SimpleWampWebsocketListener;

public class OrderManager implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(OrderManager.class.getName());
    public static final Locale LOCALE = Locale.US;
    private static final String RPC_ORDERS_CREATE = "tridex.dev.orders.create";
    private static final String TOPIC_COUNTER = "order.oncounter";
//    private static WampClient wampclt;
    private static WampClient wampclt;
    private static WampRouterBuilder wampRouterBuilder;
    private static WampRouter wampRouter;
    private static SimpleWampWebsocketListener wampRouterServer;
    private static OrderManager self;
    private static Subscription addProcSubscription;

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
//        this.wampClient = new WampClient();
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
        while (true) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

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

    private static void serverRouter(String uri, String realm) {
        wampRouterBuilder = new WampRouterBuilder();
        try {
            wampRouterBuilder.addRealm(realm);
            wampRouter = wampRouterBuilder.build();
        } catch (ApplicationError e1) {
            e1.printStackTrace();
            return;
        }

        try {
            URI suri = new URI(uri);

            try {
                wampRouterServer = new SimpleWampWebsocketListener(wampRouter, suri, null);
                wampRouterServer.start();
            } catch (ApplicationError applicationError) {
                applicationError.printStackTrace();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

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

            wampclt.statusChanged().subscribe(new Action1<WampClient.State>() {
                @Override
                public void call(WampClient.State t1) {
                    System.out.println("Session1 status changed to " + t1);

                    if (t1 instanceof WampClient.ConnectedState) {
                        // Register a procedure
                        addProcSubscription = regCreateOrder(self);
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable t) {
                    System.out.println("WSTicker connection ended with error " + t);
                }
            }, new Action0() {
                @Override
                public void call() {
                    System.out.println("WSTicker connection ended normally");
                }
            });

            wampclt.open();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
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

        String routerUrl = config.getString("orderentry-router.url");
        String routerRealm = config.getString("orderentry-router.realm");

        self = OrderManager.open(new InetSocketAddress(orderEntryAddress, orderEntryPort),
                orderEntryUsername, orderEntryPassword, instruments);
        serverRouter(routerUrl, routerRealm);
        // init WAMP Client: connect to WAMP router
        initWampClient(routerUrl, routerRealm);

    }

    private static Subscription regCreateOrder(OrderManager om) {
        return wampclt.registerProcedure(RPC_ORDERS_CREATE).subscribe(new Action1<Request>() {
            @Override
            public void call(Request request) {
                if (request.arguments() == null || request.arguments().size() != 5
                        || !request.arguments().get(1).canConvertToInt()
                        || !request.arguments().get(3).canConvertToLong()
                        || !request.arguments().get(4).canConvertToLong())
                {
                    try {
                        request.replyError(new ApplicationError(ApplicationError.INVALID_PARAMETER));
                    } catch (ApplicationError e) {
                        e.printStackTrace();
                    }
                }
                else {
                    String account = request.arguments().get(0).textValue();
                    int side = request.arguments().get(1).asInt();
                    boolean isbuy = (side == 0) ? false : true;
                    String symbol = request.arguments().get(2).asText();
                    long amount = request.arguments().get(3).asLong();
                    long price = request.arguments().get(4).asLong();

                    ArrayList<Object> args = new ArrayList<Object>();
                    args.add(0, request.arguments().get(0));
                    args.add(1, request.arguments().get(1));
                    args.add(2, request.arguments().get(2));
                    args.add(3, request.arguments().get(3));
                    args.add(4, request.arguments().get(4));

//                    Command cmd = Commands.find("buy");
                    EnterCommand cmd = new EnterCommand(isbuy ? POE.BUY : POE.SELL);
                    try {
                        cmd.execute(self, args);
                    } catch (CommandException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // create a entercommand and exec it
//                    request.reply(a + b);
                }
            }
        });
    }
}
