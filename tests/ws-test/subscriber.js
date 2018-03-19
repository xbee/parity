try {
   // for Node.js
   var autobahn = require('autobahn');
} catch (e) {
   // for browsers (where AutobahnJS is available globally)
}

var Guid = require("guid");
var connection = new autobahn.Connection({url: 'ws://localhost:8020/ws/', realm: 'realm1'});

var FUNC_CREATEORDER = "tridex.dev.orders.create";

connection.onopen = function (session) {

   // 1) subscribe to a topic
   function onevent(args) {
      console.log("Event:", args[0]);
   }

   function ontick(args) {
      console.log("Tick:", args[0]);
   }

   session.subscribe('data', onevent, { match: "prefix" });
   session.subscribe('ticker.BTC-USD', ontick, { match: "prefix" });

   // 2) publish an event
   session.publish('com.myapp.hello', ['Hello, world!']);

   // 3) register a procedure for remoting
   function add2(args) {
      return args[0] + args[1];
   }
   session.register('com.myapp.add2', add2);

   function createNewOrder(args) {
        console.log("create new order: ", args);
        // create a order number(guid)
        var guid = Guid.create();
        orderNumber = guid.value;
        return {"status": "ok", "data": {"on": orderNumber, "ts": Date.now()}}
   }
   session.register(FUNC_CREATEORDER, createNewOrder);

   // 4) call a remote procedure
   session.call('com.myapp.add2', [2, 3]).then(
      function (res) {
         console.log("Result:", res);
      }
   );
};

connection.open();

