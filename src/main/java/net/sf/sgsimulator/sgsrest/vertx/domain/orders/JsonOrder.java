package net.sf.sgsimulator.sgsrest.vertx.domain.orders;

import java.util.Date;

import mired.ucm.properties.PATHS;
import mired.ucm.simulator.orders.Order;

public interface JsonOrder<T extends Order> {

	public T toOrder(Date timestamp,PATHS paths);
}
