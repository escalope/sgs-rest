package net.sf.sgsimulator.sgsrest.vertx.controllers;

import java.io.IOException;

import com.github.aesteve.vertx.nubes.annotations.Controller;
import com.github.aesteve.vertx.nubes.annotations.routing.http.GET;
import com.github.aesteve.vertx.nubes.annotations.services.Service;

import info.macias.sse.vertx3.VertxEventTarget;
import io.vertx.ext.web.RoutingContext;
import net.sf.sgsimulator.sgsrest.vertx.services.GridLabSimulatorService;

@Controller("/sg/events")
public class EventsController {

	@Service("GridLabSimulatorService")
	private GridLabSimulatorService gridLab;

	@GET("/")
	public void eventStream(RoutingContext ctx) throws IOException
	{
		VertxEventTarget target = new VertxEventTarget(ctx.request())
				.ok().open();
		long listenerId = this.gridLab.addListener((evt, lId) -> {
			try
			{
				target.send(evt.getString("type"), evt.toString());
			} catch (IOException e)
			{
				this.gridLab.removeListener(lId);
			}
		});
		ctx.request().response().closeHandler(_v -> {
			this.gridLab.removeListener(listenerId);
		});

	}

}
