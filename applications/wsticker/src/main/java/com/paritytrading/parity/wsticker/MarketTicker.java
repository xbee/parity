package com.paritytrading.parity.wsticker;

import static java.util.Arrays.*;
import static org.jvirtanen.util.Applications.*;

import com.lmax.disruptor.dsl.Disruptor;
import com.paritytrading.nassau.MessageListener;
import com.paritytrading.nassau.util.BinaryFILE;
import com.paritytrading.nassau.util.MoldUDP64;
import com.paritytrading.nassau.util.SoupBinTCP;
import com.paritytrading.parity.book.Market;
import com.paritytrading.parity.net.pmd.PMDParser;
import com.paritytrading.parity.util.Instrument;
import com.paritytrading.parity.util.Instruments;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.jvirtanen.config.Configs;
import rx.functions.Action0;
import rx.functions.Action1;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;

class MarketTicker {

    private static WampClient wampclt;

    public static void main(String[] args) {
        if (args.length < 1)
            usage();

        boolean taq = args[0].equals("-t");

        try {
            main(taq, taq ? copyOfRange(args, 1, args.length) : args);
        } catch (ConfigException | FileNotFoundException e) {
            error(e);
        } catch (IOException e) {
            fatal(e);
        }
    }

    private static void main(boolean taq, String[] args) throws IOException {
        switch (args.length) {
        case 1:
            listen(taq, config(args[0]));
            return;
        case 2:
            read(taq, config(args[0]), new File(args[1]));
            return;
        default:
            usage();
            return;
        }
    }

    private static void listen(boolean taq, Config config) throws IOException {
        Instruments instruments = Instruments.fromConfig(config, "instruments");

        // Executor that will be used to construct new threads for consumers
        Executor executor = Executors.newCachedThreadPool();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;

        // Construct the Disruptor
        Disruptor<TickEvent> disruptor = new Disruptor<>(TickEvent::new, bufferSize, executor);

        // Connect the handler
        disruptor.handleEventsWith(MarketTicker::handleEvent);

        // Start the Disruptor, starts all threads running
        disruptor.start();

        String routerUrl = config.getString("wamp-router.url");
        String routerRealm = config.getString("wamp-router.realm");
        // init WAMP Client: connect to WAMP router
        initWampClient(routerUrl, routerRealm);

        MarketDataListener listener = taq ? new TAQFormat(instruments) : new TickEventProducer(instruments, disruptor);

        Market market = new Market(listener);

        for (Instrument instrument : instruments)
            market.open(instrument.asLong());

        MarketDataProcessor processor = new MarketDataProcessor(market, listener);

        listen(config, new PMDParser(processor));
    }

    private static void listen(Config config, MessageListener listener) throws IOException {
        if (config.hasPath("market-data.multicast-interface")) {
            NetworkInterface multicastInterface = Configs.getNetworkInterface(config, "market-data.multicast-interface");
            InetAddress      multicastGroup     = Configs.getInetAddress(config, "market-data.multicast-group");
            int              multicastPort      = Configs.getPort(config, "market-data.multicast-port");
            InetAddress      requestAddress     = Configs.getInetAddress(config, "market-data.request-address");
            int              requestPort        = Configs.getPort(config, "market-data.request-port");

            MoldUDP64.receive(multicastInterface, new InetSocketAddress(multicastGroup, multicastPort),
                    new InetSocketAddress(requestAddress, requestPort), listener);
        } else {
            InetAddress address  = Configs.getInetAddress(config, "market-data.address");
            int         port     = Configs.getPort(config, "market-data.port");
            String      username = config.getString("market-data.username");
            String      password = config.getString("market-data.password");

            SoupBinTCP.receive(new InetSocketAddress(address, port), username, password, listener);
        }
    }

    private static void read(boolean taq, Config config, File file) throws IOException {
        Instruments instruments = Instruments.fromConfig(config, "instruments");

        MarketDataListener listener = taq ? new TAQFormat(instruments) : new DisplayFormat(instruments);

        Market market = new Market(listener);

        for (Instrument instrument : instruments)
            market.open(instrument.asLong());

        MarketDataProcessor processor = new MarketDataProcessor(market, listener);

        BinaryFILE.read(file, new PMDParser(processor));
    }

    private static void usage() {
        System.err.println("Usage: parity-ticker [-t] <configuration-file> [<input-file>]");
        System.exit(2);
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
                    System.out.println("WSTicker connection status changed to " + t1);
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

    public static void handleEvent(TickEvent event, long sequence, boolean endOfBatch)
    {
        JSONObject obj = event.get().toJSON();
        obj.put("seq", sequence);
        String topic = String.format("ticker.%s", event.get().instrument);
        wampclt.publish(topic, obj);
    }

}
