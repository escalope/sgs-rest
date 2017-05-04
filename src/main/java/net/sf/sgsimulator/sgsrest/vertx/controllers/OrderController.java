package net.sf.sgsimulator.sgsrest.vertx.controllers;

import com.github.aesteve.vertx.nubes.annotations.Controller;
import com.github.aesteve.vertx.nubes.annotations.mixins.ContentType;
import com.github.aesteve.vertx.nubes.annotations.params.Param;
import com.github.aesteve.vertx.nubes.annotations.params.RequestBody;
import com.github.aesteve.vertx.nubes.annotations.routing.http.POST;
import com.github.aesteve.vertx.nubes.annotations.services.Service;

import io.vertx.core.json.JsonObject;
import net.sf.sgsimulator.sgsrest.vertx.domain.orders.JsonSetControlBattery;
import net.sf.sgsimulator.sgsrest.vertx.domain.orders.JsonSetControlGenerator;
import net.sf.sgsimulator.sgsrest.vertx.domain.orders.JsonSwitchOff;
import net.sf.sgsimulator.sgsrest.vertx.domain.orders.JsonSwitchOn;
import net.sf.sgsimulator.sgsrest.vertx.services.GridLabSimulatorService;

@Controller("/sg/execute")
public class OrderController {

	@Service("GridLabSimulatorService")
	private GridLabSimulatorService gridLab;

	@POST("/switch-off")
	@ContentType("application/json")
	public JsonObject executeOrder(@RequestBody JsonSwitchOff order)
			
	{ System.err.println("order1"+order);
	System.err.println("order1"+order.getDeviceName());
	
		this.gridLab.executeOrder(order );
		return orderSent();

	}

	@POST("/switch-on")
	@ContentType("application/json")
	public JsonObject executeOrder(@RequestBody JsonSwitchOn order)
	{
		this.gridLab.executeOrder(order);
		return orderSent();
	}
	
	@POST("/battery")
	@ContentType("application/json")
	public JsonObject executeOrder(@RequestBody JsonSetControlBattery order)
	{
		this.gridLab.executeOrder(order);
		return orderSent();
	}
	
	@POST("/generator")
	@ContentType("application/json")
	public JsonObject executeOrder(@RequestBody JsonSetControlGenerator order)
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
