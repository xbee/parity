try {
   // for Node.js
   var autobahn = require('autobahn');
} catch (e) {
   // for browsers (where AutobahnJS is available globally)
}

var wsuri = "wss://api.poloniex.com";
var connection = new autobahn.Connection({
  url: wsuri,
  realm: "realm1"
});
 
connection.onopen = function (session) {
        console.log("Websocket connection opened !!");

        function marketEvent (args,kwargs) {
                console.log(args);
        }
        function tickerEvent (args,kwargs) {
                console.log(args);
        }
        function trollboxEvent (args,kwargs) {
                console.log(args);
        }
        session.subscribe('BTC_ETH', marketEvent);
        session.subscribe('ticker', tickerEvent);
        session.subscribe('trollbox', trollboxEvent);
}
 
connection.onclose = function () {
  console.log("Websocket connection closed !");
}
                       
connection.open();

