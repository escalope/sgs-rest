package net.sf.sgsimulator.sgsrest.vertx.controllers;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.github.aesteve.vertx.nubes.annotations.Controller;
import com.github.aesteve.vertx.nubes.annotations.params.Param;
import com.github.aesteve.vertx.nubes.annotations.params.Params;
import com.github.aesteve.vertx.nubes.annotations.params.ContextData;
import com.github.aesteve.vertx.nubes.annotations.routing.http.GET;
import com.github.aesteve.vertx.nubes.annotations.services.Service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import mired.ucm.grid.ElementEMS;
import mired.ucm.simulator.SensorContainerNotFound;
import net.sf.sgsimulator.sgsrest.vertx.services.GridLabSimulatorService;
import com.github.aesteve.vertx.nubes.annotations.Controller;

@Controller("/sg/pages")
public class PageController {

	@Service("GridLabSimulatorService")
	private GridLabSimulatorService gL;
	private Hashtable<String, Boolean> available = new Hashtable<String, Boolean>();

	private Hashtable<String, String> panelPerKey = new Hashtable<String, String>();
	int counter = 0;
	int id = 0;

	String billing = "<div><b>Current bill (€):</b> <div id=\"bill\"></div>";
	String chartMetering = "<div style=\"height: 200px;\">		<div id=\"chart1\"></div>	</div>";
	String chartWeather = "<div style=\"height: 200px;\">                <div id=\"chart2\"></div></div>";
	String ballance = "<b>Power balance (kW):</b> <div id=\"accumpower\"></div></div>";

	@GET("/panel")
	public synchronized String getPanel(RoutingContext ctx) {
		SocketAddress remote = ctx.request().remoteAddress();
		JsonObject obj = new JsonObject().put("host", remote.host()).put("port", remote.port());
		// ctx.response().end(obj.encodePrettily());
		String host = obj.getString("host");

		if (panelPerKey.containsKey(host))
			return panelPerKey.get(host);
		counter = counter + 1;
		String page = "Not working. Contact the administrator";

		if (System.getProperties().get("sgsimulator.scenario") == null)
			System.getProperties().put("sgsimulator.scenario", "solarbyperson");

		switch (System.getProperties().get("sgsimulator.scenario").toString().toLowerCase()) {
		case "fulloperational":

			String ptrans = ""; {
			int id = 0;
			for (String transformer : gL.getTransformerNames()) {
				ptrans = ptrans + "<h2>" + transformer + "</h2>\n";
				for (ElementEMS generator : gL.getGeneratorsByTransformer(transformer)) {
					ptrans = ptrans + generateButtonCode(generator.getName(), generator.getMaxPower(), id);
					id = id + 1;
				}
				;
			}
		}

			page = "<!DOCTYPE html><html><head><link href=\"/assets/sg.css\" />"
					+ "<style></style><meta charset=\"utf-8\"><title>Be a Solar Panel My Friend</title></head><body>	" + chartMetering
					+ chartWeather + ballance + ptrans
					+ "<script src=\"https://code.jquery.com/jquery-3.2.0.min.js\"></script>"
					+ "<script src=\"http://canvasjs.com/assets/script/canvasjs.min.js\"></script>"
					+ "<script src=\"/assets/sg.js\"></script><script  type=\"text/javascript\">"
					+ "updateCounterListener(\"bill\",\"#bill\");    "
					+ "updateCounterListener(\"powerdump\",\"#accumpower\");" + "charts.chart1.render();"
					+ "updateListeners(\"generation\",charts.chart1,LASTGENERATED);"
					+ "updateListeners(\"consumption\",charts.chart1,LASTCONSUMPTION);"
					+ "updateListeners(\"demand\",charts.chart1,LASTDEMAND);" + "charts.chart2.render();"
					+ "updateListeners(\"wind\",charts.chart2,WIND);" + "updateListeners(\"sun\",charts.chart2,SUN);"
					+ "</script>" + "</body>" + "</html>";

			break;
		case "solarbyperson":
			if (available.isEmpty()) {
				for (String transformer : gL.getTransformerNames()) {
					for (ElementEMS generator : gL.getGeneratorsByTransformer(transformer)) {
						available.put(generator.getName(), true);
					}
					;
				}
			}
			if (available.values().contains(true)) {
				String found = "";
				for (String key : available.keySet()) {
					if (available.get(key)) {
						found = key;
					}
				}
				if (found != "") {
					ptrans = "<H2>Assigned panels</h2>"
							+ generateButtonCode(found, this.gL.getElementByName(found).getMaxPower(), id);
					page = "<!DOCTYPE html><html><head><link href=\"/assets/sg.css\" />"
							+ "<style></style><meta charset=\"utf-8\"><title>SG Charts</title></head><body>	"
							+ chartMetering + chartWeather + ballance + ptrans
							+ "<script src=\"https://code.jquery.com/jquery-3.2.0.min.js\"></script>"
							+ "<script src=\"http://canvasjs.com/assets/script/canvasjs.min.js\"></script>"
							+ "<script src=\"/assets/sg.js\"></script><script  type=\"text/javascript\">"
							+ "updateCounterListener(\"bill\",\"#bill\");    "
							+ "updateCounterListener(\"powerdump\",\"#accumpower\");" + "charts.chart1.render();"
							+ "updateListeners(\"generation\",charts.chart1,LASTGENERATED);"
							+ "updateListeners(\"consumption\",charts.chart1,LASTCONSUMPTION);"
							+ "updateListeners(\"demand\",charts.chart1,LASTDEMAND);" + "charts.chart2.render();"
							+ "updateListeners(\"wind\",charts.chart2,WIND);"
							+ "updateListeners(\"sun\",charts.chart2,SUN);" + "</script>" + "</body>" + "</html>";
					id = id + 1;
				} else
					page = "All elements have been assigned. Try again later";
			}

		}
		panelPerKey.put(host, page);
		return page;

	}

	private String generateButtonCode(String name, double maxpower, int id) {
		// TODO Auto-generated method stub
		return "<button id=\"btn_" + id + "\" name=\""+name+"\"" + " onClick=deactivatePanel('" + name + "'," + id + ")>" + name
				+ ":last order was on" + "</button>:  max power is " + Math.abs(maxpower/1000) + "kW\n";
	}

}
