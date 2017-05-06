/*
	This file is part of SGSim-REST framework, a game to learn coordination protocols with microgrids
	
    Copyright (C) 2017 Rafael Pax, Jorge J. Gomez-Sanz

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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
