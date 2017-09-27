package mines.ea.fitness.sim.cont;

import mines.util.*;
import mines.sim.*;
import java.util.*;

/**
 * Simulator class for a complex road network that can initialise a simulation from a stored state.
 * Intended to be extended as a fitness function.
 */
public abstract class SimFitnessFunction4 {

	private static class LightChange implements Comparable<LightChange> {

		int road;
		double time;

		public LightChange(int road, double time) {
			this.road = road;
			this.time = time;
		}

		public int compareTo(LightChange other) {
			return Double.compare(this.time,other.time);
		}
	}

	private static class Tuple implements Comparable<Tuple> {

		int t;
		double p;
		int l;

		public Tuple(int t, double p, int l) {
			this.t = t;
			this.p = p;
			this.l = l;
		}

		public int compareTo(Tuple other) {
			int pd = Double.compare(other.p,this.p);
			if (pd == 0) {
				int ld = Integer.compare(this.l,other.l);
				return (ld == 0 ? this.t - other.t : ld);
			}
			else {
				return pd;
			}
		}
	}

	private static final double EPSILON = 1e-6;	//small value.

	/*
	 * Simulation parameters.
	 */
	private int numTrucks;					//number of trucks.
	private int numShovels;					//number of shovels.
	private int numCrusherLocs;				//number of crusher locations.
	private int[] numCrushers;				//number of crushers at each location.
	private int numRoads;					//number of roads.
	private double[] emptyTimesMean;		//average emptying times for crushers at each location.
	private double[] emptyTimesSD;			//standard deviations of emptying times for crushers at each location.
	private double[] fillTimesMean;			//average filling times for each shovel.
	private double[] fillTimesSD;			//standard deviations of filling times for each shovel.
	private double[][] roadTravelTimesMean;	//average travelling times on each road in both directions.
	private double[][] roadTravelTimesSD;	//standard deviations of travelling time on each road in both directions.
	private double fullSlowdown;			//travel time increase for travelling full.
	private boolean[] isOneWay;				//whether each road is one-lane.

	/*
	 * Route specifications.
	 */
	private int numRoutes;				//number of routes.
	private int[][] routeRoads;			//list of roads comprising each route.
	private int[][] routeDirections;	//list of directions travelled on each road in each route.
	private int[] routeLengths;			//number of roads in each route.
	private int[] routeShovels;			//the shovel at the end of each route.
	private int[] routeCrushers;		//the crusher at the start of each route.

	/*
	 * Additional derived parameters.
	 */
	private int numOneWay;					//number of one-lane roads.
	private int[] lightIndexes;				//indexes of light associated with one-lane roads.
	private IntList reverseLightIndexes;	//indexes of roads associated with lights.
	private int[] defaultRoute;				//default route out of each crusher.
	private int[] initialCrushers;			//initial location of each truck.
	private double totalCrushingRate;		//net rate of all crushers.

	private TimeDistribution tgen;	//the distribution used for generating all stochastic values.

	/*
	 * Current simulation variables.
	 */
	private double currTime;								//current time in current simulation.
	private PriorityQueue<Transition> eventQueue;			//upcoming non-instant transitions in current simulation.
	private ShortPriorityQueue<Transition> instantQueue;	//upcoming instant transitions in current simulation.
	private TruckLocation[] truckLocs;						//current locations of each truck in current simulation.
	private int[] assignedShovel;							//current assigned shovel for each truck in current simulation.
	private int[] assignedCrusher;							//current assigned crusher for each truck in current simulation.
	private int[] assignedRoute;							//current assigned route for each truck in current simulation.
	private int[] routePoint;								//current route index for each truck in current simulation.
	private IntQueue[] crusherQueues;						//queues for each crusher location in current simulation.
	private IntQueue[] shovelQueues;						//queues for each shovel in current simulation.
	private int[] numEmptying;								//number of crushers active at each crusher location in current simulation.
	private boolean[] shovelInUse;							//whether each shovel is in use in current simulation.
	private TrafficLight[] lights;							//state of each traffic light in current simulation.
	private IntQueue[][] lightQueues;						//queues for each traffic light in current simulation.
	private IntQueue[][] roadQueues;						//order of trucks on each road in current simulation.
	private ShortPriorityQueue<LightChange> lightSchedule;	//upcoming changes from green lights in current simulation.
	private boolean[] greedyMode;							//whether each light is set to greedy rules in current simulation.
	private double[][] roadAvailable;						//the minimum possible arrival time for each road end in current simulation.
	private int[][] roadPriority;							//priority values used for transitions to preserve order.
	private int numEmpties;									//number of empties completed in current simulation.
	private int numUnused;									//number of trucks in unused state in current simulation.

	/*
	 * Stored state variables.
	 */
	private double simTime;				//current time in stored state.
	private TruckLocation[] simLocs;	//current truck locations in stored state.
	private int[] simAShovel;			//current assigned shovel for each truck in stored state.
	private int[] simACrusher;			//current assigned crusher for each truck in stored state.
	private int[] simARoute;			//current assigned route for each truck in stored state.
	private int[] simRoutePoint;		//current route index for each truck in stored state.
	private double[] simProgress;		//current fractional completion of current task for each truck in stored state.
	private TrafficLight[] simLights;	//current state of each traffic light in stored state.
	private double[] simLightSchedule;	//upcoming times of light changes in stored state.
	private boolean[] simGreedyMode;	//whether each light is currently using greedy lights in stored state.
	private int[] simLastTransition;	//time of last transition supplied to simulator for each truck.
	private int simTransitionCount;		//number of transitions supplied to simulator.

	/*
	 * Variables related to the stored state allowing for quick reinitialisation of simulation.
	 */
	private ArrayList<Transition> eventQueueStored;		//initial non-instant transitions from stored state.
	private ArrayList<Transition> instantQueueStored;	//initial instant transitions from stored state.
	private IntQueue[] crusherQueuesStored;				//queues for each crusher location from stored state.
	private IntQueue[] shovelQueuesStored;				//queues for each shovel from stored state.
	private int[] numEmptyingStored;					//number of active crushers from stored state.
	private boolean[] shovelInUseStored;				//whether each shovel is active in stored state.
	private IntQueue[][] lightQueuesStored;				//queues for each traffic light from stored state.
	private IntQueue[][] roadQueuesStored;				//order of trucks on each road from stored state.
	private int[][] roadPriorityStored;					//initial priority values from stored state.
	private double[][] roadProgress;					//used in reinitialisation.

	/*
	 * Statistics for current simulation.
	 */
	private double[] lastServiceStart;				//time of last service start for each truck in current simulation.
	private double[] serviceWaitingTime;			//total time spent waiting in service queues for each truck in current simulation.
	private double[] lastWaitStart;					//starting time of last wait for each truck in current simulation.
	private double[] serviceAvailableTime;
	//for each truck in current simulation, for its last service the time that machine became available prior to the service.
	private double[] lastFillEnd;					//last service completion for each shovel in current simulation
	private double[] lastEmptyEnd;					//last service completion for each crusher location in current simulation
	private double[] roadWaitingTime;				//total time spent waiting at passing points for each truck in current simulation.
	private double[] shovelWaitingTime;				//total idle time of shovels in current simulation.
	private int[] dispatched;						//number of dispatches for each truck in current simulation.
	private int successfulFills;					//number of completed services at shovels in current simulation.
	private double shovelIdleOre;					//maximum truckloads wasted by shovel idle times in current simulation.
	private int successfulEmpties;					//number of completed services at crushers in current simulation.
	private int[] successfulFillsFromCrusher;		//number of completed services at shovels per dispatching location in current simulation.
	private double truckIdle;						//maximum truckloads wasted by truck idle times in current simulation.
	private double[] simDispatchTime;				//time of last dispatch for each truck in stored state.
	private double[] dispatchTime;					//time of last dispatch for each truck in current simulation.
	private DoubleList completeCycles;				//cycle times of completed cycles in current simulation.
	private double[] lastCrusherChange;				//last time number of active crushers changed at each location in current simulation.
	private double crusherIdleOre;					//maximum truckloads wasted by crusher idle times in current simulation.
	private double[] individualTruckWaitingTime;	//total waiting times of each truck in current simulation.

