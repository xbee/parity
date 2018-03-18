package main

import (
	"log"
	"os"
	"os/signal"

	"github.com/gammazero/nexus/client"
	"github.com/gammazero/nexus/wamp"
)

const tickTopic = "ticker.BTC-USD"
const dataTopic = "data.BTC-USD"

func main() {
	logger := log.New(os.Stdout, "", 0)
	cfg := client.ClientConfig{
		Realm:  "realm1",
		Logger: logger,
	}

	// Connect subscriber session.
	subscriber, err := client.ConnectNet("ws://localhost:8020/ws/", cfg)
	if err != nil {
		logger.Fatal(err)
	}
	defer subscriber.Close()

	// Define function to handle events received.
	tickHandler := func(args wamp.List, kwargs wamp.Dict, details wamp.Dict) {
		logger.Println("Received ", tickTopic)
		if len(args) != 0 {
			logger.Println("  ", args[0])
		}
	}

	dataHandler := func(args wamp.List, kwargs wamp.Dict, details wamp.Dict) {
		logger.Println("Received ", dataTopic)
		if len(args) != 0 {
			logger.Println(" Message:", args[0])
		}
	}

	// Subscribe to topic.
	err = subscriber.Subscribe(tickTopic, tickHandler, nil)
	if err != nil {
		logger.Fatal("subscribe error:", err)
	}
	logger.Println("Subscribed to", tickTopic)

	err = subscriber.Subscribe(dataTopic, dataHandler, nil)
	if err != nil {
		logger.Fatal("subscribe error:", err)
	}
	logger.Println("Subscribed to", dataTopic)

	// Wait for CTRL-c or client close while handling events.
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt)
	select {
	case <-sigChan:
	case <-subscriber.Done():
		logger.Print("Router gone, exiting")
		return // router gone, just exit
	}

	// Unsubscribe from topic.
	if err = subscriber.Unsubscribe(tickTopic); err != nil {
		logger.Println("Failed to unsubscribe:", err)
	}

	if err = subscriber.Unsubscribe(dataTopic); err != nil {
		logger.Println("Failed to unsubscribe:", err)
	}
}
