package net.sf.sgsimulator.sgsrest.vertx.controllers;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.github.aesteve.vertx.nubes.annotations.Controller;
import com.github.aesteve.vertx.nubes.annotations.params.Param;
import com.github.aesteve.vertx.nubes.annotations.routing.http.GET;
import com.github.aesteve.vertx.nubes.annotations.services.Service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import mired.ucm.simulator.SensorContainerNotFound;
import net.sf.sgsimulator.sgsrest.vertx.services.GridLabSimulatorService;

@Controller("/sg/query")
public class QueryController {

	@Service("GridLabSimulatorService")
	private GridLabSimulatorService gL;

	@GET("/applied-orders")
	public String getAppliedOrdersSince(
			@Param(value = "timestamp", mandatory = true) Long timestamp)
	{
		return jsonResponse(() -> this.gL.getAppliedOrdersSince(timestamp));
	}

	@GET("/system-consumption")
	public String getSystemConsumption()
	{
		return jsonResponse(gL::getSystemConsumption);
	}

	@GET("/substation-sensors")
	public String getSubstationSensors()
	{
		return jsonResponse(gL::getSubstationSensors);
	}

	@GET("/losses-sensor")
	public String getLossesSensor()
			throws RemoteException
	{
		return jsonResponse(gL::getLossesSensor);
	}

	@GET("/transformer-names")
	public String getTransformerNames()
	{
		return jsonResponse(() -> {
			JsonArray res = new JsonArray();
			gL.getTransformerNames().forEach(res::add);
			return res;
		});
	}

	@GET("/transformer-sensors")
	public String getTransformerSensors(
			@Param(value = "transformer-name", mandatory = true) String tN,
			@Param(value = "timestamp", mandatory = false) Long ts)
			throws SensorContainerNotFound
	{
		return jsonResponse(() -> gL.getTransformerSensors(tN, ts));

	}

	@GET("/transformer-elements")
	public String getElementsByTransformer(
			@Param(value = "transformer-name", mandatory = true) String tN)
	{
		return jsonResponse(() -> gL.getElementsByTransformer(tN).stream()
				.map(e -> e.getName()).collect(Collectors.toList()));

	}

	@GET("/device-sensors")
	public String getDeviceSensors(
			@Param(value = "transformer-name", mandatory = true) String tName,
			@Param(value = "device-name", mandatory = true) String dN,
			@Param(value = "timestamp", mandatory = false) Long ts)
	{
		return jsonResponse(() -> this.gL.getDeviceSensors(dN, ts));
	}

	@GET("/current-sun")
	public String getCurrentSun(
			@Param(value = "timestamp", mandatory = false) Long timestamp)
	{
		return jsonResponse(() -> gL.getCurrentSun(timestamp));
	}

	@GET("/current-wind")
	public String getCurrentWind(
			@Param(value = "timestamp", mandatory = false) Long timestamp)
	{
		return jsonResponse(() -> gL.getCurrentWind(timestamp));
	}

	@GET("/current-energy")
	public String getCurrentEnergy(
			@Param(value = "battery-name", mandatory = true) String batteryName,
			@Param(value = "timestamp", mandatory = false) Long timestamp)
			throws RemoteException
	{
		return jsonResponse(() -> gL.getCurrentEnergy(batteryName, timestamp));
	}

	@GET("/server-status")
	public String isServerActive()
	{
		return jsonResponse(() -> gL.isServerActive() ? "running" : "stopped");
	}

	@GET("/pending-orders")
	public String /* isSimulatorBusy() */ getPendingOrdersSize()
	{
		return jsonResponse(() -> gL.getPendingOrdersSize());
	}

	private static <T> /* JsonObject */ String jsonResponse(Callable<T> action)
	{
		try
		{
			return new JsonObject()
					.put("status", "success")
					.put("result", action.call()).toString();
		} catch (Exception e)
		{
			return new JsonObject()
					.put("status", "error")
					.put("cause", e.getMessage()).toString();
		}
	}

}
