
// initialize the event bus when we initialize the server
var EventBus = require('vertx3-eventbus-client');

// this will typically come from a config, but this will suffice for now
var eventBus = new EventBus("http://localhost:8080/eventbus/");

// create the Express web server once the event bus is ready so we know we can use it straight away.
eventBus.onopen = function() {

  console.log("Event bus connected");

  setInterval(function() {

    var usage = process.cpuUsage();
    var totalCpu = usage.user + usage.system;
    var cpu = (totalCpu - usage.user) / totalCpu;
    var mem = process.memoryUsage().heapUsed;

    var metrics = {};
    metrics["" + process.pid] = {
      CPU: cpu,
      Mem: mem
    };

    eventBus.send("metrics", metrics);
  }, 1000);


};
