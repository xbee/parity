package com.paritytrading.parity.obm;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SessionDetails;

public class WampClient {

    public static final int CREATEORDER_ARGS_SIZE = 5;

    private static final Logger LOGGER = Logger.getLogger("WampClient");
    private static final String RPC_ORDERS_CREATE = "tridex.dev.orders.create";
    private static final String TOPIC_COUNTER = "order.oncounter";

    private OrderEntry orderEntry;

    public WampClient(OrderEntry orderentry) {
        this.orderEntry = orderentry;
    }

    public CompletableFuture<ExitInfo> open(String websocketURL, String realm) {
        Session session = new Session();
        session.addOnConnectListener(this::onConnectCallback);
        session.addOnJoinListener(this::onJoinCallback);
        session.addOnLeaveListener(this::onLeaveCallback);
        session.addOnDisconnectListener(this::onDisconnectCallback);

        // finally, provide everything to a Client instance and connect
        Client client = new Client(session, websocketURL, realm);
        return client.connect();
    }

    private void onConnectCallback(Session session) {
        LOGGER.info("Session connected, ID=" + session.getID());
    }

    private void onJoinCallback(Session session, SessionDetails details) {
        // Register all of the rpc functions
        CompletableFuture<Registration> regFuture = session.register(RPC_ORDERS_CREATE, this::createOder);
        regFuture.thenAccept(reg ->
                LOGGER.info(String.format("Registered procedure: %s", RPC_ORDERS_CREATE)));

    }

    private void onLeaveCallback(Session session, CloseDetails detail) {
        LOGGER.info(String.format("Left reason=%s, message=%s", detail.reason, detail.message));
    }

    private void onDisconnectCallback(Session session, boolean wasClean) {
        LOGGER.info(String.format("Session with ID=%s, disconnected.", session.getID()));
    }

    private List<Object> createOder(List<Object> args, InvocationDetails details) {
        // should have 5 args
        if (args.size() != CREATEORDER_ARGS_SIZE) {
            return null;
        }
        // arg 0: account | text
        // arg 1: order id | text  - The order identifier must be unique within the trading session.
        // arg 1: side | int
        // arg 2: symbol | text
        // arg 3: amount | number
        // arg 4: price | number
        String account = (String) args.get(0);
//        String oid = (String) args.get(1);
        boolean isBuy = (boolean) args.get(1);
        String symbol = (String) args.get(2);
        double amount = (double) args.get(3);
        double price = (double) args.get(4);

        // send order to POE


//        int res = args.get(0) + args.get(1);
//        return Arrays.asList(res, details.session.getID(), "Java");
        return null;
    }

    private void onCounter(List<Object> args) {
        LOGGER.info(String.format("oncounter event, counter value=%s from component %s (%s)",
                args.get(0), args.get(1), args.get(2)));
    }
}
