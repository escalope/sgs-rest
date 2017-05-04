package net.sf.sgsimulator.sgsrest.vertx.domain.orders;

import java.util.Date;

import mired.ucm.grid.TypesElement;
import mired.ucm.properties.PATHS;
import mired.ucm.simulator.orders.SetControl;
import mired.ucm.simulator.orders.SwitchOn;

public class JsonSetControlBattery implements JsonOrder<SetControl> {

	private String deviceName="";
	/*
	 * value >0 => discharge
	 * value <0 => charge
	 * value = 0 => stop charging/discharging
	 */
	private double value=0; 
	

	// serialization
	public JsonSetControlBattery()
	{
	}

	public JsonSetControlBattery(String deviceName, double value)
	{
		this.deviceName = deviceName;
		this.value=value;
	}

	@Override
	public SetControl toOrder(Date timestamp, PATHS paths)
	{
		return new SetControl(this.deviceName, timestamp,value, paths,TypesElement.BATTERY);
	}

	public double getValue() {
		return value;
	}

	public String getDeviceName()
	{
		return deviceName;
	}

}
