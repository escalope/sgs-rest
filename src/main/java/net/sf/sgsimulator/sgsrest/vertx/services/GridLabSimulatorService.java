package net.sf.sgsimulator.sgsrest.vertx.services;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.github.aesteve.vertx.nubes.services.Service;
import com.github.aesteve.vertx.nubes.utils.functional.TriConsumer;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import mired.ucm.dynamicGLM.XMLUtilities;
import mired.ucm.grid.ElementEMS;
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
	private double bill;
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
		Calendar calendar = Calendar.getInstance();
		calendar.set(
				sdjson.getInteger(0), sdjson.getInteger(1),
				sdjson.getInteger(2), sdjson.getInteger(3),
				sdjson.getInteger(4), sdjson.getInteger(5));

		String configurationFile = config.getString("configurationFile");
		String gridFile = config.getString("gridFile");
		// definition
		String scenarioFile = config.getString("scenarioFile");
		String gridLabFolder = config.getString("gridLabFolder");
		PATHS paths = new PATHS(configurationFile, gridFile, scenarioFile,
				gridLabFolder);

		Date[] dates = readSimulationTime(paths.getNetXmlSrcFile());
		long diffInMillies = dates[1].getTime() - dates[0].getTime();
		int hoursOfSimulation = (int) TimeUnit.HOURS.convert(diffInMillies,
				TimeUnit.MILLISECONDS);
		init(title, dates[0], cycleTimeInMillis, paths, intelligence,
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
		} catch (GridlabException | SimulatorNotInitializedException e)
		{
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

	}

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
	/* 222 */     this.bill = 0.0D;
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
	//XXX DONE in start() method
//=======================
//	/* 241 */       initSimulator(startDate, 14, cycleTimeInMillis, paths, intelligence);
//=======================
//	/* 242 */       initSimulationChart(minRange, maxRange, chartCalendar.getTime());
//	/* 243 */       initWeatherChart(chartCalendar.getTime());
//	/* 244 */       initFramePowerGridVisualizer(paths);
//	/* 245 */       initCtsDisplayer();
//	/* 246 */       initEventsDisplayer();
//	/*     */     } catch (GridlabException e) {
//	/* 248 */       e.printStackTrace();
//	/*     */     }
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

}
