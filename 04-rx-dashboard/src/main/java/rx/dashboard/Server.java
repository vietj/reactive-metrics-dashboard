package rx.dashboard;

import io.reactivex.Observable;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Server extends AbstractVerticle {

  public static void main(String[] args) {
    io.vertx.core.Vertx.clusteredVertx(new VertxOptions(), ar -> {
      if (ar.succeeded()) {
        io.vertx.core.Vertx vertx = ar.result();
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
    options.setOutboundPermitted(Collections.singletonList(new PermittedOptions().setAddress("dashboard")));
    router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(options));

    // The proxy handler
    Observable<Message<JsonObject>> stream = vertx.eventBus()
      .<JsonObject>consumer("metrics")
      .toObservable();

    stream
      .map(msg -> msg.body())
      .buffer(1, TimeUnit.SECONDS)
      .map((List<JsonObject> metrics) -> {
        JsonObject dashboard = new JsonObject();
        for (JsonObject metric : metrics) {
          dashboard.mergeIn(metric);
        }
        return dashboard;
      }).subscribe(dashboard -> {
      vertx.eventBus().publish("dashboard", dashboard);
    });


    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080);
  }
}
