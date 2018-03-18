package com.paritytrading.parity.obm;

import java.util.Arrays;
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
import io.crossbar.autobahn.wamp.types.Subscription;

public class WampClient {

    public static final int CREATEORDER_ARGS_SIZE = 6;

    private static final Logger LOGGER = Logger.getLogger(WampClient.class.getName());
    private static final String PROC_CREATEORDER = "order.create";
    private static final String TOPIC_COUNTER = "order.oncounter";

    public CompletableFuture<ExitInfo> Start(String websocketURL, String realm) {
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
        CompletableFuture<Registration> regFuture = session.register(PROC_CREATEORDER, this::createOder);
        regFuture.thenAccept(reg -> LOGGER.info("Registered procedure: com.example.createOder"));

        CompletableFuture<Subscription> subFuture = session.subscribe(
                TOPIC_COUNTER, this::onCounter);
        subFuture.thenAccept(subscription ->
                LOGGER.info(String.format("Subscribed to topic: %s", subscription.topic)));

        final int[] x = {0};
        final int[] counter = {0};

        final PublishOptions publishOptions = new PublishOptions(true, false);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {

            // here we CALL every second
            CompletableFuture<CallResult> f = session.call(PROC_CREATEORDER, x[0], 3);
            f.whenComplete((callResult, throwable) -> {
                if (throwable == null) {
                    LOGGER.info(String.format("Got result: %s, ", callResult.results.get(0)));
                    x[0] += 1;
                } else {
                    LOGGER.info(String.format("ERROR - call failed: %s", throwable.getMessage()));
                }
            });

            CompletableFuture<Publication> p = session.publish(
                    TOPIC_COUNTER, publishOptions, counter[0], session.getID(), "Java");
            p.whenComplete((publication, throwable) -> {
                if (throwable == null) {
                    LOGGER.info("published to 'oncounter' with counter " + counter[0]);
                    counter[0] += 1;
                } else {
                    LOGGER.info(String.format("ERROR - pub failed: %s", throwable.getMessage()));
                }
            });

        }, 0, 2, TimeUnit.SECONDS);
    }

    private void onLeaveCallback(Session session, CloseDetails detail) {
        LOGGER.info(String.format("Left reason=%s, message=%s", detail.reason, detail.message));
    }

    private void onDisconnectCallback(Session session, boolean wasClean) {
        LOGGER.info(String.format("Session with ID=%s, disconnected.", session.getID()));
    }

    private List<Object> createOder(List<Integer> args, InvocationDetails details) {
        // should have 5 args
        if (args.size() != CREATEORDER_ARGS_SIZE) {
            return null;
        }
        // arg 0: order id | text  - The order identifier must be unique within the trading session.
        // arg 1: side | int
        // arg 2: symbol | text
        // arg 3: quantity | number
        // arg 4: price | number
        // arg 5: custome id | text
        int res = args.get(0) + args.get(1);
        return Arrays.asList(res, details.session.getID(), "Java");
    }

    private void onCounter(List<Object> args) {
        LOGGER.info(String.format("oncounter event, counter value=%s from component %s (%s)",
                args.get(0), args.get(1), args.get(2)));
    }
}
