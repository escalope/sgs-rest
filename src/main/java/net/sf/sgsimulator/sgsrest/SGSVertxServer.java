package net.sf.sgsimulator.sgsrest;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.github.aesteve.vertx.nubes.VertxNubes;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class SGSVertxServer {

	public static void main(String[] args) throws Exception
	{
		Vertx vertx = Vertx.vertx();
		JsonObject config = new JsonObject(
				new String(Files.readAllBytes(Paths.get("config.json"))));
		VertxNubes nubes = new VertxNubes(vertx, config);
		nubes.bootstrap(res -> {
			if (res.succeeded())
			{
				final Router router = res.result();
				final HttpServer server = vertx
						.createHttpServer(new HttpServerOptions(config));
				server.requestHandler(router::accept);
				server.listen();
			} else
			{
				res.cause().printStackTrace();
			}
		});
		// nubes.stop((_void) -> {
		// vertx.close();
		// });
	}

}
