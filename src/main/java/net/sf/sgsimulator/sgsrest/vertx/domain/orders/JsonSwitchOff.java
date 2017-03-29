package net.sf.sgsimulator.sgsrest.vertx.domain.orders;

import java.util.Date;

import mired.ucm.properties.PATHS;
import mired.ucm.simulator.orders.SwitchOff;

public class JsonSwitchOff implements JsonOrder<SwitchOff> {

	private String deviceName;

	// serialization
	public JsonSwitchOff()
	{
	}

	public JsonSwitchOff(String deviceName)
	{
		this.deviceName = deviceName;
	}

	@Override
	public SwitchOff toOrder(Date timestamp, PATHS paths)
	{
		return new SwitchOff(this.deviceName, timestamp, paths);
	}

	public String getDeviceName()
	{
		return deviceName;
	}

}
