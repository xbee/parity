package com.paritytrading.parity.wsfix;

import static org.jvirtanen.util.Applications.*;

import com.paritytrading.parity.util.Instruments;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.jvirtanen.config.Configs;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import ws.wamp.jawampa.ApplicationError;
import ws.wamp.jawampa.Request;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;

class OrderEntryGateway {

    private static WampClient wampclt;

    public static void main(String[] args) {
        if (args.length != 1)
            usage("parity-fix <configuration-file>");

        try {
            main(config(args[0]));
        } catch (ConfigException | FileNotFoundException e) {
            error(e);
        } catch (IOException e) {
            fatal(e);
        }
    }

    private static void main(Config config) throws IOException {

        OrderEntryFactory orderEntry = orderEntry(config);
        FIXAcceptor       fix        = fix(orderEntry, config);

        Events.process(fix);
    }

    private static OrderEntryFactory orderEntry(Config config) {
        InetAddress address = Configs.getInetAddress(config, "order-entry.address");
        int         port    = Configs.getPort(config, "order-entry.port");

        return new OrderEntryFactory(new InetSocketAddress(address, port));
    }

    private static FIXAcceptor fix(OrderEntryFactory orderEntry, Config config) throws IOException {
        InetAddress address      = Configs.getInetAddress(config, "fix.address");
        int         port         = Configs.getPort(config, "fix.port");
        String      senderCompId = config.getString("fix.sender-comp-id");

        Instruments instruments = Instruments.fromConfig(config, "instruments");

        return FIXAcceptor.open(orderEntry, new InetSocketAddress(address, port),
                senderCompId, instruments);
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
            wampclt.open();

            // Provide a procedure
            Subscription proc = wampclt.registerProcedure("order.new").subscribe(new Action1<Request>() {
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

            // Unregister the procedure
//            proc.unsubscribe();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

}
