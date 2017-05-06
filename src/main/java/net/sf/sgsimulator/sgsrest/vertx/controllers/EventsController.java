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
