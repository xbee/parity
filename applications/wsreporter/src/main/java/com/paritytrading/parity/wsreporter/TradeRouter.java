package com.paritytrading.parity.wsreporter;

import static org.jvirtanen.util.Applications.*;

import com.paritytrading.nassau.MessageListener;
import com.paritytrading.nassau.util.MoldUDP64;
import com.paritytrading.nassau.util.SoupBinTCP;
import com.paritytrading.parity.net.pmr.PMRParser;
import com.paritytrading.parity.util.Instruments;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.concurrent.TimeUnit;

//import org.fluentd.logger.FluentLogger;
import org.json.simple.JSONObject;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.RingBuffer;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jvirtanen.config.Configs;

public class TradeRouter {

//    private static FluentLogger LOG = FluentLogger.getLogger("app");
    private static final String USAGE = "parity-router [-t] <configuration-file>";
    private static WampClient client1;
    private static Disruptor<PMREvent> disruptor;
    private static PMREventProducer eventProducer;

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
            client1 = builder.build();
            client1.open();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public static void handleEvent(PMREvent event, long sequence, boolean endOfBatch)
    {
        Trade t = event.get();
        JSONObject obj = new JSONObject();
        obj.put("timestamp", t.timestamp);
        obj.put("instrument", t.instrument);
        obj.put("quantity", t.quantity);
        obj.put("price", t.price);

        // create json object and send it
        client1.publish("data", obj);
//        LOG.log("data", obj);

    }

    public static void main(String[] args) {
        if (args.length != 1 && args.length != 2)
            usage(USAGE);

        boolean tsv = false;

        if (args.length == 2) {
            if (!args[0].equals("-t"))
                usage(USAGE);

            tsv = true;
        }

        try {
            main(config(args[tsv ? 1 : 0]), tsv);
        } catch (ConfigException | FileNotFoundException e) {
            error(e);
        } catch (IOException e) {
            fatal(e);
        }
    }


    private static void main(Config config, boolean tsv) throws IOException {

        Instruments instruments = Instruments.fromConfig(config, "instruments");

        // Executor that will be used to construct new threads for consumers
        Executor executor = Executors.newCachedThreadPool();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;

        // Construct the Disruptor
        Disruptor<PMREvent> disruptor = new Disruptor<>(PMREvent::new, bufferSize, executor);

        // Connect the handler
        disruptor.handleEventsWith(TradeRouter::handleEvent);

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // initialize the event producer to submit messages
        eventProducer  = new PMREventProducer(disruptor);

        initWampClient("ws://localhost:8080/ws1", "realm1");

        MessageListener listener = new PMRParser(new TradeProcessor(tsv ?
                new JsonFormat(instruments, eventProducer) : new DisplayFormat(instruments)));

        if (config.hasPath("trade-report.multicast-interface")) {
            NetworkInterface multicastInterface = Configs.getNetworkInterface(config, "trade-report.multicast-interface");
            InetAddress      multicastGroup     = Configs.getInetAddress(config, "trade-report.multicast-group");
            int              multicastPort      = Configs.getPort(config, "trade-report.multicast-port");
            InetAddress      requestAddress     = Configs.getInetAddress(config, "trade-report.request-address");
            int              requestPort        = Configs.getPort(config, "trade-report.request-port");

            MoldUDP64.receive(multicastInterface, new InetSocketAddress(multicastGroup, multicastPort),
                    new InetSocketAddress(requestAddress, requestPort), listener);
        } else {
            InetAddress address  = Configs.getInetAddress(config, "trade-report.address");
            int         port     = Configs.getPort(config, "trade-report.port");
            String      username = config.getString("trade-report.username");
            String      password = config.getString("trade-report.password");

            SoupBinTCP.receive(new InetSocketAddress(address, port), username, password, listener);
        }

        client1.close().toBlocking().last();

    }

    private void waitUntilKeypressed() {
        try {
            System.in.read();
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
