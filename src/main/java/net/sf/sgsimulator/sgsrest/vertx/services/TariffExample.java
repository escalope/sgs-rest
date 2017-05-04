package net.sf.sgsimulator.sgsrest.vertx.services;

import java.awt.Color;
import java.util.Calendar;
import java.util.Date;

import mired.ucm.price.Tariff;

/**
 * Example of tariff with two periods
 * 
 * @author Nuria Cuartero-Soler
 * 
 */
public class TariffExample implements Tariff {

	@Override
	public String getPeriod(Date date)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
				|| calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
		{
			return "P1";
		} else
		{
			return "P2";
		}
	}

	@Override
	public double getEnergyPrice(String period)
	{
		if (period.equals("P1"))
		{
			return 0.068219;
		} else
		{
			return 0.045724;
		}
	}

	@Override
	public Color getPeriodColor(String period)
	{
		if (period.equals("P1"))
		{
			return Color.red;
		} else
		{
			return Color.orange;
		}
	}

	@Override
	public double getFineForExporting(String period) {
		// TODO Auto-generated method stub
		if (period.equals("P1"))
		{
			return 0.0068219;
		} else
		{
			return 0.0045724;
		}
	}

}
