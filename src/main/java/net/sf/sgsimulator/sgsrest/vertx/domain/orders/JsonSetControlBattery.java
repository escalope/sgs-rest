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
