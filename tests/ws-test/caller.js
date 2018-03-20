
try {
   // for Node.js
   var autobahn = require('autobahn');
} catch (e) {
   // for browsers (where AutobahnJS is available globally)
}

var Guid = require("guid");
var connection = new autobahn.Connection({url: 'ws://localhost:8010/poe/', realm: 'realm1'});

var RPC_CREATEORDER = "tridex.dev.orders.create";
var RPC_CREATEORDER_TEST = "tridex.test.orders.create";

connection.onopen = function (session) {

   // 1) subscribe to a topic
   function onevent(args) {
      console.log("Event:", args[0]);
   }

   function ontick(args) {
      console.log("Tick:", args[0]);
   }

//   session.subscribe('data', onevent, { match: "prefix" });
//   session.subscribe('ticker.BTC-USD', ontick, { match: "prefix" });

   // 2) publish an event
//   session.publish('com.myapp.hello', ['Hello, world!']);

   // 3) register a procedure for remoting
   function add2(args) {
      return args[0] + args[1];
   }
   session.register('com.myapp.test', add2);

   function createNewOrder(args) {
        console.log("create new order: ", args);
        // create a order number(guid)
        account = args[0]
        isbuy = args[1]
        symbol = args[2]
        amount = args[3]
        price = args[4]

        var guid = Guid.create();
        orderNumber = guid.value;
        return {"status": "ok", "data": {"oid": orderNumber, "ts": Date.now()}}
   }
   session.register(RPC_CREATEORDER_TEST, createNewOrder);

   // 4) call a remote procedure
   session.call(RPC_CREATEORDER, [1001, 1, 'BTC-USD', 1, 8950]).then(
      function (res) {
         console.log("Result:", res);
      }
   );

   session.call(RPC_CREATEORDER_TEST, [1005, 3, 'BTC-USD', 1, 8950]).then(
         function (res) {
            console.log("Result:", res);
         }
      );
};

connection.open();

