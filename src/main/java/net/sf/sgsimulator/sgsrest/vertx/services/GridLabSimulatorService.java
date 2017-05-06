/*
	This file is part of SGSim-REST framework, a game to learn coordination protocols with microgrids
	
    Copyright (C) 2017 Rafael Pax, Jorge J. Gomez-Sanz
    based on code by Jorge J. Gomez Sanz, Nuria Cuartero-Soler, Sandra Garcia-Rodriguez

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

package net.sf.sgsimulator.sgsrest.vertx.services;

import java.awt.Color;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.github.aesteve.vertx.nubes.services.Service;
import com.github.aesteve.vertx.nubes.utils.functional.TriConsumer;

import io.netty.handler.codec.http2.Http2HeadersEncoder.SensitivityDetector;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import mired.ucm.dynamicGLM.XMLUtilities;
import mired.ucm.grid.ElementEMS;
import mired.ucm.grid.TypesElement;
import mired.ucm.grid.TypesGenerator;
import mired.ucm.monitor.Event;
import mired.ucm.monitor.ReadScenario;
import mired.ucm.price.Tariff;
import mired.ucm.properties.PATHS;
import mired.ucm.simulator.GridLabDiscreteEventSimulator;
import mired.ucm.simulator.GridlabActions;
import mired.ucm.simulator.GridlabException;
import mired.ucm.simulator.NetworkElementsStatus;
import mired.ucm.simulator.NetworkListener;
import mired.ucm.simulator.NetworkStatus;
import mired.ucm.simulator.SensorContainerNotFound;
import mired.ucm.simulator.SensorIDS;
import mired.ucm.simulator.SimulatorNotInitializedException;
import mired.ucm.simulator.NetworkStatus.UnknownSensor;
import mired.ucm.simulator.displayer.CtsDisplayer;
import mired.ucm.simulator.displayer.StateWindow;
import mired.ucm.simulator.orders.Order;
import mired.ucm.simulator.orders.SwitchOn;
import mired.ucm.viewer.PowerGridViewer;
import net.sf.sgsimulator.sgsrest.vertx.domain.orders.JsonOrder;

@SuppressWarnings({ "unused", "rawtypes" })
public class GridLabSimulatorService implements NetworkListener, Service {

	private AtomicLong listenerCount = new AtomicLong(0);
	private ConcurrentHashMap<Long, Consumer<JsonObject>> handlers = new ConcurrentHashMap<>();

	private SimpleDateFormat sdf;
	private DecimalFormat decimalFormat;
	private int moments;
	private NetworkElementsStatus nes;
	private boolean intelligence;
	private GridlabActions gridlabActions;
	private int hoursOfSimulation;
	private ArrayBlockingQueue demandCurve;
	private String title;
	private int cycleTimeInMillis;
	private float importedEnergy;
	private float exportedEnergy;
	private float energyLosses;
	private float bill;
	private Tariff tariff;
	private boolean activeClients;
	private ReadScenario rs;
	private String currentTariffPeriod;
	private double currentTariffPrice;
	private GridLabDiscreteEventSimulator gldes;
	private Date startDate;
	private PATHS paths;

	private void onInit(Vertx vertx, JsonObject config) throws IOException
	{
		config = config.getJsonObject("gridlab");
		int cycleTimeInMillis = config.getInteger("cycleTimeInMillis");// 60000;
		boolean intelligence = config.getBoolean("intelligence");// true;
		int moments = config.getInteger("moments");// 30;
		Double minRange = config.getDouble("minRange", null);
		Double maxRange = config.getDouble("maxRange", null);
		boolean activeClients = config.getBoolean("activeClients");// true;
		// TODO parametrize
		Tariff tariff = new TariffExample();

		JsonArray sdjson = config.getJsonArray("startDate");
		JsonArray edjson = config.getJsonArray("endDate");
		Calendar calendarStart = Calendar.getInstance();
		calendarStart.set(
				sdjson.getInteger(0), sdjson.getInteger(1),
				sdjson.getInteger(2), sdjson.getInteger(3),
				sdjson.getInteger(4), sdjson.getInteger(5));
		
		Calendar calendarEnd = Calendar.getInstance();
		calendarEnd.set(
				edjson.getInteger(0), edjson.getInteger(1),
				edjson.getInteger(2), edjson.getInteger(3),
				edjson.getInteger(4), edjson.getInteger(5));

		
		String configurationFile = config.getString("configurationFile");
		String gridFile = config.getString("gridFile");
		// definition
		String scenarioFile = config.getString("scenarioFile");
		String gridLabFolder = config.getString("gridLabFolder");
		PATHS paths = new PATHS(configurationFile, gridFile, scenarioFile,
				gridLabFolder);

		Date[] dates = readSimulationTime(paths.getNetXmlSrcFile());
		long diffInMillies = calendarEnd.getTimeInMillis() - calendarStart.getTimeInMillis();
		
		int hoursOfSimulation = (int) TimeUnit.HOURS.convert(diffInMillies,
				TimeUnit.MILLISECONDS);
		startDate=calendarStart.getTime();
		init(title, startDate, cycleTimeInMillis, paths, intelligence,
				hoursOfSimulation, tariff, moments, minRange, maxRange,
				activeClients);
	}

	private void onStart(Future<Void> future)
	{
		try
		{
			initSimulator(startDate, 14, cycleTimeInMillis, paths,
					intelligence);
			this.gldes.start();
			future.complete();
			for (ElementEMS generator:this.nes.getGenerators()){
				if (generator.getType().equals(TypesElement.GENERATOR)){
					this.gldes.getSimulator()
					.queueOrder(new SwitchOn(generator.getName(), getTime(), paths));					
				}
			}
			
		} catch (GridlabException | SimulatorNotInitializedException e)
		{ e.printStackTrace();
			future.fail(e);
		}
	}

	private void onStop(Future<Void> future)
	{
		try
		{
			this.gldes.stop();
			future.complete();
		} catch (TimeoutException e)
		{
			future.fail(e);
		}

	}

	private void sendEvent(Date timestampDate, String type, Object value)
	{
		long timestamp = timestampDate.getTime();
		JsonObject evt = new JsonObject()
				.put("timestamp", timestamp)
				.put("type", type)
				.put("value", value);

		this.handlers.values().forEach(h -> h.accept(evt));
	}

	@Override
	public void processCycleStatus(
			NetworkStatus ns,
			NetworkElementsStatus nes,
			Date timestampDate)
	{

		/*
		 * Consumption
		 */
		this.sendEvent(
				timestampDate,
				"system-consumption",
				this.rs.getConsumption(nes.getElements(),
						timestampDate));
		/*
		 * Substation sensors
		 */
		ns.getSubstationSensors().forEach((sensorId, value) -> {
			this.sendEvent(
					timestampDate,
					"substation-sensor",
					new JsonObject()
							.put("sensorId", sensorId)
							.put("value", value));
		});
		/*
		 * Overvoltage
		 */
		this.sendEvent(
				timestampDate,
				"overvoltage",
				ns.thereIsOvervoltage());

		/*
		 * Transformer sensors
		 */
		forEachTransformerSensor(timestampDate, (tName, sensorId, value) -> {

			this.sendEvent(timestampDate, "transformer-sensor",
					new JsonObject()
							.put("transformer-name", tName)
							.put("sensorId", sensorId)
							.put("value", value));
		});
		/*
		 * Sun percentage
		 */
		this.sendEvent(
				timestampDate,
				"sun",
				this.rs.getSunPercentage(timestampDate));
		/*
		 * Wind
		 */
		this.sendEvent(
				timestampDate,
				"wind",
				this.rs.getWindPercentage(timestampDate));
		/*
		 * Generation sum
		 */
		this.sendEvent(
				timestampDate,
				"generation-sum",
				nes.calculateGenerationSum() / 1000.0F);
		

		this.sendEvent(
				timestampDate,
				"demand",
				getLastKWDemand());
		
		this.sendEvent(
				timestampDate,
				"bill",
				getCurrentBill());
		
		this.sendEvent(
				timestampDate,
				"export",
				getLastKWExport());
		
		this.sendEvent(
				timestampDate,
				"import",
				getLastKWImport());
		
		this.sendEvent(
				timestampDate,
				"consumption",
				getLastKWConsumption());
		
		this.sendEvent(
				timestampDate,
				"generation",
				getLastKWGeneration());
		
		this.sendEvent(
				timestampDate,
				"powerdump",
				getLastKWConsumption()-getLastKWGeneration());
		
		
		/**
		 * Compute bill, generated energy, and 
		 */
				
		computeAdditionalCycleStatus(ns,nes,timestampDate);
	}
	


	protected int NUMBER_OF_SERIES=4;
	protected int CONSUMPTION_SERIES_INDEX = 0;
	protected int GENERATION_SERIES_INDEX = 1;
	protected int DEMAND_SERIES_INDEX = 2;
	protected int LOSSES_SERIES_INDEX = 3;
	protected final int timeUnit = Calendar.MILLISECOND;
	


	private long realTimeCycleLengthMillis;
	private Object lastTimeStamp;
	private double lastExport;
	private double lastConsumption;
	
	
	public float getCurrentBill(){
		return bill;
	}
	
	public float getTotalImportedEnergykWh(){
		return importedEnergy;
	}
	
	public float getTotalExportedEnergykWh(){
		return exportedEnergy;
	}
	
	public double getLastKWImport(){
		return lastImport;
	}
	
	public double getLastKWDemand(){
		return lastDemand;
	}
	
	
	public double getLastKWGeneration(){
		return lastGeneration;
	}
	public double getLastKWExport(){
		return lastExport;
	}
	public double getLastKWConsumption(){
		return lastConsumption;
	}
	
	
	
	private double lastImport=0;// energy demand from the microgrid
	private double lastGeneration=0;// energy generation from the microgrid
	private double lastDemand=0;// energy generation from the microgrid
	public void computeAdditionalCycleStatus(NetworkStatus ns, NetworkElementsStatus nes, Date timestamp) {
		Date init=new Date(); // to measure cycle length
		
		if (ns.thereIsOvervoltage())
			System.err.println(ns.getOvervoltageTimestamp());
		
		// UPDATING SIMULATION CHART
		float[] newSimulationData = new float[NUMBER_OF_SERIES];
		newSimulationData[CONSUMPTION_SERIES_INDEX] = (float)rs.getConsumption(nes.getAvailableElements(),timestamp); // Getting consumption of energy of the next simtime
		newSimulationData[GENERATION_SERIES_INDEX] = (float) nes.calculateGenerationSum()/1000; // Getting the sum of the generation of the grid of the next simtime, in kW

		
		try {
			newSimulationData[DEMAND_SERIES_INDEX] = (float) ns.getSubstationSensor(SensorIDS.POWER_DEMAND)/1000; // Getting substation demand of the next simtime, in kW
			newSimulationData[LOSSES_SERIES_INDEX] = (float) ns.getSubstationSensor(SensorIDS.LOSSES)/1000; // Getting losses of the next simtime, in kW
		} catch (UnknownSensor e) {
			e.printStackTrace();
		}

		//Updating data for summary chart (substation demand)
		
		//	demandCurve.offer(newSimulationData[DEMAND_SERIES_INDEX]);
			lastDemand=newSimulationData[DEMAND_SERIES_INDEX];
			lastGeneration=newSimulationData[GENERATION_SERIES_INDEX];
			lastConsumption=newSimulationData[CONSUMPTION_SERIES_INDEX] ;
			
		
	//		System.out.println(getTime()+"=demand:"+lastDemand+" gen:"+lastGeneration+" lastcons:"+lastConsumption+" import:"+lastImport+" export:"+lastExport);

		

	

		// Updating tariff period and price
		if(tariff!=null){
			//labelTariff.setVisible(true);
			//labelPeriod.setVisible(true);
			currentTariffPeriod = tariff.getPeriod(timestamp);
			currentTariffPrice = tariff.getEnergyPrice(currentTariffPeriod);
			//labelPeriod.setText(currentTariffPeriod+" ("+currentTariffPrice+" €/kWh)");
			//labelPeriod.setForeground(tariff.getPeriodColor(currentTariffPeriod));
		}
		
		// Updating system losses
		//labelLosses.setVisible(true);
		//labelLosses.setText("Losses: "+decimalFormat.format(newSimulationData[LOSSES_SERIES_INDEX])+" kW");

		// Calculating imported energy, exported energy and bill
		double energyOfLastCycle = (newSimulationData[DEMAND_SERIES_INDEX]*(cycleTimeInMillis/60000.0))/1000;
		if(newSimulationData[DEMAND_SERIES_INDEX]>=0){
			importedEnergy+=energyOfLastCycle;
			if(tariff!=null){
				bill+=(energyOfLastCycle*currentTariffPrice);
			}
			lastImport=Math.abs(energyOfLastCycle);
			lastExport=0;
		}else{
			exportedEnergy+=Math.abs(energyOfLastCycle);
			if(tariff!=null){
				bill+=(Math.abs(energyOfLastCycle)*tariff.getFineForExporting(tariff.getPeriod(timestamp)));
			}
			lastImport=0;
			lastExport=Math.abs(energyOfLastCycle);
		}

		// Calculating energy losses
		energyLosses+=(newSimulationData[LOSSES_SERIES_INDEX]*(cycleTimeInMillis/60000.0));

		// UPDATING WEATHER CHART
		float[] newWeatherData = new float[2];
		newWeatherData[0] =  (float)rs.getSunPercentage(timestamp); // Getting the % of sun of the next simulation time
		newWeatherData[1] = (float)rs.getWindPercentage(timestamp); // Getting the % of wind of the next simulation time
		//weatherChart.getDataset().advanceTime();  // Advance to the next simtime
		//weatherChart.getDataset().appendData(newWeatherData); //Append new data to the chart dataset
		try {
			long millisSpent=(new Date().getTime()-init.getTime());
			if (realTimeCycleLengthMillis>millisSpent)
			Thread.currentThread().sleep(realTimeCycleLengthMillis-millisSpent);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		lastTimeStamp=timestamp;
	} // End of processCycleStatus

	private void forEachTransformerSensor(
			Date timestamp,
			TriConsumer<String, String, Float> action)
	{
		for (String tName : getTransformerNames())
		{
			getTransformerSensors(tName, timestamp);
		}
	}

	public Set<ElementEMS> getElementsByTransformer(String transformerName)
	{
		return this.nes.getElementsByTransformer(transformerName);
	}
	
	public Set<ElementEMS> getGeneratorsByTransformer(String transformerName)
	{
		HashSet<ElementEMS> set = new HashSet<ElementEMS>(this.nes.getElementsByTransformer(transformerName));
		 set.removeIf(s -> s.getType()!=TypesElement.GENERATOR);
		 return set;
	}

	public Hashtable<SensorIDS, Float> getTransformerSensors(String tName,
			Long timestamp) throws SensorContainerNotFound
	{
		if (timestamp == null)
		{
			return this.gridlabActions.getTransformerSensors(tName, getTime());
		} else
		{
			return this.gridlabActions.getTransformerSensors(tName,
					new Date(timestamp));
		}
	}

	public Hashtable<SensorIDS, Float> getTransformerSensors(String tName,
			Date timestamp)
	{
		try
		{
			return this.gridlabActions.getTransformerSensors(tName, timestamp);
		} catch (SensorContainerNotFound e)
		{
			throw new RuntimeException(e);
		}
	}

	public Iterable<String> getTransformerNames()
	{
		return nes.getTransformers().stream().map(t -> t.getName())::iterator;
	}

	public long addListener(BiConsumer<JsonObject, Long> handler)
	{
		long handlerId = listenerCount.incrementAndGet();
		this.handlers.put(handlerId, (jobj) -> handler.accept(jobj, handlerId));
		return handlerId;
	}

	public void removeListener(long listenerId)
	{
		this.handlers.remove(listenerId);
	}

	public void executeOrder(JsonOrder<?> order)
	{
		System.err.println(order);
		this.gldes.getSimulator()
				.queueOrder(order.toOrder(getTime(), this.paths));
	}

	private Date getTime()
	{
		return this.gldes.getSimTime();
	}

	@Override
	public void processEvent(Event event)
	{
		System.out.println("EVENT: " + event);
	}

	private AtomicBoolean initiated = new AtomicBoolean(false);
	private AtomicBoolean started = new AtomicBoolean(false);
	private AtomicBoolean stopped = new AtomicBoolean(false);

	@Override
	public void init(Vertx vertx, JsonObject config)
	{
		if (!initiated.getAndSet(true))
		{
			try
			{
				this.onInit(vertx, config);

			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

	}

	@Override
	public void start(Future<Void> future)
	{
		if (!started.getAndSet(true))
		{
			this.onStart(future);
		} else
		{
			future.complete();
		}

	}

	@Override
	public void stop(Future<Void> future)
	{
		if (!stopped.getAndSet(true))
		{
			this.onStop(future);
		} else
		{
			future.complete();
		}

	}

	/**
	 * Code taken from {@link mired.ucm.simulator.displayer.SimulatorDisplayer}
	 */

	/**
	 * @formatter:off
	 */
	/*     */   private void init(String title, Date startDate, int cycleTimeInMillis, PATHS paths, boolean intelligence, int hoursOfSimulation, Tariff tarif, int moments, Double minRange, Double maxRange, boolean activeClients)
	/*     */   {
				  this.paths=paths;
				  this.startDate=startDate;
	/* 209 */     this.sdf = new SimpleDateFormat("HH:mm:ss");
	/* 210 */     this.decimalFormat = new DecimalFormat("#.##");
	/* 211 */     this.moments = moments;
	/* 212 */     this.nes = new NetworkElementsStatus();
	/* 213 */     this.intelligence = intelligence;
	/* 214 */     this.gridlabActions = new GridlabActions(paths);
	/* 215 */     this.hoursOfSimulation = hoursOfSimulation;
	/* 216 */     this.demandCurve = new ArrayBlockingQueue(100);
	/* 217 */     this.title = title;
	/* 218 */     this.cycleTimeInMillis = cycleTimeInMillis;
	/* 219 */     this.importedEnergy = 0.0F;
	/* 220 */     this.exportedEnergy = 0.0F;
	/* 221 */     this.energyLosses = 0.0F;
	/* 222 */     this.bill = 0.0F;
	/* 223 */     this.tariff = tarif;
//	/* 224 */     this.mainFrame = new JFrame(title);
	/* 225 */     this.activeClients = activeClients;
//	/* 226 */     setName("SimulatorDisplayer");
	/*     */     
	/* 228 */     this.rs = new ReadScenario(startDate, paths);
	/* 229 */     if (this.tariff != null) {
	/* 230 */       this.currentTariffPeriod = this.tariff.getPeriod(startDate);
	/* 231 */       this.currentTariffPrice = this.tariff.getEnergyPrice(this.currentTariffPeriod);
	/*     */     }
	/*     */     
//	/*     */     try
//	/*     */     {
	/* 236 */       int minutesToStartCharts = cycleTimeInMillis * moments;
	/* 237 */       Calendar chartCalendar = Calendar.getInstance();
	/* 238 */       chartCalendar.setTime(startDate);
	/* 239 */       chartCalendar.add(14, -minutesToStartCharts);
	/*     */       

	/*     */   }

	/*     */   protected void initSimulator(Date startDate, int timeUnit, int timeUnitAmount, PATHS paths, boolean intelligence)
	/*     */     throws GridlabException
	/*     */   {
	/* 523 */     Calendar calendar = Calendar.getInstance();
	/* 524 */     calendar.setTime(startDate);
	/*     */     try
	/*     */     {
	/* 527 */       this.gldes = new GridLabDiscreteEventSimulator(paths, startDate, timeUnit, timeUnitAmount, this.nes, intelligence, this.rs, this.gridlabActions, this.activeClients);
	/* 528 */       this.gldes.registerListener(this);
	/*     */     }
	/*     */     catch (SimulatorNotInitializedException ex) {
	/* 531 */       ex.printStackTrace();
	/*     */     }
	/*     */   }
	/*     */   private Date[] readSimulationTime(String XMLfile)
	/*     */   {
	/* 261 */     Date[] dates = new Date[2];
	/*     */     try {
	/* 263 */       org.w3c.dom.Document doc = XMLUtilities.loadXMLDoc(XMLfile);
	/* 264 */       Node clock = doc.getElementsByTagName("clock").item(0);
	/* 265 */       if (clock.getNodeType() == 1)
	/*     */       {
	/* 267 */         Element eclock = (Element)clock;
	/* 268 */         String starttime = eclock.getElementsByTagName("starttime").item(0).getTextContent();
	/* 269 */         String stoptime = eclock.getElementsByTagName("stoptime").item(0).getTextContent();
	/*     */         
	/* 271 */         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/* 272 */         dates[0] = sdf.parse(starttime);
	/* 273 */         dates[1] = sdf.parse(stoptime);
	/*     */       }
	/*     */     }
	/*     */     catch (ParserConfigurationException e) {
	/* 277 */       e.printStackTrace();
	/*     */     }
	/*     */     catch (SAXException e) {
	/* 280 */       e.printStackTrace();
	/*     */     }
	/*     */     catch (IOException e) {
	/* 283 */       e.printStackTrace();
	/*     */     }
	/*     */     catch (ParseException e) {
	/* 286 */       e.printStackTrace();
	/*     */     }
	/*     */     
	/* 289 */     return dates;
	/*     */   }
	
  public void processStatus(Hashtable<SensorIDS, Float> sensorValues, Date timestamp) {}
/*     */   public void overvoltageEvent(Date timestamp)
/*     */   {
/* 743 */     //System.err.println("overvoltage " + timestamp);
/* 744 */     //this.simulationChart.drawAlternatingMarker(this.simulationChart.getDataset().getEndXValue(0, this.simulationChart.getDataset().getItemCount(0) - 1), "overvoltage", Color.red);
/*     */   }
///*     */for (NetworkListener currentListener : getListeners()) {
///* 189 */       currentListener.processCycleStatus(ns, this.nes, timestamp);
///* 190 */       if (ns.thereIsOvervoltage()) {
///* 191 */         currentListener.overvoltageEvent(ns.getOvervoltageTimestamp());
///*     */       }
///*     */     }
/**
 * @formatter:on
 */

	public JsonArray getAppliedOrdersSince(long timestamp)
	{
		return new JsonArray(this.gldes.getSimulator()
				.getAppliedOrdersSince(new Date(timestamp)));
	}

	public double getSystemConsumption()
	{
		return this.rs.getConsumption(this.nes.getElements(), getTime());
	}

	public Hashtable<SensorIDS, Float> getSubstationSensors()
	{
		return this.gridlabActions.getSubstationSensors(getTime());
	}

	public float getLossesSensor()
	{
		return this.gridlabActions.getLossesSensor(getTime());
	}

	public Hashtable<SensorIDS, Float> getDeviceSensors(String deviceName,
			Long timestamp)
	{
		return this.gridlabActions.getDeviceSensors(deviceName,
				timestamp == null ? getTime() : new Date(timestamp));
	}

	public double getCurrentSun(Long timestamp)
	{
		return this.rs.getSunPercentage(
				timestamp == null ? getTime() : new Date(timestamp));
	}

	public double getCurrentWind(Long timestamp)
	{
		return this.rs.getWindPercentage(
				timestamp == null ? getTime() : new Date(timestamp));
	}

	public double getCurrentEnergy(String batteryName, Long timestamp)
	{
		String energy = this.gridlabActions.readBatteryEnergy(batteryName,
				timestamp == null ? getTime() : new Date(timestamp));
		if (energy == null)
		{
			return 0.0D;
		}
		return Double.parseDouble(energy);
	}

	public boolean isServerActive()
	{
		return this.gldes.isSimulationEnded();
	}

	public int getPendingOrdersSize()
	{
		return this.gldes.getSimulator().getPendingOrders().size();
	}

	public ElementEMS getElementByName(String found) {
		return this.nes.getElementByName(found);
	}

}
