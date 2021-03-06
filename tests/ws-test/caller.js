
try {
   // for Node.js
   var autobahn = require('autobahn');
} catch (e) {
   // for browsers (where AutobahnJS is available globally)
}

var Guid = require("guid");
var connection = new autobahn.Connection({url: 'ws://localhost:9080/', realm: 'nexus.realm1'});

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
        account = args[0];
        clordid = args[1];
        isbuy = args[2];
        amount = args[3];
        symbol = args[4];
        price = args[5];

        var guid = Guid.create();
        orderNumber = guid.value;
        return {"state": "order_received", "data": {"oid": orderNumber, "ts": Date.now()}}
   }
//   session.register(RPC_CREATEORDER, createNewOrder);

   // 4) call a remote procedure
   session.call(RPC_CREATEORDER, ['a1001', 'cs34', 1, 946781, 'BTC-USD', 895046]).then(
      function (res) {
         console.log("Result:", res);
      }
   );

   session.call(RPC_CREATEORDER_TEST, ['a1005', 'c23', 1, 739641, 'BTC-USD', 895027]).then(
         function (res) {
            console.log("Result(Test):", res);
         }
      );
   session.call('com.myapp.test', [4, 6]).then(
            function (res) {
               console.log("Result(add):", res);
            }
         );
};

connection.open();

