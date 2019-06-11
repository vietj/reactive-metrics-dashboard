package push.dashboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

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

  private JsonObject dashboard = new JsonObject();

  @Override
  public void start() {

    Router router = Router.router(vertx);

    // The web server handler
    router.route().handler(StaticHandler.create().setCachingEnabled(false));

    router.get("/dashboard").handler(ctx -> {
      ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(dashboard.encode());
    });

    vertx.eventBus().<JsonObject>consumer("metrics").handler(msg -> {
      JsonObject metrics = msg.body();
      dashboard.mergeIn(metrics);
    });


    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080);
  }
}
