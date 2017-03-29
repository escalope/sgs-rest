package net.sf.sgsimulator.sgsrest.vertx.controllers;

import com.github.aesteve.vertx.nubes.annotations.Controller;
import com.github.aesteve.vertx.nubes.annotations.mixins.ContentType;
import com.github.aesteve.vertx.nubes.annotations.params.RequestBody;
import com.github.aesteve.vertx.nubes.annotations.routing.http.POST;
import com.github.aesteve.vertx.nubes.annotations.services.Service;

import io.vertx.core.json.JsonObject;
import net.sf.sgsimulator.sgsrest.vertx.domain.orders.JsonSwitchOff;
import net.sf.sgsimulator.sgsrest.vertx.domain.orders.JsonSwitchOn;
import net.sf.sgsimulator.sgsrest.vertx.services.GridLabSimulatorService;

@Controller("/sg/execute")
public class OrderController {

	@Service("GridLabSimulatorService")
	private GridLabSimulatorService gridLab;

	@POST("/execute/switch-off")
	@ContentType("application/json")
	public JsonObject executeOrder(@RequestBody JsonSwitchOff order)
	{
		this.gridLab.executeOrder(order);
		return orderSent();

	}

	@POST("/execute/switch-on")
	@ContentType("application/json")
	public JsonObject executeOrder(@RequestBody JsonSwitchOn order)
	{
		this.gridLab.executeOrder(order);
		return orderSent();
	}

	private static JsonObject orderSent()
	{
		return new JsonObject()
				.put("status", "success")
				.put("result", "Order sent");
	}

}
