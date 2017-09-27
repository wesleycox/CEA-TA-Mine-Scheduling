package mines.ea.fitness.sim.shift;

import mines.util.*;
import mines.sim.*;
import java.util.*;

/**
 * Simulator class for a complex road network to simulate entire shifts.
 * Intended to be extended as a fitness function.
 */
public abstract class SimpleSimFitnessFunction4 {

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

	private TimeDistribution tgen;	//the distribution used for generating all stochastic values.

	/*
	 * Current simulation variables.
	 */
	private double currTime;							//current time in current simulation.
	private PriorityQueue<Transition> eventQueue;		//upcoming non-instant transitions in current simulation.
	private PriorityQueue<Transition> instantQueue;		//upcoming instant transitions in current simulation.
	private TruckLocation[] truckLocs;					//current locations of each truck in current simulation.
	private int[] assignedShovel;						//current assigned shovel for each truck in current simulation.
	private int[] assignedCrusher;						//current assigned crusher for each truck in current simulation.
	private int[] assignedRoute;						//current assigned route for each truck in current simulation.
	private int[] routePoint;							//current route index for each truck in current simulation.
	private IntQueue[] crusherQueues;					//queues for each crusher location in current simulation.
	private IntQueue[] shovelQueues;					//queues for each shovel in current simulation.
	private int[] numEmptying;							//number of crushers active at each crusher location in current simulation.
	private boolean[] shovelInUse;						//whether each shovel is in use in current simulation.
	private TrafficLight[] lights;						//state of each traffic light in current simulation.
	private IntQueue[][] lightQueues;					//queues for each traffic light in current simulation.
	private IntQueue[][] roadQueues;					//order of trucks on each road in current simulation.
	private PriorityQueue<LightChange> lightSchedule;	//upcoming changes from green lights in current simulation.
	private boolean[] greedyMode;						//whether each light is set to greedy rules in current simulation.
	private double[][] roadAvailable;					//the minimum possible arrival time for each road end in current simulation.
	private int[][] roadPriority;						//priority values used for transitions to preserve order.
	private double[] arrivalTime;						//last transition time per truck in current simulation.
	private double[] intendedArrival;					//intended transition times before considering slowdowns
	private int numEmpties;								//number of empties completed in current simulation.

