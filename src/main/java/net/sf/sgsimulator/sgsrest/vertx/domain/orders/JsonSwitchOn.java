package net.sf.sgsimulator.sgsrest.vertx.domain.orders;

import java.util.Date;

import mired.ucm.properties.PATHS;
import mired.ucm.simulator.orders.SwitchOn;

public class JsonSwitchOn implements JsonOrder<SwitchOn> {

	private String deviceName;

	// serialization
	public JsonSwitchOn()
	{
	}

	public JsonSwitchOn(String deviceName)
	{
		this.deviceName = deviceName;
	}

	@Override
	public SwitchOn toOrder(Date timestamp, PATHS paths)
	{
		return new SwitchOn(this.deviceName, timestamp, paths);
	}

	public String getDeviceName()
	{
		return deviceName;
	}

}
