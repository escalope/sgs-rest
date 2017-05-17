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

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jfree.util.Log;

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

	String billing = "<div style='font-size: xx-large;'><b>Current bill (€):</b> <div id=\"bill\"></div></div>\n";
	String chartMetering = "<div style=\"height: 400px;\">		<div id=\"chart1\"></div>	</div>\n";
	String chartWeather = "<div style=\"height: 400px;\">                <div id=\"chart2\"></div></div>\n";
	String ballance = "<div style='font-size: xx-large;'><b>Power balance (kW):</b><div id=\"accumpower\"></div></div>\n";
	Hashtable<String,Vector<String>> remaining=new Hashtable<String,Vector<String>>(); 
	// based on http://stackoverflow.com/questions/18991540/randomly-shaking-an-array-to-assign-new-random-spots
	public void shake(Vector shakeable) 
	{
		for (int i = 0; i < shakeable.size(); i++)
		{
			int swap = (int) (Math.random()*(shakeable.size()));
			Object temp = shakeable.elementAt(swap);	        
			shakeable.setElementAt(shakeable.elementAt(i),swap);
			shakeable.setElementAt(temp,i);
		}
	}


	/*@GET("/admin/:code")
	public synchronized String getAdmin(RoutingContext ctx,
			@Param(value = "code", mandatory = true) String code) {*/
	@GET("/admin")
	public synchronized String getAdmin(RoutingContext ctx) {

		SocketAddress remote = ctx.request().remoteAddress();
		JsonObject obj = new JsonObject().put("host", remote.host()).put("port", remote.port());
		// ctx.response().end(obj.encodePrettily());
		String host = obj.getString("host");	
		String admincode=System.getProperties().get("sgsimulator.admincode").toString();

		/*if (!admincode.equalsIgnoreCase(code)){
			Log.error("ILLEGAL ACCESS FROM IP "+host);
			return "<H1>WARNING: illegal attempt to access admin interface from IP "+host+". Police will be notified </H1>";
		}*/

		if (allowedhost.isEmpty()){
			if (System.getProperties().get("sgsimulator.adminip") != null)
				if (System.getProperties().get("sgsimulator.adminip").equals ("-1")){
					allowedhost=host;
					System.err.println("First host will be the admin");
				} else
					allowedhost=System.getProperties().get("sgsimulator.adminip").toString();
		}

		if (!host.equalsIgnoreCase(allowedhost)){
			Log.error("ILLEGAL ACCESS FROM IP "+host);
			return "<H1>WARNING: illegal attempt to access admin interface from IP "+host+". Police will be notified </H1>";
		}


		String page = "Not working. Contact the administrator";

		if (System.getProperties().get("sgsimulator.scenario") == null)
			System.getProperties().put("sgsimulator.scenario", "solarbyperson");


		String ptrans = " $.getJSON('/sg/query/transformer-names', function (data) {"+
				"var items = [];\n"+
				"console.log(data);\n"+
				"$.each(data.result, function (index, tName) {\n"+
				"    updateTransformers(tName);\n"+
				"});\n"+
				"});\n";

		page = "<!DOCTYPE html><html><head><link href=\"/sg/assets/sg.css\" />\n"
				+ "<style></style><meta charset=\"utf-8\"><title>SG Charts</title></head><body>	\n"
				+" <H1>Admin panel</H1>"
				+ "<script src=\"https://code.jquery.com/jquery-3.2.0.min.js\"></script>\n"					
				+ chartMetering + 
				chartWeather +
				billing+
				ballance
				+"<H2>Assigned panels</h2>"
				+"<div id='transformer-elements' style=\"text-align: center;\"/>"
				+ "<script src=\"http://canvasjs.com/assets/script/canvasjs.min.js\"></script>\n"
				+ "<script src=\"/sg/assets/sg.js\"></script>\n"
				+"<script  type=\"text/javascript\">\n"
				+ "updateCounterListener(\"bill\",\"#bill\");    \n"
				+ "updateCounterListener(\"powerdump\",\"#accumpower\");\n"
				+ "updateListeners(\"generation\",charts.chart1,LASTGENERATED);\n"
				+ "updateListeners(\"consumption\",charts.chart1,LASTCONSUMPTION);\n"
				+ "updateListeners(\"demand\",charts.chart1,LASTDEMAND);\n"
				+ "updateListeners(\"wind\",charts.chart2,WIND);\n"
				+ "updateListeners(\"sun\",charts.chart2,SUN);\n" +
				ptrans +
				"</script>\n" + 
				"</body>\n" + 
				"</html>\n";

		return page;

	}
	String allowedhost="";
	@GET("/screen")
	public synchronized String getToProjectScreen(RoutingContext ctx) {
		SocketAddress remote = ctx.request().remoteAddress();
		JsonObject obj = new JsonObject().put("host", remote.host()).put("port", remote.port());
		// ctx.response().end(obj.encodePrettily());
		String host = obj.getString("host");		
		if (allowedhost.isEmpty()){
			if (System.getProperties().get("sgsimulator.adminip") != null)
				if (System.getProperties().get("sgsimulator.adminip").equals ("-1")){
					allowedhost=host;
					System.err.println("First host will be the admin");
				} else
					allowedhost=System.getProperties().get("sgsimulator.adminip").toString();
		}

		if (!host.equalsIgnoreCase(allowedhost)){
			Log.error("ILLEGAL ACCESS FROM IP "+host);
			return "<H1>WARNING: illegal attempt to access admin interface from IP "+host+". Police will be notified </H1>";
		}


		String page = "Not working. Contact the administrator";

		if (System.getProperties().get("sgsimulator.scenario") == null)
			System.getProperties().put("sgsimulator.scenario", "solarbyperson");

		page = "<!DOCTYPE html><html><head><link href=\"/sg/assets/sg.css\" />\n"
				+ "<style></style><meta charset=\"utf-8\"><title>SG Charts</title></head><body>	\n"
				+" <H1>Screen panel</H1>"
				+ "<script src=\"https://code.jquery.com/jquery-3.2.0.min.js\"></script>\n"					
				+ chartMetering + 
				chartWeather +
				billing+
				ballance					
				+"<div id='transformer-elements' style=\"text-align: center;\"/>"
				+ "<script src=\"http://canvasjs.com/assets/script/canvasjs.min.js\"></script>\n"
				+ "<script src=\"/sg/assets/sg.js\"></script>\n"
				+"<script  type=\"text/javascript\">\n"
				+ "updateCounterListener(\"bill\",\"#bill\");    \n"
				+ "updateCounterListener(\"powerdump\",\"#accumpower\");\n"
				+ "updateListeners(\"generation\",charts.chart1,LASTGENERATED);\n"
				+ "updateListeners(\"consumption\",charts.chart1,LASTCONSUMPTION);\n"
				+ "updateListeners(\"demand\",charts.chart1,LASTDEMAND);\n"
				+ "updateListeners(\"wind\",charts.chart2,WIND);\n"
				+ "updateListeners(\"sun\",charts.chart2,SUN);\n" 	+				
				"</script>\n" + 
				"</body>\n" + 
				"</html>\n";


		return page;

	}




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

			String ptrans = " $.getJSON('/sg/query/transformer-names', function (data) {"+
					"var items = [];\n"+
					"console.log(data);\n"+
					"$.each(data.result, function (index, tName) {\n"+
					"    updateTransformers(tName);\n"+
					"});\n"+
					"});\n";

			/*{
			int id = 0;
			for (String transformer : gL.getTransformerNames()) {
				ptrans = ptrans + "<h1>" + transformer + "</h1>\n";
				for (ElementEMS generator : gL.getGeneratorsByTransformer(transformer)) {
					ptrans = ptrans + generateButtonCode(generator.getName(), generator.getMaxPower(), id);
					id = id + 1;
				}
				;
			}
		}*/

			page = "<!DOCTYPE html><html><head><link href=\"/sg/assets/sg.css\" />\n"
					+ "<style></style><meta charset=\"utf-8\"><title>SG Charts</title></head><body>	\n"
					+ "<script src=\"https://code.jquery.com/jquery-3.2.0.min.js\"></script>\n"					
					+ chartMetering + 
					chartWeather + 
					ballance
					+"<H2>Assigned panels</h2>"
					+"<div id='transformer-elements' style=\"text-align: center;\"/>"
					+ "<script src=\"http://canvasjs.com/assets/script/canvasjs.min.js\"></script>\n"
					+ "<script src=\"/sg/assets/sg.js\"></script>\n"
					+"<script  type=\"text/javascript\">\n"
					+ "updateCounterListener(\"bill\",\"#bill\");    \n"
					+ "updateCounterListener(\"powerdump\",\"#accumpower\");\n"
					+ "updateListeners(\"generation\",charts.chart1,LASTGENERATED);\n"
					+ "updateListeners(\"consumption\",charts.chart1,LASTCONSUMPTION);\n"
					+ "updateListeners(\"demand\",charts.chart1,LASTDEMAND);\n"
					+ "updateListeners(\"wind\",charts.chart2,WIND);\n"
					+ "updateListeners(\"sun\",charts.chart2,SUN);\n" +
					ptrans +
					"</script>\n" + 
					"</body>\n" + 
					"</html>\n";
			id = id + 1;

			break;
		case "centralized":
			// one 1 weather + 1 solar panel per person except the controller
			// controller has weather+metering+billing+ballance
			if (remaining.isEmpty()){
				int id = 0;
				Vector<String> panels=new Vector<String>();
				remaining.put("panel", panels);
				for (String transformer : gL.getTransformerNames()) {					
					for (ElementEMS generator : gL.getGeneratorsByTransformer(transformer)) {						
						panels.add(generator.getName());
					}
					;
				};			
				Vector<String> centrals=new Vector<String>();
				centrals.add("firstandonly");
				remaining.put("central",centrals);
				shake(panels); // make vector randomly organized
				shake(centrals); // make vector randomly organized
			}

			if (remaining.get("panel").size()==0 &&remaining.get("central").size()==0){
				page ="<h1>No more panels, sorry. Wait for the microgrid to be restarted</h1>";
			} else {
				if (remaining.get("panel").size()>0){						
					String panel=remaining.get("panel").remove(0);
					int id=0;
					String button = generateButtonCode("transformer-elements", panel,Math.abs(this.gL.getElementByName(panel).getMaxPower()/1000), id);
					page="<!DOCTYPE html><html><head><link href=\"/sg/assets/sg.css\" />\n"
							+ "<style></style><meta charset=\"utf-8\"><title>SG Charts</title></head><body>	\n"								
							+"<H1>Solar Panel node :"+panel+" </H1>"							
							+ "<script src=\"https://code.jquery.com/jquery-3.2.0.min.js\"></script>\n"+
							chartWeather
							+"<div id='transformer-elements' style=\"text-align: center;\"/>"
							+ "<script src=\"http://canvasjs.com/assets/script/canvasjs.min.js\"></script>\n"
							+ "<script src=\"/sg/assets/sg.js\"></script>\n"
							+"<script  type=\"text/javascript\">\n"
							+ "updateListeners(\"wind\",charts.chart2,WIND);\n"
							+ "updateListeners(\"sun\",charts.chart2,SUN);\n" +	
							button+
							"</script>\n" + 
							"</body>\n" + 
							"</html>\n";
				}  else
					if (remaining.get("central").size()>0){
						page="<!DOCTYPE html><html><head><link href=\"/sg/assets/sg.css\" />\n"
								+ "<style></style><meta charset=\"utf-8\"><title>SG Charts</title></head><body>	\n"							
								+"<H1>SCADA central node</H1>"
								+ "<script src=\"https://code.jquery.com/jquery-3.2.0.min.js\"></script>\n"					
								+ chartMetering + 
								chartWeather + 
								ballance+
								billing
								+ "<script src=\"http://canvasjs.com/assets/script/canvasjs.min.js\"></script>\n"
								+ "<script src=\"/sg/assets/sg.js\"></script>\n"
								+"<script  type=\"text/javascript\">\n"
								+ "updateCounterListener(\"bill\",\"#bill\");    \n"
								+ "updateCounterListener(\"powerdump\",\"#accumpower\");\n"
								+ "updateListeners(\"generation\",charts.chart1,LASTGENERATED);\n"
								+ "updateListeners(\"consumption\",charts.chart1,LASTCONSUMPTION);\n"
								+ "updateListeners(\"demand\",charts.chart1,LASTDEMAND);\n"
								+ "updateListeners(\"wind\",charts.chart2,WIND);\n"
								+ "updateListeners(\"sun\",charts.chart2,SUN);\n" +							
								"</script>\n" + 
								"</body>\n" + 
								"</html>\n";
						remaining.get("central").remove(0);
					}  

			}
			break;


		case "fullsolarbyperson":
		default:
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
					ptrans = generateButtonCode("transformer-elements",found, this.gL.getElementByName(found).getMaxPower(), id);
					page = "<!DOCTYPE html><html><head><link href=\"/sg/assets/sg.css\" />\n"
							+ "<style></style><meta charset=\"utf-8\"><title>SG Charts</title></head><body>	\n"
							+ "<script src=\"https://code.jquery.com/jquery-3.2.0.min.js\"></script>\n"

							+ chartMetering + 
							chartWeather + 
							ballance
							+"<H1>Assigned panels</h1>"
							+"<div id='transformer-elements'/>"
							+ "<script src=\"http://canvasjs.com/assets/script/canvasjs.min.js\"></script>\n"
							+ "<script src=\"/sg/assets/sg.js\"></script>\n"
							+"<script  type=\"text/javascript\">\n"
							+ "updateCounterListener(\"bill\",\"#bill\");    \n"
							+ "updateCounterListener(\"powerdump\",\"#accumpower\");\n"
							+ "updateListeners(\"generation\",charts.chart1,LASTGENERATED);\n"
							+ "updateListeners(\"consumption\",charts.chart1,LASTCONSUMPTION);\n"
							+ "updateListeners(\"demand\",charts.chart1,LASTDEMAND);\n"
							+ "updateListeners(\"wind\",charts.chart2,WIND);\n"
							+ "updateListeners(\"sun\",charts.chart2,SUN);\n" +
							ptrans +
							"</script>\n" + 
							"</body>\n" + 
							"</html>\n";
					id = id + 1;
				} else
					page = "All elements have been assigned. Try again later";
			}

		}
		panelPerKey.put(host, page);
		return page;

	}

	private String generateButtonCode(String mainpanel,String name, double maxkw, int id) {
		// TODO Auto-generated method stub
		return "generatePanel('"+mainpanel+"','"+name+"',"+id+","+maxkw+");\n";										
	}

}