	/**
	 * Simulator constructor.
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
	public SimpleSimFitnessFunction4(int numTrucks, int numShovels, int numCrusherLocs, int[] numCrushers, int numRoads, 
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

		this.tgen = tgen;

		eventQueue = new PriorityQueue<>();
		instantQueue = new PriorityQueue<>();
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
		lightSchedule = new PriorityQueue<>();
		greedyMode = new boolean[numOneWay];
		roadAvailable = new double[numRoads][2];
		roadPriority = new int[numRoads][2];
		arrivalTime = new double[numTrucks];
		intendedArrival = new double[numTrucks];
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
	 * Resets the simulation to the start of a shift.
	 */
	protected void reReady() {
		currTime = 0;
		eventQueue.clear();
		instantQueue.clear();
		for (int i=0; i<numTrucks; i++) {
			instantQueue.add(new Transition(i,currTime,TruckLocation.WAITING,TruckLocation.WAITING,getPriority(i,TruckLocation.WAITING)));
			truckLocs[i] = TruckLocation.WAITING;
			assignedCrusher[i] = (initialCrushers == null ? i % numCrusherLocs : initialCrushers[i]);
			assignedRoute[i] = defaultRoute[assignedCrusher[i]];
			assignedShovel[i] = routeShovels[assignedRoute[i]];
			routePoint[i] = 0;
			arrivalTime[i] = currTime;
			intendedArrival[i] = currTime;
		}
		for (int i=0; i<numCrusherLocs; i++) {
			crusherQueues[i].clear();
			numEmptying[i] = 0;
		}
		for (int i=0; i<numShovels; i++) {
			shovelQueues[i].clear();
			shovelInUse[i] = false;
		}
		for (int i=0; i<numOneWay; i++) {
			lights[i] = TrafficLight.GR;
			greedyMode[i] = true;
			for (int j=0; j<2; j++) {
				lightQueues[i][j].clear();
			}
		}
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				roadQueues[i][j].clear();
				roadAvailable[i][j] = currTime;
				roadPriority[i][j] = Integer.MIN_VALUE;
			}
		}
		lightSchedule.clear();
		numEmpties = 0;
	}

	/**
	 * Runs a simulation.
	 * 
	 * @param	runtime	the termination time of the simulation.
	 */
	public void simulate(double runtime) {
		while (true) {
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
			singleEvent();
		}
	}

	/**
	 * Move the simulation forward by one transition.
	 * 
	 * @throws	IllegalStateException	if a simulation error occurred,
	 *									e.g. illegal route index, 
	 *									approaching non-existent traffic lights,
	 *									illegal traffic light state,
	 *									illegal transition.
	 */
	private void singleEvent() {
		Transition next = getNextEvent();
		if (next.getTime() < currTime) {
			throw new IllegalStateException("Negative time step");
		}
		currTime = next.getTime();
		int tid = next.getIndex();
		TruckLocation tOrigin = next.getSource();
		TruckLocation tDest = next.getTarget();
		arrivalTime[tid] = currTime;
		if (tOrigin == truckLocs[tid]) {
			switch (tDest) {
				case WAITING: {
					if (tOrigin == TruckLocation.EMPTYING) {
						updateCrusher(assignedCrusher[tid]);
					}
					getRoute(tid,true);
					break;
				}
				case TRAVEL_TO_SHOVEL: {
					int road = routeRoads[assignedRoute[tid]][routePoint[tid]];
					if (!isOneWay[road]) {
						clearedRoad(tid,true);
					}
					eventQueue.add(preventCollisions(tid,true));
					routePoint[tid]++;
					break;
				}
				case APPROACHING_TL_CS: {
					clearedRoad(tid,true);
					arrivedAtLights(tid,true);
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
					intendedArrival[tid] = currTime + fillTime;
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
					}
					getRoute(tid,false);
					break;
				}
				case TRAVEL_TO_CRUSHER: {
					int road = routeRoads[assignedRoute[tid]][routePoint[tid]];
					if (!isOneWay[road]) {
						clearedRoad(tid,false);
					}
					eventQueue.add(preventCollisions(tid,false));
					routePoint[tid]--;
					break;
				}
				case APPROACHING_TL_SS: {
					clearedRoad(tid,false);
					arrivedAtLights(tid,false);
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
						numEmptying[cid]++;
					}
					else {
						nextLoc = TruckLocation.WAITING_AT_CRUSHER;
						crusherQueues[cid].add(tid);
					}
					instantQueue.add(new Transition(tid,currTime,tDest,nextLoc,getPriority(tid,nextLoc)));
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
					intendedArrival[tid] = currTime + emptyTime;
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
	}

	/**
	 * Used after a truck has finished emptying at a crusher location.
	 *
	 * @param	cid	the crusher location.
	 */
	private void updateCrusher(int cid) {
		if (crusherQueues[cid].isEmpty()) {
			numEmptying[cid]--;
		}
		else {
			int head = crusherQueues[cid].poll();
			instantQueue.add(new Transition(head,currTime,TruckLocation.WAITING_AT_CRUSHER,TruckLocation.EMPTYING,getPriority(head,
				TruckLocation.EMPTYING)));
		}
		numEmpties++;
	}

	/**
	 * Used when a truck needs a new destination.
	 *
	 * @param	tid			the truck index.
	 * @param	toShovel	whether the truck is heading towards a shovel.
	 */
	private void getRoute(int tid, boolean toShovel) {
		int route = nextRoute(tid);
		int[] routeMachines;
		int mid;
		String machine;
		if (toShovel) {
			routeMachines = routeCrushers;
			mid = assignedCrusher[tid];
			machine = "crusher";
		}
		else {
			routeMachines = routeShovels;
			mid = assignedShovel[tid];
			machine = "shovel";
		}
		if (routeMachines[route] != mid) {
			throw new IllegalStateException(String.format("Illegal route supplied: %d at %s %d",route,machine,mid));
		}
		assignedRoute[tid] = route;
		assignedCrusher[tid] = routeCrushers[route];
		assignedShovel[tid] = routeShovels[route];
		TruckLocation origin;
		TruckLocation target;
		if (toShovel) {
			routePoint[tid] = 0;
			origin = TruckLocation.WAITING;
			target = (isOneWay[routeRoads[route][0]] ? TruckLocation.APPROACHING_TL_CS : TruckLocation.TRAVEL_TO_SHOVEL);
		}
		else {
			routePoint[tid] = routeLengths[route] - 1;
			origin = TruckLocation.LEAVING_SHOVEL;
			target = (isOneWay[routeRoads[route][routePoint[tid]]] ? TruckLocation.APPROACHING_TL_SS : TruckLocation.TRAVEL_TO_CRUSHER);
		}
		instantQueue.add(new Transition(tid,currTime,origin,target,getPriority(tid,target)));
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
		intendedArrival[tid] = currTime + travelTime;
		double actualArrival = Math.max(intendedArrival[tid],roadAvailable[road][to]);
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
	 * Get the progress values for each truck,
	 * specifying the fractional progress until transitioning for trucks in timed states,
	 * or the waiting time for trucks in waiting states.
	 *
	 * @return	a double array of progress values
	 */
	private double[] getProgress() {
		double[] progress = new double[numTrucks];
		boolean[] marked = new boolean[numTrucks];
		for (int i=0; i<numRoads; i++) {
			for (int j=0; j<2; j++) {
				double minProgress = 1;
				for (int k=0; k<roadQueues[i][j].size(); k++) {
					int t = roadQueues[i][j].get(k);
					double intendedProgress = (currTime - arrivalTime[t]) / (intendedArrival[t] - arrivalTime[t]);
					minProgress = Math.min(minProgress,intendedProgress);
					progress[t] = minProgress;
					marked[t] = true;
				}
			}
		}
		for (int i=0; i<numTrucks; i++) {
			switch (truckLocs[i]) {
				case WAITING:
				case APPROACHING_TL_CS:
				case APPROACHING_SHOVEL:
				case LEAVING_SHOVEL:
				case APPROACHING_TL_SS:
				case APPROACHING_CRUSHER: {
					break;
				}
				case TRAVEL_TO_SHOVEL:
				case TRAVEL_TO_CRUSHER: {
					if (!marked[i]) {
						throw new IllegalStateException("Road queues are incorrect");
					}
					break;
				}
				case FILLING:
				case EMPTYING: {
					progress[i] = (currTime - arrivalTime[i]) / (intendedArrival[i] - arrivalTime[i]);
					break;
				}
				case STOPPED_AT_TL_CS:
				case WAITING_AT_SHOVEL:
				case STOPPED_AT_TL_SS:
				case WAITING_AT_CRUSHER: {
					progress[i] = currTime - arrivalTime[i];
					break;
				}
				default: {
					throw new IllegalStateException(String.format("Truck has entered illegal state: %s",truckLocs[i]));
				}
			}
		}
		return progress;
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
	protected int getRouteCrusher(int tid) {
		return routeCrushers[tid];
	}

	/**
	 * Get the shovel linked to a route.
	 *
	 * @param	route	the route index.
	 * @return	the shovel index.
	 */
	protected int getRouteShovel(int tid) {
		return routeShovels[tid];
	}

	/**
	 * Get the number of one-lane roads.
	 *
	 * @return the number of one-lane roads.
	 */
	protected int getNumOneWay() {
		return numOneWay;
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

}