	private boolean isReady;	//whether the stored state is unchanged since the last simulation.

	/**
	 * Simulator constructor.
	 * Sets the initial stored state as the default with all trucks at crushers.
	 *
	 * @param numTrucks				the number of trucks.
	 * @param numShovels			the number of shovels.
	 * @param numCrusherLocs		the number of crusher locations.
	 * @param numCrushers			an array of the number of crushers at each location.
	 * @param numRoads				the number of roads.
	 * @param emptyTimesMean		an array of average emptying times for each crusher.
	 * @param emptyTimesSD			an array of standard deviations of emptying times for each crusher.
	 * @param fillTimesMean			an array of average filling times for each shovel.
	 * @param fillTimesSD			an array of standard deviations of filling times for each shovel.
	 * @param roadTravelTimesMean	a 2D array of average travelling times on each road in both directions.
	 * @param roadTravelTimesSD		a 2D array of standard deviations of travelling time on each road in both directions.
	 * @param fullSlowdown			the travel time increase for travelling full.
	 * @param isOneWay				an array specifying whether each road is one-lane.
	 * @param numRoutes				the number of routes.
	 * @param routeRoads			a 2D array listing the roads comprising each route.
	 * @param routeDirections		a 2D array listing the directions travelled on each road in each route.
	 * @param routeLengths			an array of the number of roads in each route.
	 * @param routeShovels			an array of the shovel at the end of each route.
	 * @param routeCrushers			an array of the crusher at the start of each route.
	 * @param tgen					a TimeDistrubtion specifying the distribution used for generating all stochastic values.
	 */
	public SimFitnessFunction4(int numTrucks, int numShovels, int numCrusherLocs, int[] numCrushers, int numRoads, 
		double[] emptyTimesMean, double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[][] roadTravelTimesMean, 
		double[][] roadTravelTimesSD, double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections,
		int[] routeLengths, int[] routeShovels, int[] routeCrushers, TimeDistribution tgen) {
		this.numTrucks = numTrucks;
		this.numShovels = numShovels;
		this.numCrusherLocs = numCrusherLocs;
		this.numCrushers = numCrushers;
		this.numRoads = numRoads;
		this.emptyTimesMean = emptyTimesMean;
		this.emptyTimesSD = emptyTimesSD;
		this.fillTimesMean = fillTimesMean;
		this.fillTimesSD = fillTimesSD;
		this.roadTravelTimesMean = roadTravelTimesMean;
		this.roadTravelTimesSD = roadTravelTimesSD;
		this.fullSlowdown = fullSlowdown;
		this.isOneWay = isOneWay;

		this.numRoutes = numRoutes;
		this.routeRoads = routeRoads;
		this.routeLengths = routeLengths;
		this.routeShovels = routeShovels;
		this.routeCrushers = routeCrushers;
		this.routeDirections = routeDirections;

		numOneWay = 0;
		lightIndexes = new int[numRoads];
		reverseLightIndexes = new IntList();
		for (int i=0; i<numRoads; i++) {
			if (isOneWay[i]) {
				lightIndexes[i] = numOneWay;
				reverseLightIndexes.add(i);
				numOneWay++;
			}
			else {
				lightIndexes[i] = -1;
			}
		}
		defaultRoute = new int[numCrusherLocs];
		for (int i=0; i<numCrusherLocs; i++) {
			defaultRoute[i] = -1;
		}
		for (int i=0; i<numRoutes; i++) {
			if (defaultRoute[routeCrushers[i]] < 0) {
				defaultRoute[routeCrushers[i]] = i;
			}
		}
		for (int i=0; i<numCrusherLocs; i++) {
			if (defaultRoute[i] < 0) {
				throw new IllegalArgumentException(String.format("No routes out of crusher %d",i));
			}
		}
		initialCrushers = null;
		totalCrushingRate = 0;
		for (int i=0; i<numCrusherLocs; i++) {
			totalCrushingRate += numCrushers[i] / emptyTimesMean[i];
		}

		this.tgen = tgen;

		eventQueue = new PriorityQueue<>();
		instantQueue = new ShortPriorityQueue<>();
		truckLocs = new TruckLocation[numTrucks];
		assignedShovel = new int[numTrucks];
		assignedCrusher = new int[numTrucks];
		assignedRoute = new int[numTrucks];
		routePoint = new int[numTrucks];
		crusherQueues = new IntQueue[numCrusherLocs];
		for (int i=0; i<numCrusherLocs; i++) {
			crusherQueues[i] = new IntQueue();
		}
		shovelQueues = new IntQueue[numShovels];
		for (int i=0; i<numShovels; i++) {
			shovelQueues[i] = new IntQueue();
		}
		numEmptying = new int[numCrusherLocs];
		shovelInUse = new boolean[numShovels];
		lights = new TrafficLight[numOneWay];
		lightQueues = new IntQueue[numOneWay][2];
		for (int i=0; i<numOneWay; i++) {
			for (int j=0; j<2; j++) {
				lightQueues[i][j] = new IntQueue();
			}
		}
		roadQueues = new IntQueue[numRoads][2];
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				roadQueues[i][j] = new IntQueue();
			}
		}
		lightSchedule = new ShortPriorityQueue<>();
		greedyMode = new boolean[numOneWay];
		roadAvailable = new double[numRoads][2];
		roadPriority = new int[numRoads][2];

		simLocs = new TruckLocation[numTrucks];
		simAShovel = new int[numTrucks];
		simACrusher = new int[numTrucks];
		simARoute = new int[numTrucks];
		simRoutePoint = new int[numTrucks];
		simProgress = new double[numTrucks];
		simLights = new TrafficLight[numOneWay];
		simLightSchedule = new double[numOneWay];
		simGreedyMode = new boolean[numOneWay];
		simLastTransition = new int[numTrucks];

		eventQueueStored = new ArrayList<>();
		instantQueueStored = new ArrayList<>();
		crusherQueuesStored = new IntQueue[numCrusherLocs];
		for (int i=0; i<numCrusherLocs; i++) {
			crusherQueuesStored[i] = new IntQueue();
		}
		shovelQueuesStored = new IntQueue[numShovels];
		for (int i=0; i<numShovels; i++) {
			shovelQueuesStored[i] = new IntQueue();
		}
		numEmptyingStored = new int[numCrusherLocs];
		shovelInUseStored = new boolean[numShovels];
		lightQueuesStored = new IntQueue[numOneWay][2];
		for (int i=0; i<numOneWay; i++) {
			for (int j=0; j<2; j++) {
				lightQueuesStored[i][j] = new IntQueue();
			}
		}
		roadQueuesStored = new IntQueue[numRoads][2];
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				roadQueuesStored[i][j] = new IntQueue();
			}
		}
		roadPriorityStored = new int[numRoads][2];
		roadProgress = new double[numRoads][2];

		lastServiceStart = new double[numTrucks];
		serviceWaitingTime = new double[numTrucks];
		lastWaitStart = new double[numTrucks];
		serviceAvailableTime = new double[numTrucks];
		lastFillEnd = new double[numShovels];
		lastEmptyEnd = new double[numCrusherLocs];
		roadWaitingTime = new double[numTrucks];
		shovelWaitingTime = new double[numShovels];
		dispatched = new int[numTrucks];
		successfulFillsFromCrusher = new int[numCrusherLocs];
		simDispatchTime = new double[numTrucks];
		dispatchTime = new double[numTrucks];
		completeCycles = new DoubleList();
		lastCrusherChange = new double[numCrusherLocs];
		individualTruckWaitingTime = new double[numTrucks];

		isReady = false;
	}

	/**
	 * Set the initial locations of trucks.
	 *
	 * @param	initialCrushers	an array of crusher indexes,
	 *							or null for the default option of trucks spread evenly among crushers
	 */
	public void setInitialCrushers(int[] initialCrushers) {
		if (initialCrushers == null) {
			this.initialCrushers = null;
		}
		else if (initialCrushers.length < numTrucks) {
			throw new IllegalArgumentException(String.format("Initial locations need to be specified for all trucks: %s length %d",
				Arrays.toString(initialCrushers),initialCrushers.length));
		}
		else {
			this.initialCrushers = Arrays.copyOf(initialCrushers,numTrucks);
		}
		// this.initialCrushers = initialCrushers;
	}

	/**
	 * Get the next route for the given truck.
	 * 
	 * @param	tid	the index of the truck requiring routing.
	 * @return	a positive route index,
	 *			or -2 to take the truck out of use,
	 *			otherwise any negative value to terminate the current simulation.
	 */
	protected abstract int nextRoute(int tid);

	/**
	 * Get the amount of time for a light to remain green.
	 *
	 * @param	road	the one-lane road changing light state.
	 * @param	change	the new light state -
	 *					if green then it has just been yellow in opposite direction,
	 *					if yellow then it has been green, 
	 *					and can remain green if a positive value is returned.
	 * @return	a time value.
	 */
	protected abstract double getNextLight(int road, TrafficLight change);

	/**
	 * Resets the stored state to the start of a shift.
	 */
	public void reset() {
		simTime = 0;
		for (int i=0; i<numTrucks; i++) {
			simLocs[i] = TruckLocation.WAITING;
			simACrusher[i] = (initialCrushers == null ? i % numCrusherLocs : initialCrushers[i]);
			for (int j=0; j<numRoutes; j++) {
				if (routeCrushers[j] == simACrusher[i]) {
					simARoute[i] = j;
					simAShovel[i] = routeShovels[j];
					break;
				}
				if (j == numRoutes - 1) {
					throw new IllegalStateException("No routes out of crusher");
				}
			}
			simRoutePoint[i] = 0;
			simProgress[i] = 0;
			simDispatchTime[i] = 0;
		}
		for (int i=0; i<numOneWay; i++) {
			simLights[i] = TrafficLight.GR;
			simLightSchedule[i] = 0;
			simGreedyMode[i] = true;
		}
		simTransitionCount = 0;
		isReady = false;
	}

	/**
	 * Update the stored state for the trucks.
	 * Should be used after each transition.
	 * 
	 * @param	change	a StateChange specifying the transition that occurred.
	 */
	public void event(StateChange change) {
		simTime = change.getTime();
		int truck = change.getTruck();
		simLocs[truck] = change.getTarget();
		simARoute[truck] = change.getInformation(0);
		simRoutePoint[truck] = change.getInformation(1);
		simAShovel[truck] = routeShovels[simARoute[truck]];
		simACrusher[truck] = routeCrushers[simARoute[truck]];
		for (int i=0; i<numTrucks; i++) {
			simProgress[i] = change.getProgress(i);
		}
		simLastTransition[truck] = simTransitionCount;
		simTransitionCount++;

		if (simLocs[truck] == TruckLocation.WAITING) {
			simDispatchTime[truck] = simTime;
		}

		isReady = false;
	}

	/**
	 * Update the stored state for the traffic lights.
	 * Should be used after each change in traffic lights.
	 * 
	 * @param	light		the road which changed light state.
	 * @param	change		the new TrafficLight value.
	 * @param	time		the time of the change.
	 * @param	schedule	the time of the next change if change is green.
	 * @param	progress	the progress of each truck,
	 *						null if unchanged from previous updates.
	 */
	public void lightEvent(int road, TrafficLight change, double time, double schedule, double[] progress) {
		int light = lightIndexes[road];
		simLights[light] = change;
		simTime = time;
		simLightSchedule[light] = schedule;
		simGreedyMode[light] = (schedule == time);
		if (progress != null) {
			for (int i=0; i<numTrucks; i++) {
				simProgress[i] = progress[i];
			}
		}
		isReady = false;
	}

	/**
	 * Readies the simulator for initialisation based on the stored state.
	 * Should be run once if the stored state has changed.
	 *
	 * @see isReady()
	 */
	protected void ready() {
		ArrayList<Tuple> progressList = new ArrayList<>(numTrucks);
		for (int i=0; i<numTrucks; i++) {
			progressList.add(new Tuple(i,simProgress[i],simLastTransition[i]));
		}
		Collections.sort(progressList);
		instantQueueStored.clear();
		eventQueueStored.clear();
		for (int i=0; i<numCrusherLocs; i++) {
			crusherQueuesStored[i].clear();
			numEmptyingStored[i] = 0;
		}
		for (int i=0; i<numShovels; i++) {
			shovelQueuesStored[i].clear();
			shovelInUseStored[i] = false;
		}
		for (int i=0; i<numOneWay; i++) {
			for (int j=0; j<2; j++) {
				lightQueuesStored[i][j].clear();
			}
		}
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				roadPriorityStored[i][j] = Integer.MIN_VALUE;
				roadQueuesStored[i][j].clear();
			}
		}
		for (Tuple p : progressList) {
			int tid = p.t;
			int point = simRoutePoint[tid];
			int route = simARoute[tid];
			int road;
			int dir;
			if (point < 0) {
				road = routeRoads[route][0];
				dir = routeDirections[route][0];
			}
			else if (point < routeLengths[route]) {
				road = routeRoads[route][point];
				dir = routeDirections[route][point];
			}
			else {
				road = routeRoads[route][routeLengths[route] - 1];
				dir = routeDirections[route][routeLengths[route] - 1];
			}
			int sid = simAShovel[tid];
			int cid = simACrusher[tid];
			switch (simLocs[tid]) {
				case WAITING: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.WAITING,TruckLocation.WAITING,getPriority(tid,
						TruckLocation.WAITING)));
					break;
				}
				case TRAVEL_TO_SHOVEL: {
					TruckLocation nextLoc;
					if (point == routeLengths[route] - 1) {
						nextLoc = TruckLocation.APPROACHING_SHOVEL;
					}
					else if (isOneWay[routeRoads[route][point + 1]]) {
						nextLoc = TruckLocation.APPROACHING_TL_CS;
					}
					else {
						nextLoc = TruckLocation.TRAVEL_TO_SHOVEL;
					}
					eventQueueStored.add(new Transition(tid,0,TruckLocation.TRAVEL_TO_SHOVEL,nextLoc,roadPriorityStored[road][dir]));
					roadPriorityStored[road][dir]++;
					roadQueuesStored[road][dir].add(tid);
					break;
				}
				case APPROACHING_TL_CS: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.APPROACHING_TL_CS,TruckLocation.APPROACHING_TL_CS,
						getPriority(tid,TruckLocation.APPROACHING_TL_CS)));
					if (point > 0) {
						roadQueuesStored[routeRoads[route][point - 1]][/*dir*/routeDirections[route][point - 1]].addFront(tid);
					}
					break;
				}
				case STOPPED_AT_TL_CS: {
					lightQueuesStored[lightIndexes[road]][dir].add(tid);
					break;
				}
				case APPROACHING_SHOVEL: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.APPROACHING_SHOVEL,TruckLocation.APPROACHING_SHOVEL,
						getPriority(tid,TruckLocation.APPROACHING_SHOVEL)));
					roadQueuesStored[road][dir].addFront(tid);
					break;
				}
				case WAITING_AT_SHOVEL: {
					shovelQueuesStored[sid].add(tid);
					break;
				}
				case FILLING: {
					shovelInUseStored[sid] = true;
					eventQueueStored.add(new Transition(tid,0,TruckLocation.FILLING,TruckLocation.LEAVING_SHOVEL,getPriority(tid,
						TruckLocation.LEAVING_SHOVEL)));
					break;
				}
				case LEAVING_SHOVEL: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.LEAVING_SHOVEL,TruckLocation.LEAVING_SHOVEL,
						getPriority(tid,TruckLocation.LEAVING_SHOVEL)));
					break;
				}
				case TRAVEL_TO_CRUSHER: {
					TruckLocation nextLoc;
					if (point == 0) {
						nextLoc = TruckLocation.APPROACHING_CRUSHER;
					}
					else if (isOneWay[routeRoads[route][point - 1]]) {
						nextLoc = TruckLocation.APPROACHING_TL_SS;
					}
					else {
						nextLoc = TruckLocation.TRAVEL_TO_CRUSHER;
					}
					eventQueueStored.add(new Transition(tid,0,TruckLocation.TRAVEL_TO_CRUSHER,nextLoc,roadPriorityStored[road][1 - dir]));
					roadPriorityStored[road][1 - dir]++;
					roadQueuesStored[road][1 - dir].add(tid);
					break;
				}
				case APPROACHING_TL_SS: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.APPROACHING_TL_SS,TruckLocation.APPROACHING_TL_SS,
						getPriority(tid,TruckLocation.APPROACHING_TL_SS)));
					if (point < routeLengths[route] - 1) {
						roadQueuesStored[routeRoads[route][point + 1]][1 - /*dir*/routeDirections[route][point + 1]].addFront(tid);
					}
					break;
				}
				case STOPPED_AT_TL_SS: {
					lightQueuesStored[lightIndexes[road]][1 - dir].add(tid);
					break;
				}
				case APPROACHING_CRUSHER: {
					instantQueueStored.add(new Transition(tid,simTime,TruckLocation.APPROACHING_CRUSHER,TruckLocation.APPROACHING_CRUSHER,
						getPriority(tid,TruckLocation.APPROACHING_CRUSHER)));
					roadQueuesStored[road][1 - dir].addFront(tid);
					break;
				}
				case WAITING_AT_CRUSHER: {
					crusherQueuesStored[cid].add(tid);
					break;
				}
				case EMPTYING: {
					numEmptyingStored[cid]++;
					eventQueueStored.add(new Transition(tid,0,TruckLocation.EMPTYING,TruckLocation.WAITING,getPriority(tid,
						TruckLocation.WAITING)));
					break;
				}
			}
		}
		for (int i=0; i<numShovels; i++) {
			if (!shovelInUseStored[i] && !shovelQueuesStored[i].isEmpty()) {
				int head = shovelQueuesStored[i].poll();
				instantQueueStored.add(new Transition(head,simTime,TruckLocation.WAITING_AT_SHOVEL,TruckLocation.FILLING,getPriority(head,
					TruckLocation.FILLING)));
				shovelInUseStored[i] = true;
			}
		}
		for (int i=0; i<numCrusherLocs; i++) {
			while (numEmptyingStored[i] < numCrushers[i] && !crusherQueuesStored[i].isEmpty()) {
				int head = crusherQueuesStored[i].poll();
				instantQueueStored.add(new Transition(head,simTime,TruckLocation.WAITING_AT_CRUSHER,TruckLocation.EMPTYING,getPriority(
					head,TruckLocation.EMPTYING)));
				numEmptyingStored[i]++;
			}
		}
		Collections.sort(eventQueueStored);
		isReady = true;
	}

	/**
	 * Initialises the simulator.
	 * Should be run before every simulation, and only if readied.
	 *
	 * @see ready()
	 * @see isReady()
	 */
	protected void reReady() {
		currTime = simTime;
		reTruckInfo();
		reMachineInfo();
		reRoadInfo();
		reEventInfo();
		numEmpties = 0;
		resetStatistics();
	}

	/**
	 * Initialises truck information.
	 */
	private void reTruckInfo() {
		System.arraycopy(simLocs,0,truckLocs,0,numTrucks);
		System.arraycopy(simAShovel,0,assignedShovel,0,numTrucks);
		System.arraycopy(simACrusher,0,assignedCrusher,0,numTrucks);
		System.arraycopy(simARoute,0,assignedRoute,0,numTrucks);
		System.arraycopy(simRoutePoint,0,routePoint,0,numTrucks);
		numUnused = 0;
	}

	/**
	 * Initialises crusher and shovel information.
	 */
	private void reMachineInfo() {
		for (int i=0; i<numCrusherLocs; i++) {
			crusherQueues[i].clear();
			crusherQueues[i].addAll(crusherQueuesStored[i]);
		}
		System.arraycopy(numEmptyingStored,0,numEmptying,0,numCrusherLocs);
		for (int i=0; i<numShovels; i++) {
			shovelQueues[i].clear();
			shovelQueues[i].addAll(shovelQueuesStored[i]);
		}
		System.arraycopy(shovelInUseStored,0,shovelInUse,0,numShovels);
	}

	/**
	 * Initialises road information.
	 */
	private void reRoadInfo() {
		lightSchedule.clear();
		System.arraycopy(simLights,0,lights,0,numOneWay);
		System.arraycopy(simGreedyMode,0,greedyMode,0,numOneWay);
		for (int i=0; i<numOneWay; i++) {
			for (int j=0; j<2; j++) {
				lightQueues[i][j].clear();
				lightQueues[i][j].addAll(lightQueuesStored[i][j]);
			}
			if (lights[i] == TrafficLight.GR || lights[i] == TrafficLight.RG) {
				if (!greedyMode[i]) {
					lightSchedule.add(new LightChange(reverseLightIndexes.get(i),simLightSchedule[i]));
				}
			}
		}
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				roadQueues[i][j].clear();
				roadQueues[i][j].addAll(roadQueuesStored[i][j]);
			}
			Arrays.fill(roadAvailable[i],currTime);
			System.arraycopy(roadPriorityStored[i],0,roadPriority[i],0,2);
			Arrays.fill(roadProgress[i],1.0);
		}
	}

	/**
	 * Initialises event information.
	 */
	private void reEventInfo() {
		instantQueue.clear();
		instantQueue.addAll(instantQueueStored);
		eventQueue.clear();
		for (Transition t : eventQueueStored) {
			int tid = t.getIndex();
			TruckLocation source = t.getSource();
			TruckLocation target = t.getTarget();
			int priority = t.getPriority();
			int route = assignedRoute[tid];
			int point = routePoint[tid];
			int road = -1;
			int dir = -1;
			if (point >= 0 && point < routeLengths[route]) {
				road = routeRoads[route][point];
				dir = routeDirections[route][point];
			}
			int sid = assignedShovel[tid];
			int cid = assignedCrusher[tid];
			double progress = simProgress[tid];
			if (progress < 0 || progress > 1) {
				throw new IllegalStateException("Illegal progress value found");
			}
			switch (source) {
				case TRAVEL_TO_SHOVEL: {
					if (roadProgress[road][dir] - progress > EPSILON) {
						double travelTime = tgen.nextTime(roadTravelTimesMean[road][dir],roadTravelTimesSD[road][dir]) * (1 - progress);
						roadAvailable[road][dir] = Math.max(roadAvailable[road][dir],currTime + travelTime);
					}
					roadProgress[road][dir] = progress;
					eventQueue.add(new Transition(tid,roadAvailable[road][dir],source,target,priority));
					routePoint[tid]++;
					break;
				}
				case FILLING: {
					double finish = currTime + tgen.nextTime(fillTimesMean[sid],fillTimesSD[sid]) * (1 - progress);
					eventQueue.add(new Transition(tid,finish,source,target,priority));
					break;
				}
				case TRAVEL_TO_CRUSHER: {
					if (roadProgress[road][1 - dir] - progress > EPSILON) {
						double travelTime = tgen.nextTime(roadTravelTimesMean[road][1 - dir],roadTravelTimesSD[road][1 - dir]) * 
							(1 - progress) * fullSlowdown;
						roadAvailable[road][1 - dir] = Math.max(roadAvailable[road][1 - dir],currTime + travelTime);
					}
					roadProgress[road][1 - dir] = progress;
					eventQueue.add(new Transition(tid,roadAvailable[road][1 - dir],source,target,priority));
					routePoint[tid]--;
					break;
				}
				case EMPTYING: {
					double emptyTime = tgen.nextTime(emptyTimesMean[cid],emptyTimesSD[cid]) * (1 - progress);
					double finish = currTime + emptyTime;
					eventQueue.add(new Transition(tid,finish,source,target,priority));
					break;
				}
				default: {
					throw new IllegalStateException(String.format("Stored non-instant event is invalid: %s",t));
				}
			}
		}
	}

	/**
	 * Initialises statistics.
	 */
	private void resetStatistics() {
		Arrays.fill(lastServiceStart,currTime);
		Arrays.fill(serviceWaitingTime,0);
		Arrays.fill(lastWaitStart,currTime);
		Arrays.fill(serviceAvailableTime,currTime);
		Arrays.fill(roadWaitingTime,0);
		Arrays.fill(dispatched,0);
		System.arraycopy(simDispatchTime,0,dispatchTime,0,numTrucks);
		Arrays.fill(lastEmptyEnd,currTime);
		Arrays.fill(lastCrusherChange,currTime);
		Arrays.fill(successfulFillsFromCrusher,0);
		Arrays.fill(lastFillEnd,currTime);
		Arrays.fill(shovelWaitingTime,0);
		Arrays.fill(individualTruckWaitingTime,0);
		successfulFills = 0;
		shovelIdleOre = 0;
		successfulEmpties = 0;
		truckIdle = 0;
		completeCycles.clear();
		crusherIdleOre = 0;
	}

	/**
	 * Runs a forward simulation.
	 * Will terminate if no trucks are in use, or a negative value other than -2 is assigned as the route.
	 * 
	 * @param	runtime	the termination time of the simulation.
	 */
	public void simulate(double runtime) {
		for (int i=0; i<numOneWay; i++) {
			checkLights(reverseLightIndexes.get(i));
		}
		while (numUnused < numTrucks) {
			Transition next = peekNextEvent();
			if (next == null || (!lightSchedule.isEmpty() && lightSchedule.peek().time <= next.getTime())) {
				if (lightSchedule.peek().time > runtime) {
					break;
				}
				updateLights();
				continue;
			}
			else if (next.getTime() > runtime) {
				break;
			}
			if (!singleEvent()) {
				break;
			}
		}
	}

	/**
	 * Move the simulation forward by one transition.
	 * 
	 * @return	false if a termination request is received, true otherwise.
	 * @throws	IllegalStateException	if a simulation error occurred,
	 *									e.g. illegal route index, 
	 *									approaching non-existent traffic lights,
	 *									illegal traffic light state,
	 *									illegal transition.
	 */
	private boolean singleEvent() {
		Transition next = getNextEvent();
		if (next.getTime() < currTime) {
			throw new IllegalStateException("Negative time step");
		}
		currTime = next.getTime();
		int tid = next.getIndex();
		TruckLocation tOrigin = next.getSource();
		TruckLocation tDest = next.getTarget();
		if (tOrigin == truckLocs[tid]) {
			switch (tDest) {
				case WAITING: {
					if (tOrigin == TruckLocation.EMPTYING) {
						updateCrusher(assignedCrusher[tid]);
						if (dispatched[tid] > 0) {
							successfulEmpties++;
						}
						completeCycles.add(currTime - dispatchTime[tid]);
					}
					if (!getRoute(tid,true)) {
						return false;
					}
					dispatched[tid]++;
					dispatchTime[tid] = currTime;
					break;
				}
				case TRAVEL_TO_SHOVEL: {
					int road = routeRoads[assignedRoute[tid]][routePoint[tid]];
					if (isOneWay[road]) {
						roadWaitingTime[tid] += currTime - lastWaitStart[tid];
						truckIdle += (currTime - lastWaitStart[tid]) * (totalCrushingRate / numTrucks);
						individualTruckWaitingTime[tid] += (currTime - lastWaitStart[tid]);
					}
					else {
						clearedRoad(tid,true);
					}
					eventQueue.add(preventCollisions(tid,true));
					routePoint[tid]++;
					break;
				}
				case APPROACHING_TL_CS: {
					clearedRoad(tid,true);
					arrivedAtLights(tid,true);
					lastWaitStart[tid] = currTime;
					break;
				}
				case STOPPED_AT_TL_CS: {
					stoppedAtLights(tid);
					break;
				}
				case APPROACHING_SHOVEL: {
					clearedRoad(tid,true);
					int sid = assignedShovel[tid];
					TruckLocation nextLoc;
					if (shovelInUse[sid]) {
						nextLoc = TruckLocation.WAITING_AT_SHOVEL;
						shovelQueues[sid].add(tid);
					}
					else {
						nextLoc = TruckLocation.FILLING;
						shovelInUse[sid] = true;
					}
					instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
					lastWaitStart[tid] = currTime;
					break;
				}
				case WAITING_AT_SHOVEL: {
					break;
				}
				case FILLING: {
					int sid = assignedShovel[tid];
					double fillTime = tgen.nextTime(fillTimesMean[sid],fillTimesSD[sid]);
					eventQueue.add(new Transition(tid,currTime + fillTime,tDest,TruckLocation.LEAVING_SHOVEL,getPriority(tid,
						TruckLocation.LEAVING_SHOVEL)));
					lastServiceStart[tid] = currTime;
					serviceWaitingTime[tid] += currTime - lastWaitStart[tid];
					truckIdle += (currTime - lastWaitStart[tid]) * (totalCrushingRate / numTrucks);
					individualTruckWaitingTime[tid] += (currTime - lastWaitStart[tid]);
					serviceAvailableTime[tid] = lastFillEnd[sid];
					shovelWaitingTime[sid] += currTime - lastFillEnd[sid];
					shovelIdleOre += (currTime - lastFillEnd[sid]) / fillTimesMean[sid];
					break;
				}
				case LEAVING_SHOVEL: {
					int sid = assignedShovel[tid];
					if (tOrigin == TruckLocation.FILLING) {
						if (shovelQueues[sid].isEmpty()) {
							shovelInUse[sid] = false;
						}
						else {
							int head = shovelQueues[sid].poll();
							instantQueue.add(new Transition(head,currTime,TruckLocation.WAITING_AT_SHOVEL,TruckLocation.FILLING,
								getPriority(head,TruckLocation.FILLING)));
						}
						lastFillEnd[sid] = currTime;
						if (dispatched[tid] > 0) {
							successfulFills++;
							successfulFillsFromCrusher[assignedCrusher[tid]]++;
						}
					}
					if (!getRoute(tid,false)) {
						return false;
					}
					break;
				}
				case TRAVEL_TO_CRUSHER: {
					int road = routeRoads[assignedRoute[tid]][routePoint[tid]];
					if (isOneWay[road]) {
						roadWaitingTime[tid] += currTime - lastWaitStart[tid];
						truckIdle += (currTime - lastWaitStart[tid]) * (totalCrushingRate / numTrucks);
						individualTruckWaitingTime[tid] += (currTime - lastWaitStart[tid]);
					}
					else {
						clearedRoad(tid,false);
					}
					eventQueue.add(preventCollisions(tid,false));
					routePoint[tid]--;
					break;
				}
				case APPROACHING_TL_SS: {
					clearedRoad(tid,false);
					arrivedAtLights(tid,false);
					lastWaitStart[tid] = currTime;
					break;
				}
				case STOPPED_AT_TL_SS: {
					stoppedAtLights(tid);
					break;
				}
				case APPROACHING_CRUSHER: {
					clearedRoad(tid,false);
					int cid = assignedCrusher[tid];
					TruckLocation nextLoc;
					if (numEmptying[cid] < numCrushers[cid]) {
						nextLoc = TruckLocation.EMPTYING;
						crusherIdleOre += (currTime - lastCrusherChange[cid]) * (numCrushers[cid] - numEmptying[cid]) / emptyTimesMean[cid];
						numEmptying[cid]++;
						lastCrusherChange[cid] = currTime;
					}
					else {
						nextLoc = TruckLocation.WAITING_AT_CRUSHER;
						crusherQueues[cid].add(tid);
					}
					instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
					lastWaitStart[tid] = currTime;
					break;
				}
				case WAITING_AT_CRUSHER: {
					break;
				}
				case EMPTYING: {
					int cid = assignedCrusher[tid];
					double emptyTime = tgen.nextTime(emptyTimesMean[cid],emptyTimesSD[cid]);
					eventQueue.add(new Transition(tid,currTime + emptyTime,tDest,TruckLocation.WAITING,getPriority(tid,
						TruckLocation.WAITING)));
					lastServiceStart[tid] = currTime;
					serviceWaitingTime[tid] += currTime - lastWaitStart[tid];
					truckIdle += (currTime - lastWaitStart[tid]) * (totalCrushingRate / numTrucks);
					individualTruckWaitingTime[tid] += (currTime - lastWaitStart[tid]);
					serviceAvailableTime[tid] = lastEmptyEnd[cid];
					break;
				}
				case UNUSED: {
					numUnused++;
					lastWaitStart[tid] = currTime;
					break;
				}
				default: {
					throw new IllegalStateException(String.format("Truck has entered illegal state: %s",tDest));
				}
			}
			truckLocs[tid] = tDest;
		}
		else {
			throw new IllegalStateException(String.format("Transition occurred from %s to %s when truck is in %s",tOrigin,tDest,
				truckLocs[tid]));
		}
		return true;
	}

	/**
	 * Used after a truck has finished emptying at a crusher location.
	 *
	 * @param	cid	the crusher location.
	 */
	private void updateCrusher(int cid) {
		if (crusherQueues[cid].isEmpty()) {
			crusherIdleOre += (currTime - lastCrusherChange[cid]) * (numCrushers[cid] - numEmptying[cid]) / emptyTimesMean[cid];
			numEmptying[cid]--;
			lastCrusherChange[cid] = currTime;
		}
		else {
			int head = crusherQueues[cid].poll();
			instantQueue.add(new Transition(head,currTime,TruckLocation.WAITING_AT_CRUSHER,TruckLocation.EMPTYING,getPriority(head,
				TruckLocation.EMPTYING)));
		}
		numEmpties++;
		lastEmptyEnd[cid] = currTime;
	}

	/**
	 * Used when a truck needs a new destination.
	 *
	 * @param	tid			the truck index.
	 * @param	toShovel	whether the truck is heading towards a shovel.
	 * @return	false if the simulation should terminate,
	 *			true otherwise.
	 */
	private boolean getRoute(int tid, boolean toShovel) {
		int route = nextRoute(tid);
		int[] routeMachines;
		int mid;
		TruckLocation origin;
		if (toShovel) {
			routeMachines = routeCrushers;
			mid = assignedCrusher[tid];
			origin = TruckLocation.WAITING;
		}
		else {
			routeMachines = routeShovels;
			mid = assignedShovel[tid];
			origin = TruckLocation.LEAVING_SHOVEL;
		}
		if (route < 0) {
			if (route == -2) {
				instantQueue.add(new Transition(tid,currTime,origin,TruckLocation.UNUSED,getPriority(tid,TruckLocation.UNUSED)));
				return true;
			}
			return false;
		}
		if (routeMachines[route] != mid) {
			throw new IllegalStateException(String.format("Illegal route supplied: %d at %s %d",route,(toShovel ? "crusher" : "shovel"),
				mid));
		}
		assignedRoute[tid] = route;
		assignedCrusher[tid] = routeCrushers[route];
		assignedShovel[tid] = routeShovels[route];
		TruckLocation target;
		if (toShovel) {
			routePoint[tid] = 0;
			target = (isOneWay[routeRoads[route][0]] ? TruckLocation.APPROACHING_TL_CS : TruckLocation.TRAVEL_TO_SHOVEL);
		}
		else {
			routePoint[tid] = routeLengths[route] - 1;
			target = (isOneWay[routeRoads[route][routePoint[tid]]] ? TruckLocation.APPROACHING_TL_SS : TruckLocation.TRAVEL_TO_CRUSHER);
		}
		instantQueue.add(new Transition(tid,currTime,origin,target,getPriority(tid,target)));
		return true;
	}

	/**
	 * Used after a truck has completed a travel transition.
	 *
	 * @param	tid			the index of the truck that cleared a road.
	 * @param	toShovel	whether the truck was heading towards a shovel or not.
	 * @throws	IllegalStateException if this truck cleared a road before a truck ahead of it.
	 */
	private void clearedRoad(int tid, boolean toShovel) {
		int route = assignedRoute[tid];
		int startPoint = (toShovel ? 0 : routeLengths[route] - 1);
		int point = routePoint[tid];
		int off = (toShovel ? -1 : 1);
		if (point != startPoint) {
			int dir = routeDirections[route][point + off];
			int to = (toShovel ? dir : 1 - dir);
			int prevRoad = routeRoads[route][point + off];
			int front = roadQueues[prevRoad][to].poll();
			if (front != tid) {
				throw new IllegalStateException("Trucks out of order");
			}
			if (isOneWay[prevRoad]) {
				checkLights(prevRoad);
			}
		}
	}

	/**
	 * Get the priority for a transition.
	 * Should only affect event order when a non-random time distribution is used.
	 * Should not be used for travel transitions --
	 * those are controlled by using the roadPriority variable.
	 * 
	 * @param	tid		the index of the truck to transition.
	 * @param	dest	the destination TruckLocation of the transition.
	 * @return	the priority of the transition.
	 * @throws	IllegalArgumentException if dest is illegal.
	 */
	private int getPriority(int tid, TruckLocation dest) {
		switch (dest) {
			case WAITING: {
				return numTrucks * 4 + tid;
			}
			case APPROACHING_TL_CS: {
				return numTrucks * 3 + tid;
			}
			case STOPPED_AT_TL_CS: {
				return tid;
			}
			case TRAVEL_TO_SHOVEL: {
				return numTrucks + tid;
			}
			case APPROACHING_SHOVEL: {
				return numTrucks * 5 + tid;
			}
			case WAITING_AT_SHOVEL: {
				return numTrucks * 2 + tid;
			}
			case FILLING: {
				return numTrucks * 2 + tid;
			}
			case LEAVING_SHOVEL: {
				return numTrucks * 4 + tid;
			}
			case TRAVEL_TO_CRUSHER: {
				return numTrucks + tid;
			}
			case APPROACHING_TL_SS: {
				return numTrucks * 3 + tid;
			}
			case STOPPED_AT_TL_SS: {
				return tid;
			}
			case APPROACHING_CRUSHER: {
				return numTrucks * 5 + tid;
			}
			case WAITING_AT_CRUSHER: {
				return numTrucks * 2 + tid;
			}
			case EMPTYING: {
				return numTrucks * 2 + tid;
			}
			case UNUSED: {
				return -1;
			}
			default: {
				throw new IllegalArgumentException(String.format("Truck cannot transition to: %s",dest));
			}
		}
	}

	/**
	 * Used when a traffic arrives at traffic lights.
	 *
	 * @param	tid			the index of the truck that cleared a road.
	 * @param	toShovel	whether the truck was heading towards a shovel or not.
	 */
	private void arrivedAtLights(int tid, boolean toShovel) {
		int road = routeRoads[assignedRoute[tid]][routePoint[tid]];
		if (!isOneWay[road]) {
			throw new IllegalStateException(String.format("Arrived at lights for two-way road %d",road));
		}
		int dir = routeDirections[assignedRoute[tid]][routePoint[tid]];
		TruckLocation stopTarget;
		TruckLocation travelTarget;
		TruckLocation origin;
		if (toShovel) {
			origin = TruckLocation.APPROACHING_TL_CS;
			stopTarget = TruckLocation.STOPPED_AT_TL_CS;
			travelTarget = TruckLocation.TRAVEL_TO_SHOVEL;
		}
		else {
			dir = 1 - dir;
			origin = TruckLocation.APPROACHING_TL_SS;
			stopTarget = TruckLocation.STOPPED_AT_TL_SS;
			travelTarget = TruckLocation.TRAVEL_TO_CRUSHER;
		}
		int lIndex = lightIndexes[road];
		switch (lights[lIndex]) {
			case RR:
			case YR:
			case RY: {
				instantQueue.add(new Transition(tid,currTime,origin,stopTarget,getPriority(tid,stopTarget)));
				lightQueues[lIndex][dir].add(tid);
				break;
			}
			case RG: {
				if (dir == 0) {
					instantQueue.add(new Transition(tid,currTime,origin,stopTarget,getPriority(tid,stopTarget)));
					lightQueues[lIndex][dir].add(tid);
					if (greedyMode[lIndex]) {
						lights[lIndex] = TrafficLight.RY;
					}
				}
				else {
					instantQueue.add(new Transition(tid,currTime,origin,travelTarget,getPriority(tid,travelTarget)));
				}
				break;
			}
			case GR: {
				if (dir == 0) {
					instantQueue.add(new Transition(tid,currTime,origin,travelTarget,getPriority(tid,travelTarget)));
				}
				else {
					instantQueue.add(new Transition(tid,currTime,origin,stopTarget,getPriority(tid,stopTarget)));
					lightQueues[lIndex][dir].add(tid);
					if (greedyMode[lIndex]) {
						lights[lIndex] = TrafficLight.YR;
					}
				}
				break;
			}
			default: {
				throw new IllegalStateException(String.format("Illegal light configuration: %s",lights[lIndex]));
			}
		}
	}

	/**
	 * Used when a traffic stops at traffic lights.
	 *
	 * @param	tid			the index of the truck that cleared a road.
	 */
	private void stoppedAtLights(int tid) {
		int road = routeRoads[assignedRoute[tid]][routePoint[tid]];
		int lIndex = lightIndexes[road];
		if (greedyMode[lIndex]) {
			checkLights(road);
		}
	}

	/**
	 * Check whether traffic lights on a road are yellow and should change state.
	 * Used after a truck clears a one-lane road.
	 *
	 * @param	road	the road index.
	 */
	private void checkLights(int road) {
		if (roadQueues[road][0].isEmpty() && roadQueues[road][1].isEmpty()) {
			int side;
			int lIndex = lightIndexes[road];
			switch (lights[lIndex]) {
				case YR: {
					lights[lIndex] = TrafficLight.RG;
					side = 1;
					break;
				}
				case RY: {
					lights[lIndex] = TrafficLight.GR;
					side = 0;
					break;
				}
				default: {
					return;
				}
			}
			int size = lightQueues[lIndex][side].size();
			for (int i=0; i<size; i++) {
				int front = lightQueues[lIndex][side].poll();
				roadPriority[road][side]++;
				TruckLocation origin;
				TruckLocation target;
				switch (truckLocs[front]) {
					case APPROACHING_TL_SS:
					case STOPPED_AT_TL_SS: {
						origin = TruckLocation.STOPPED_AT_TL_SS;
						target = TruckLocation.TRAVEL_TO_CRUSHER;
						break;
					}
					case APPROACHING_TL_CS:
					case STOPPED_AT_TL_CS: {
						origin = TruckLocation.STOPPED_AT_TL_CS;
						target = TruckLocation.TRAVEL_TO_SHOVEL;
						break;
					}
					default: {
						throw new IllegalStateException("Truck is at light queue but not stopped");
					}
				}
				instantQueue.add(new Transition(front,currTime,origin,target,roadPriority[road][side]));
			}
			double lightTime = getNextLight(road,lights[lIndex]);
			if (lightTime < 0) {
				throw new RuntimeException("Negative light schedule");
			}
			else if (lightTime == 0) {
				greedyMode[lIndex] = true;
				if (!lightQueues[lIndex][1 - side].isEmpty()) {
					switch (lights[lIndex]) {
						case GR: {
							lights[lIndex] = TrafficLight.YR;
							break;
						}
						case RG: {
							lights[lIndex] = TrafficLight.RY;
							break;
						}
						default: {
							throw new IllegalStateException("Lights not green after change");
						}
					}
					if (size == 0) {
						checkLights(road);
					}
				}
			}
			else {
				lightSchedule.add(new LightChange(road,currTime + lightTime));
				greedyMode[lIndex] = false;
			}
		}
	}

	/**
	 * Advance the simulation to the next scheduled light change.
	 */
	private void updateLights() {
		LightChange change = lightSchedule.poll();
		int road = change.road;
		int lIndex = lightIndexes[road];
		if (change.time < currTime) {
			throw new IllegalStateException("Negative time step");
		}
		currTime = change.time;
		TrafficLight yellow;
		switch (lights[lIndex]) {
			case GR: {
				yellow = TrafficLight.YR;
				break;
			}
			case RG: {
				yellow = TrafficLight.RY;
				break;
			}
			default: {
				throw new IllegalStateException("Light change scheduled for non-green light");
			}
		}
		double lightTime = getNextLight(road,yellow);
		if (lightTime > 0) {
			lightSchedule.add(new LightChange(road,currTime + lightTime));
		}
		else if (lightTime == 0) {
			lights[lIndex] = yellow;
			checkLights(road);
		}
		else {
			throw new RuntimeException("Negative light schedule");
		}
	}

	/**
	 * Removes and returns the next upcoming transition.
	 * 
	 * @return	the next Transition.
	 */
	private Transition getNextEvent() {
		if (!instantQueue.isEmpty()) {
			return instantQueue.poll();
		}
		else {
			return eventQueue.poll();
		}
	}

	/**
	 * Returns but does not remove the next upcoming transition.
	 * 
	 * @return	the next Transition.
	 */
	private Transition peekNextEvent() {
		if (!instantQueue.isEmpty()) {
			return instantQueue.peek();
		}
		else {
			return eventQueue.peek();
		}
	}

	/**
	 * Creates a transition with adjusted travel time and priority to prevent overtaking.
	 * 
	 * @param	tid			the index of the transitioning truck.
	 * @param	toShovel	whether the truck is heading to a shovel or not.
	 * @return	a Transition.
	 */
	private Transition preventCollisions(int tid, boolean toShovel) {
		int point = routePoint[tid];
		int route = assignedRoute[tid];
		int road = routeRoads[route][point];
		int dir = routeDirections[route][point];
		int to = (toShovel ? dir : 1 - dir);
		double travelTime = tgen.nextTime(roadTravelTimesMean[road][to],roadTravelTimesSD[road][to]) * (toShovel ? 1 : fullSlowdown);
		double actualArrival = Math.max(currTime + travelTime,roadAvailable[road][to]);
		roadAvailable[road][to] = actualArrival;
		TruckLocation travelLoc;
		TruckLocation targetLoc;
		if (toShovel) {
			travelLoc = TruckLocation.TRAVEL_TO_SHOVEL;
			if (point == routeLengths[route] - 1) {
				targetLoc = TruckLocation.APPROACHING_SHOVEL;
			}
			else if (isOneWay[routeRoads[route][point + 1]]) {
				targetLoc = TruckLocation.APPROACHING_TL_CS;
			}
			else {
				targetLoc = TruckLocation.TRAVEL_TO_SHOVEL;
			}
		}
		else {
			travelLoc = TruckLocation.TRAVEL_TO_CRUSHER;
			if (point == 0) {
				targetLoc = TruckLocation.APPROACHING_CRUSHER;
			}
			else if (isOneWay[routeRoads[route][point - 1]]) {
				targetLoc = TruckLocation.APPROACHING_TL_SS;
			}
			else {
				targetLoc = TruckLocation.TRAVEL_TO_CRUSHER;
			}
		}
		roadPriority[road][to]++;
		roadQueues[road][to].add(tid);
		return new Transition(tid,actualArrival,travelLoc,targetLoc,roadPriority[road][to]);
	}

	/**
	 * Get the number of empties.
	 * 
	 * @return	the number of empties.
	 */
	public int getNumEmpties() {
		return numEmpties;
	}

	/**
	 * @return	true if ready() has been called once since the simulation was last updated.
	 * @see	ready()
	 */
	protected boolean isReady() {
		return isReady;
	}

	/**
	 * Get the simulation time in stored state.
	 *
	 * @return	the simulation time.
	 */
	protected double getSimTime() {
		return simTime;
	}

	/**
	 * Get the location of a truck in stored state.
	 *
	 * @param	tid	the truck index.
	 * @return	the location.
	 */
	protected TruckLocation getSimLoc(int tid) {
		return simLocs[tid];
	}

	/**
	 * Get the location of a truck in the current simulation.
	 *
	 * @param	tid	the truck index.
	 * @return	the location.
	 */
	protected TruckLocation getTruckLoc(int tid) {
		return truckLocs[tid];
	}

	/**
	 * Get the assigned route of a truck in the current simulation.
	 *
	 * @param	tid	the truck index.
	 * @return	the route index.
	 */
	protected int getAssignedRoute(int tid) {
		return assignedRoute[tid];
	}

	/**
	 * Get the simulation time in the current simulation.
	 *
	 * @return	the current simulation time.
	 */
	protected double getCurrentTime() {
		return currTime;
	}

	/**
	 * Get the assigned crusher location of a truck in the current simulation.
	 *
	 * @param	tid	the truck index.
	 * @return	the crusher index.
	 */
	protected int getAssignedCrusher(int tid) {
		return assignedCrusher[tid];
	}

	/**
	 * Get the assigned shovel of a truck in the current simulation.
	 *
	 * @param	tid	the truck index.
	 * @return	the shovel index.
	 */
	protected int getAssignedShovel(int tid) {
		return assignedShovel[tid];
	}

	/**
	 * Get the number of crusher locations.
	 *
	 * @return	the number of crusher locations.
	 */
	protected int getNumCrusherLocs() {
		return numCrusherLocs;
	}

	/**
	 * Get the number of shovels.
	 *
	 * @return	the number of shovels.
	 */
	protected int getNumShovels() {
		return numShovels;
	}

	/**
	 * Get the number of trucks.
	 *
	 * @return	the number of trucks.
	 */
	protected int getNumTrucks() {
		return numTrucks;
	}

	/**
	 * Get the number of routes.
	 *
	 * @return	the number of routes.
	 */
	protected int getNumRoutes() {
		return numRoutes;
	}

	/**
	 * Get the crusher location linked to a route.
	 *
	 * @param	route	the route index.
	 * @return	the crusher index.
	 */
	protected int getRouteCrusher(int route) {
		return routeCrushers[route];
	}

	/**
	 * Get the shovel linked to a route.
	 *
	 * @param	route	the route index.
	 * @return	the shovel index.
	 */
	protected int getRouteShovel(int route) {
		return routeShovels[route];
	}

	/**
	 * Get the assigned shovel of a truck in stored state.
	 *
	 * @param	tid	the truck index.
	 * @return	the shovel index.
	 */
	protected int getSimAssignedShovel(int tid) {
		return simAShovel[tid];
	}

	/**
	 * Get the assigned crusher location of a truck in stored state.
	 *
	 * @param	tid	the truck index.
	 * @return	the crusher index.
	 */
	protected int getSimAssignedCrusher(int tid) {
		return simACrusher[tid];
	}

	/**
	 * Get the index of a traffic light set associated with a one-lane road.
	 *
	 * @param	road	the road index.
	 * @return	the light index,
	 *			or -1 if the road is two-lane.
	 */
	protected int getLightIndex(int road) {
		return lightIndexes[road];
	}

	/**
	 * Get the number of one-lane roads.
	 *
	 * @return the number of one-lane roads.
	 */
	protected int getNumOneWay() {
		return numOneWay;
	}

	// protected double getMeanFillTime(int sid) {
	// 	return fillTimesMean[sid];
	// }

	/**
	 * Get the starting time of a truck's last service at a shovel or crusher,
	 * in the current simulation.
	 *
	 * @param	tid	the truck index.
	 * @return	the starting time.
	 */
	protected double getLastServiceStart(int tid) {
		return lastServiceStart[tid];
	}

	/**
	 * Get the total waiting time spent at shovels and crushers,
	 * for a truck in the current simulation.
	 *
	 * @param	tid	the truck index.
	 * @return	the waiting time.
	 */
	protected double getServiceWaitingTime(int tid) {
		return serviceWaitingTime[tid];
	}

	/**
	 * For the last service of a given truck,
	 * get the time the servicing machine became available prior to servicing this truck,
	 * in the current simulation.
	 *
	 * @param	tid	the truck index.
	 * @return	the time of availability.
	 */
	protected double getServiceAvailableTime(int tid) {
		return serviceAvailableTime[tid];
	}

	/**
	 * Get the starting time of a truck's last waiting period,
	 * in the current simulation.
	 *
	 * @param	tid	the truck index.
	 * @return	the starting time.
	 */
	protected double getLastWaitStart(int tid) {
		return lastWaitStart[tid];
	}

	/**
	 * Get the total waiting time,
	 * for a truck in the current simulation.
	 *
	 * @param	tid	the truck index.
	 * @return	the waiting time.
	 */
	protected double getTotalWaitingTime(int tid) {
		return serviceWaitingTime[tid] + roadWaitingTime[tid];
	}

	/**
	 * Get the total idle time,
	 * for a shovel in the current simulation.
	 *
	 * @param	sid	the shovel index.
	 * @return	the waiting time.
	 */
	protected double getTotalShovelWaitingTime(int sid) {
		return shovelWaitingTime[sid];
	}

	// protected int getSuccessfulFills() {
	// 	return successfulFills;
	// }

	// protected int getSuccessfulFills(int cid) {
	// 	return successfulFillsFromCrusher[cid];
	// }

	// protected int getSuccessfulEmpties() {
	// 	return successfulEmpties;
	// }

	// protected double getShovelIdleOre(double endtime) {
	// 	double out = shovelIdleOre;
	// 	for (int i=0; i<numShovels; i++) {
	// 		if (!shovelInUse[i]) {
	// 			out += (endtime - lastFillEnd[i]) / fillTimesMean[i];
	// 		}
	// 	}
	// 	return out;
	// }

	/**
	 * Get the wasted productivity of crushers in the current simulation,
	 * measured in truckloads that could have been processed while idle,
	 * up to a given end point.
	 *
	 * @param	endtime	the end point of the simulation.
	 * @return	the wasted 'idle ore'.
	 */
	protected double getCrusherIdleOre(double endtime) {
		double out = crusherIdleOre;
		for (int i=0; i<numCrusherLocs; i++) {
			if (numEmptying[i] < numCrushers[i]) {
				out += (endtime - lastCrusherChange[i]) * (numCrushers[i] - numEmptying[i]) / emptyTimesMean[i];
			}
		}
		return out;
	}

	// protected double getIdleOre(double endtime) {
	// 	return getShovelIdleOre(endtime) + getCrusherIdleOre(endtime);
	// }

	/**
	 * Get the wasted productivity of trucks in the current simulation,
	 * measured in truckloads that could have been processed while idle,
	 * based on net servicing rate of crushers divided evenly among trucks,
	 * up to a given end point.
	 *
	 * @param	endtime	the end point of the simulation.
	 * @return	the wasted 'idle ore'.
	 */
	protected double getTruckIdle(double endtime) {
		double out = truckIdle;
		for (int i=0; i<numTrucks; i++) {
			switch (truckLocs[i]) {
				case STOPPED_AT_TL_CS:
				case WAITING_AT_SHOVEL:
				case STOPPED_AT_TL_SS:
				case WAITING_AT_CRUSHER:
				case UNUSED: {
					out += (endtime - lastWaitStart[i]) * (totalCrushingRate / numTrucks);
					break;
				}
			}
		}
		return out;
	}

	/**
	 * Get the average cycle time of all trucks that unloaded during the current simulation.
	 *
	 * @return	the average cycle time.
	 */
	protected double getAverageCycleTimes() {
		int numCycles = completeCycles.size();
		if (numCycles == 0) {
			return 1e9;
		}
		double out = 0;
		for (int i=0; i<numCycles; i++) {
			out += completeCycles.get(i) / numCycles;
		}
		return out;
	}

	// protected double getNetSquaredTruckIdle(double endtime) {
	// 	double out = 0;
	// 	for (int i=0; i<numTrucks; i++) {
	// 		double w = individualTruckWaitingTime[i];
	// 		switch (truckLocs[i]) {
	// 			case STOPPED_AT_TL_CS:
	// 			case WAITING_AT_SHOVEL:
	// 			case STOPPED_AT_TL_SS:
	// 			case WAITING_AT_CRUSHER:
	// 			case UNUSED: {
	// 				w += (endtime - lastWaitStart[i]);
	// 				break;
	// 			}
	// 		}
	// 		out += w * w;
	// 	}
	// 	return out;
	// }

}