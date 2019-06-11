package polling.dashboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.StaticHandler;

public class Server extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new Server());
  }

  @Override
  public void start() {

    Router router = Router.router(vertx);

    // The web server handler
    router.route().handler(StaticHandler.create().setCachingEnabled(false));

    // The proxy handler
    WebClient client = WebClient.create(vertx);
    HttpRequest<Buffer> get = client
      .get(8081, "localhost", "/");

    router.get("/dashboard").handler(ctx -> {

      get.send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> result = ar.result();
          ctx.response()
            .setStatusCode(result.statusCode())
            .putHeader("Content-Type", "application/json")
            .end(result.body());
        } else {
          ctx.fail(ar.cause());
        }
      });

    });

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080);
  }
}
