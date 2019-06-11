package bridge.dashboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.util.Collections;

public class Server extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx.clusteredVertx(new VertxOptions(), ar -> {
      if (ar.succeeded()) {
        Vertx vertx = ar.result();
        vertx.deployVerticle(new Server());
      } else {
        ar.cause().printStackTrace();
      }
    });
  }

  @Override
  public void start() {

    Router router = Router.router(vertx);

    // The web server handler
    router.route().handler(StaticHandler.create().setCachingEnabled(false));

    // The event bus bridge handler
    BridgeOptions options = new BridgeOptions();
    options.
      setOutboundPermitted(
        Collections.singletonList(
          new PermittedOptions().setAddress("dashboard")));

    // Uncomment this for node agent
    // options.setInboundPermitted(Collections.singletonList(new PermittedOptions().setAddress("metrics")));
    router.route("/eventbus/*")
      .handler(SockJSHandler.create(vertx).bridge(options));

    //
    JsonObject dashboard = new JsonObject();

    // Publish the dashboard to the browser over the bus
    vertx.setPeriodic(1000, timerID -> {
      vertx.eventBus().publish("dashboard", dashboard);
    });

    // The proxy handler
    vertx.eventBus().<JsonObject>consumer("metrics").handler(msg -> {
      JsonObject metrics = msg.body();
      dashboard.mergeIn(metrics);
    });


    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080);
  }
}
