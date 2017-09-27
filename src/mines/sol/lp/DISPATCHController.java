package mines.sol.lp;

import mines.sol.TimerBasedController;
import mines.util.*;
import mines.ea.fitness.sim.cont.SimFitnessFunction4WTimer;
import mines.sim.*;
import java.util.*;

/**
 * Controller based on the DISPATCH algorithm.
 * Uses cyclic light schedules.
 *
 * Based on the algorithm presented in:
 * 
 * White, J. W., & Olson, J. P. (1986). 
 * Computer-based dispatching in mines with concurrent operating objectives. 
 * Min. Eng.(Littleton, Colo.);(United States), 38(11).
 *
 * White, J. W., Olson, J. P., & Vohnout, S. I. (1993). 
 * On improving truck/shovel productivity in open pit mines. 
 * CIM bulletin, 86, 43-43.
 */
public class DISPATCHController extends TimerBasedController {

	private static class Pair implements Comparable<Pair> {

		int i;
		double d;

		public Pair(int i, double d) {
			this.i = i;
			this.d = d;
		}

		public int compareTo(Pair other) {
			int dc = Double.compare(this.d,other.d);
			return (dc == 0 ? this.i - other.i : dc);
		}
	}

	private static class PairList extends ArrayList<Pair> {}

	private static final double INFINITY = 1e9;

	/**
	 * Stored state simulator for estimating idle times.
	 */
	private static class Simulator extends SimFitnessFunction4WTimer {

		private PairList[] incomingTrucks;	//the upcoming trucks for each crusher.
		private boolean allowReturns;		//whether to dispatch from shovels.
		private int[] schedule;				//the schedule to simulate.
		private int currentDispatch;		//the current truck under consideration in the main algorithm.
		private double baseWaitingTime;		//the waiting time of the considered truck up to its dispatch from a crusher.
		private double baseStartingTime;	//the dispatch time of the considered truck from a crusher.

		public Simulator(int numTrucks, int numShovels, int numCrusherLocs, int[] numCrushers, int numRoads, double[] emptyTimesMean, 
			double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[][] roadTravelTimesMean, 
			double[][] roadTravelTimesSD, double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections,
			int[] routeLengths, int[] routeShovels, int[] routeCrushers, double[][] lightSchedule) {
			super(numTrucks,numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,
				roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeShovels,
				routeCrushers,new AverageTimes(),lightSchedule);
			incomingTrucks = new PairList[numCrusherLocs];
			for (int i=0; i<numCrusherLocs; i++) {
				incomingTrucks[i] = new PairList();
			}
			schedule = new int[numTrucks];
		}

		/**
		 * Get the next assignment from a fixed-truck-assignment schedule,
		 * or a termination request (-1) if the observed truck has finished filling,
		 * or a parking request (-2) if any other truck finishes filling and returns aren't being considered.
		 */
		protected int nextRoute(int tid) {
			TruckLocation currentLoc = getTruckLoc(tid);
			if (currentLoc == TruckLocation.FILLING || currentLoc == TruckLocation.LEAVING_SHOVEL) {
				if (tid == currentDispatch) {
					return -1;
				}
				else if (allowReturns) {
					return getAssignedRoute(tid);
				}
				else {
					return -2;
				}
			}
			else {
				incomingTrucks[getAssignedCrusher(tid)].add(new Pair(tid,getCurrentTime()));
				if (tid == currentDispatch) {
					baseWaitingTime = getTotalWaitingTime(tid);
					baseStartingTime = getCurrentTime();
				}
				return schedule[tid];
			}
		}

		/**
		 * Get lists of trucks that arrive at each crusher soon.
		 */
		public PairList[] getIncomingTrucks() {
			if (!isReady()) {
				ready();
			}
			reReady();
			int numCrusherLocs = getNumCrusherLocs();
			int numTrucks = getNumTrucks();
			incomingTrucks = new PairList[numCrusherLocs];
			for (int i=0; i<numCrusherLocs; i++) {
				incomingTrucks[i] = new PairList();
			}
			for (int i=0; i<numTrucks; i++) {
				schedule[i] = -2;
			}
			currentDispatch = -1;
			allowReturns = false;
			simulate(INFINITY);
			return incomingTrucks;
		}

		/**
		 * Get important times used by the main algorithm,
		 * for the considered truck and shovel,
		 * with a given FTA schedule.
		 *
		 * @param	tid			the considered truck.
		 * @param	sid			the considered shovel.
		 * @param	schedule	the FTA schedule.
		 * @return	an array [TW,SW,ST],
		 *			for TW - truck waiting time from dispatch to filling for the considered truck,
		 *			SW - shovel waiting until considered truck starts filling,
		 *			ST - time from dispatch to filling for the considered truck.
		 */
		public double[] getImportantTimes(int tid, int sid, int[] schedule) {
			if (!isReady()) {
				ready();
			}
			reReady();
			allowReturns = true;
			int numCrusherLocs = getNumCrusherLocs();
			int numTrucks = getNumTrucks();
			incomingTrucks = new PairList[numCrusherLocs];
			for (int i=0; i<numCrusherLocs; i++) {
				incomingTrucks[i] = new PairList();
			}
			for (int i=0; i<numTrucks; i++) {
				this.schedule[i] = schedule[i];
			}
			currentDispatch = tid;
			baseWaitingTime = 0;
			baseStartingTime = getSimTime();
			simulate(INFINITY);
			return new double[]{getTotalWaitingTime(tid) - baseWaitingTime,getTotalShovelWaitingTime(sid),
				getLastServiceStart(tid) - /*getSimTime()*/ baseStartingTime};
		}

		/**
		 * Get the stored-state location of a truck.
		 */
		public TruckLocation getSimLoc(int tid) {
			return super.getSimLoc(tid);
		}

	}

	private int numTrucks;			//the number of trucks.
	private int numShovels;			//number of shovels.
	private int numCrusherLocs;		//number of crusher locations.
	private int numRoutes;			//number of routes.
	private int[] routeShovels;		//shovels connected to each route.
	private int[] routeCrushers;	//crusher locations connected to each route.

	private boolean dispatchByShovel;	//whether to dispatch by considering shovels or considering routes.

	private double[][] flow;					//desired haulage rates along routes in both directions.
	private double totalDiggingRate;			//net digging rate of shovels.
	private double requiredTrucks;				//estimated required trucks to satisfy haulage rates.
	private double[] shovelFlow;				//the truck flow through shovels based on haulage rates.
	private double[] requiredShovelAllocation;	//the required allocation for each shovel.
	private double[] minTimeToShovel;			//the minimum time to each shovel, not including waiting times.
	private int[][] crusherToShovelRoute;		//the route between each crusher/shovel pair.
	private double[][] meanRouteTime;
	//the estimated route clear time in both directions, including waiting times at traffic lights.
	private IntList[] routesFromShovel;			//lists of routes out of each shovel.
	private int[] initialCrushers;				//the initial locations of each truck.

	private double simTime;						//the current simulation time.
	private boolean[] atCrusher;				//whether each truck is at the crusher in the current simulation.
	private int[] assignedCrusher;				//the assigned crusher for each truck in the current simulation.
	private int[] assignedShovel;				//the assigned shovel for each truck in the current simulation.
	private double[] simLastDispatchToShovel;	//the last dispatch to each shovel in the current simulation.
	private double[] simAllocatedToShovel;		//the allocation on paths to each shovel.
	private double[][] simLastDispatchOnRoute;	//the last dispatch on each route in both directions in the current simulation.
	private double[][] simAllocatedOnRoute;		//the allocation on each route in both directions.

	private Simulator ff;	//the simulator for estimating idle times.

	/**
	 * Controller constructor.
	 *
	 * @param 	numTrucks					the number of trucks.
	 * @param 	numShovels					the number of shovels.
	 * @param 	numCrusherLocs				the number of crusher locations.
	 * @param 	numCrushers					an array of the number of crushers at each location.
	 * @param 	numRoads					the number of roads.
	 * @param 	emptyTimesMean				an array of average emptying times for each crusher.
	 * @param 	emptyTimesSD				an array of standard deviations of emptying times for each crusher.
	 * @param 	fillTimesMean				an array of average filling times for each shovel.
	 * @param 	fillTimesSD					an array of standard deviations of filling times for each shovel.
	 * @param 	roadTravelTimesMean			a 2D array of average travelling times on each road in both directions.
	 * @param 	roadTravelTimesSD			a 2D array of standard deviations of travelling time on each road in both directions.
	 * @param 	fullSlowdown				the travel time increase for travelling full.
	 * @param 	isOneWay					an array specifying whether each road is one-lane.
	 * @param 	numRoutes					the number of routes.
	 * @param 	routeRoads					a 2D array listing the roads comprising each route.
	 * @param 	routeDirections				a 2D array listing the directions travelled on each road in each route.
	 * @param 	routeLengths				an array of the number of roads in each route.
	 * @param 	routeShovels				an array of the shovel at the end of each route.
	 * @param 	routeCrushers				an array of the crusher at the start of each route.
	 * @param 	dispatchByShovel			whether to dispatch by considering shovels -
	 *										the original algorithm is ambiguous about whether to consider the net allocation to each shovel,
	 *										or to consider each route as distinct -
	 *										leading to two different interpretations,
	 *										(in the single-crusher case the two interpretations are equivalent).
	 * @param	flow						a 2D array specifying the desired haulage rates on each route in both directions.
	 * @param	totalDiggingRate			the net truck flow through all shovels.
	 * @param	requiredTrucks				the estimated required number of trucks to satisfy flow,
	 *										as defined in the LP model.
	 * @param	shovelFlow					an array of the net flow through each shovel.
	 * @param	requiredShovelAllocation	an array of the desired allocation to each shovel -
	 *										allocation is defined in the original algorithm -
	 *										it reduces over time at the rate of the desired truck flow.
	 * @param	minTimeToShovel				an array of the minimum travel time to each shovel, not including waiting time.
	 * @param	crusherToShovelRoute		a 2D array specifying the shortest route between each crusher-shovel pair.
	 * @param	meanRouteTime				a 2D array of the expected travel time along each route in both directions,
	 *										including expected waiting times at traffic lights,
	 *										estimated with Monte Carlo simulation.
	 * @param	routesFromShovel			an array of lists of routes out of each shovel.
	 * @param	initialCrushers				an array of initial crusher locations for each truck, 
	 *										based on truck flow through each crusher.
	 * @param	numOneWay					the number of one-lane roads.
	 * @param	lightIndexes				an array of light indexes for each road.
	 * @param 	lightSchedule				a 2D array specifying the cyclic light schedule.
	 */
	public DISPATCHController(int numTrucks, int numShovels, int numCrusherLocs, int[] numCrushers, int numRoads, double[] emptyTimesMean, 
		double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[][] roadTravelTimesMean, double[][] roadTravelTimesSD, 
		double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections, int[] routeLengths, 
		int[] routeShovels, int[] routeCrushers, boolean dispatchByShovel, double[][] flow, double totalDiggingRate, double requiredTrucks,
		double[] shovelFlow, double[] requiredShovelAllocation, double[] minTimeToShovel, int[][] crusherToShovelRoute, 
		double[][] meanRouteTime, IntList[] routesFromShovel, int[] initialCrushers, int numOneWay, int[] lightIndexes, 
		double[][] lightSchedule) {
		super(numOneWay,lightIndexes,lightSchedule);
		this.numTrucks = numTrucks;
		this.numShovels = numShovels;
		this.numCrusherLocs = numCrusherLocs;
		this.numRoutes = numRoutes;
		this.routeShovels = routeShovels;
		this.routeCrushers = routeCrushers;

		this.dispatchByShovel = dispatchByShovel;

		// this.flow = flow;
		if (flow.length < numRoutes) {
			throw new IllegalArgumentException(String.format("Truck flow not defined for all routes: %d",flow.length));
		}
		this.flow = new double[numRoutes][2];
		for (int i=0; i<numRoutes; i++) {
			for (int j=0; j<2; j++) {
				this.flow[i][j] = flow[i][j];
			}
		}
		this.totalDiggingRate = totalDiggingRate;
		this.requiredTrucks = requiredTrucks;
		// this.shovelFlow = shovelFlow;
		if (shovelFlow.length < numShovels) {
			throw new IllegalArgumentException(String.format("Net flow not defined for all shovels: %d",shovelFlow.length));
		}
		this.shovelFlow = Arrays.copyOf(shovelFlow,numShovels);
		// this.requiredShovelAllocation = requiredShovelAllocation;
		if (requiredShovelAllocation.length < numShovels) {
			throw new IllegalArgumentException(String.format("Desired allocation not defined for all shovels: %d",
				requiredShovelAllocation.length));
		}
		this.requiredShovelAllocation = Arrays.copyOf(requiredShovelAllocation,numShovels);
		// this.minTimeToShovel = minTimeToShovel;
		if (minTimeToShovel.length < numShovels) {
			throw new IllegalArgumentException(String.format("Minimum travel time not defined for all shovels: %d",minTimeToShovel.length));
		}
		this.minTimeToShovel = Arrays.copyOf(minTimeToShovel,numShovels);
		// this.crusherToShovelRoute = crusherToShovelRoute;
		if (crusherToShovelRoute.length < numCrusherLocs) {
			throw new IllegalArgumentException(String.format("Routes not defined for all crushers: %d",crusherToShovelRoute.length));
		}
		this.crusherToShovelRoute = new int[numCrusherLocs][numShovels];
		for (int i=0; i<numCrusherLocs; i++) {
			if (crusherToShovelRoute[i].length < numShovels) {
				throw new IllegalArgumentException(String.format("Routes not defined for all shovels out of crusher %d: %d",i,
					crusherToShovelRoute[i].length));
			}
			for (int j=0; j<numShovels; j++) {
				this.crusherToShovelRoute[i][j] = crusherToShovelRoute[i][j];
			}
		}
		// this.meanRouteTime = meanRouteTime;
		if (meanRouteTime.length < numRoutes) {
			throw new IllegalArgumentException(String.format("Expected clear time not defined for all routes: %d",meanRouteTime.length));
		}
		this.meanRouteTime = new double[numRoutes][2];
		for (int i=0; i<numRoutes; i++) {
			for (int j=0; j<2; j++) {
				this.meanRouteTime[i][j] = meanRouteTime[i][j];
			}
		}
		this.routesFromShovel = routesFromShovel;
		// this.initialCrushers = initialCrushers;
		if (initialCrushers.length < numTrucks) {
			throw new IllegalArgumentException(String.format("Initial locations not defined for all trucks: %d",initialCrushers.length));
		}
		this.initialCrushers = Arrays.copyOf(initialCrushers,numTrucks);

		atCrusher = new boolean[numTrucks];
		assignedCrusher = new int[numTrucks];
		assignedShovel = new int[numTrucks];
		simLastDispatchToShovel = new double[numShovels];
		simAllocatedToShovel = new double[numShovels];
		simLastDispatchOnRoute = new double[numRoutes][2];
		simAllocatedOnRoute = new double[numRoutes][2];

		ff = new Simulator(numTrucks,numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,
			roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeShovels,
			routeCrushers,lightSchedule);

		// reset();
	}

	/**
	 * Determine the next route assignment for a given truck.
	 * Uses the DISPATCH algorithm,
	 * which involves sorting trucks by expected dispatch time at the crusher,
	 * sorting paths by a priority function,
	 * and assigning trucks to paths to minimise a lost-tons function.
	 * See the references for more information.
	 *
	 * @param	tid	the requesting truck index.
	 * @return	the route index.
	 */
	public int nextRoute(int tid) {
		if (atCrusher[tid]) {
			if (dispatchByShovel) {
				return nextRouteByShovelMethod(tid);
			}
			else {
				return nextRouteByRouteMethod(tid);
			}
		}
		else {
			return nextRouteFromShovel(tid);
		}
	}

	/**
	 * Get the next route -
	 * Sorts shovels by neediness,
	 * considering the net allocation to each shovel.
	 */
	private int nextRouteByShovelMethod(int tid) {
		PairList[] incomingTrucks = ff.getIncomingTrucks();
		PriorityQueue<Pair> shovelNeeds = new PriorityQueue<>();
		double[] lastDispatch = new double[numShovels];
		double[] allocated = new double[numShovels];
		for (int i=0; i<numShovels; i++) {
			if (shovelFlow[i] > 0) {
				lastDispatch[i] = simLastDispatchToShovel[i];
				allocated[i] = simAllocatedToShovel[i];
				double needTime = lastDispatch[i] + (allocated[i] - requiredShovelAllocation[i]) / shovelFlow[i];
				shovelNeeds.add(new Pair(i,needTime));
			}
		}
		int[] schedule = new int[numTrucks];
		for (int i=0; i<numTrucks; i++) {
			schedule[i] = -2;
		}
		while (true) {
			Pair neediestShovel = shovelNeeds.poll();
			int sid = neediestShovel.i;
			Pair bestPair = null;
			double bestValue = 1e9;
			int bestCID = -1;
			for (int i=0; i<numCrusherLocs; i++) {
				for (Pair p : incomingTrucks[i]) {
					if (schedule[p.i] < 0) {
						schedule[p.i] = crusherToShovelRoute[i][sid];
						double[] importantTimes = ff.getImportantTimes(p.i,sid,schedule);
						schedule[p.i] = -2;
						// double truckWaitingTime = importantTimes[0];
						double serviceTime = importantTimes[2];
						double shovelWaitingTime = importantTimes[1];
						double lostTons = totalDiggingRate * (serviceTime - minTimeToShovel[sid]) / requiredTrucks + shovelFlow[sid] * 
							shovelWaitingTime;
						// System.out.printf("%f %f\n",truckWaitingTime,serviceTime - minTimeToShovel[sid]);
						if (bestPair == null || lostTons < bestValue) {
							bestPair = p;
							bestValue = lostTons;
							bestCID = i;
						}
					}
				}
			}
			if (bestPair != null) {
				if (bestPair.d < lastDispatch[sid]) {
					return getByGreedy(tid);
				}
				if (bestPair.i == tid) {
					return crusherToShovelRoute[bestCID][sid];
				}
				schedule[bestPair.i] = crusherToShovelRoute[bestCID][sid];
				allocated[sid] = Math.max(0,allocated[sid] - (bestPair.d - lastDispatch[sid]) * shovelFlow[sid]) + 1;
				lastDispatch[sid] = bestPair.d;
				double needTime = lastDispatch[sid] + (allocated[sid] - requiredShovelAllocation[sid]) / shovelFlow[sid];
				shovelNeeds.add(new Pair(sid,needTime));
			}
		}
	}

	/**
	 * Get the next route -
	 * Sorts routes by neediness,
	 * considering the allocation on each route.
	 */
	private int nextRouteByRouteMethod(int tid) {
		PairList[] incomingTrucks = ff.getIncomingTrucks();
		PriorityQueue<Pair> routeNeeds = new PriorityQueue<>();
		double[] lastDispatch = new double[numRoutes];
		double[] allocated = new double[numRoutes];
		for (int i=0; i<numRoutes; i++) {
			if (flow[i][0] > 0) {
				lastDispatch[i] = simLastDispatchOnRoute[i][0];
				allocated[i] = simAllocatedOnRoute[i][0];
				double needTime = lastDispatch[i] + allocated[i] / flow[i][0] - meanRouteTime[i][0];
				routeNeeds.add(new Pair(i,needTime));
			}
		}
		int[] schedule = new int[numTrucks];
		for (int i=0; i<numTrucks; i++) {
			schedule[i] = -2;
		}
		while (true) {
			Pair neediestRoute = routeNeeds.poll();
			int route = neediestRoute.i;
			int cid = routeCrushers[route];
			Pair bestPair = null;
			double bestValue = 1e9;
			for (Pair p : incomingTrucks[cid]) {
				if (schedule[p.i] < 0) {
					schedule[p.i] = route;
					double[] importantTimes = ff.getImportantTimes(p.i,routeShovels[route],schedule);
					schedule[p.i] = -2;
					double truckWaitingTime = importantTimes[0];
					double shovelWaitingTime = importantTimes[1];
					double lostTons = totalDiggingRate * truckWaitingTime / requiredTrucks + flow[route][0] * shovelWaitingTime;
					if (bestPair == null || lostTons < bestValue) {
						bestPair = p;
						bestValue = lostTons;
					}
				}
			}
			if (bestPair != null) {
				if (bestPair.d < lastDispatch[route]) {
					return getByGreedy(tid);
				}
				if (bestPair.i == tid) {
					return route;
				}
				schedule[bestPair.i] = route;
				allocated[route] = Math.max(0,allocated[route] - (bestPair.d - lastDispatch[route]) * flow[route][0]) + 1;
				lastDispatch[route] = bestPair.d;
				double needTime = lastDispatch[route] + allocated[route] / flow[route][0] - meanRouteTime[route][0];
				routeNeeds.add(new Pair(route,needTime));
			}
		}
	}

	/**
	 * Assign the next to the truck greedily to the route which will minimise the lost-tons function.
	 * Used when the algorithm decides a truck should be dispatched out of order,
	 * because it will spend too much time waiting wherever it arrives otherwise.
	 *
	 * @param	tid	the requesting truck index.
	 * @return	the route assignment.
	 */
	private int getByGreedy(int tid) {
		int cid = assignedCrusher[tid];
		int[] schedule = new int[numTrucks];
		for (int i=0; i<numTrucks; i++) {
			schedule[i] = -2;
		}
		int best = -1;
		double bestValue = 1e9;
		for (int i=0; i<numShovels; i++) {
			int route = crusherToShovelRoute[cid][i];
			if (flow[route][0] > 0) {
				schedule[tid] = route;
				double[] importantTimes = ff.getImportantTimes(tid,i,schedule);
				schedule[tid] = -2;
				double truckWaitingTime = importantTimes[0];
				double shovelWaitingTime = importantTimes[1];
				double serviceTime = importantTimes[2];
				double lostTons = (dispatchByShovel ? totalDiggingRate * (serviceTime - minTimeToShovel[i]) / requiredTrucks + 
					shovelFlow[i] * shovelWaitingTime : totalDiggingRate * truckWaitingTime / requiredTrucks + flow[route][0] * 
					shovelWaitingTime);
				if (best < 0 || lostTons < bestValue) {
					best = route;
					bestValue = lostTons;
				}
			}
		}
		return best;
	}

	/**
	 * Get the next assignment from a shovel,
	 * by choosing the route with the smallest ratio of desired allocation to actual allocation.
	 */
	private int nextRouteFromShovel(int tid) {
		int sid = assignedShovel[tid];
		int bestRoute = -1;
		double bestValue = 1e9;
		for (int i=0; i<routesFromShovel[sid].size(); i++) {
			int route = routesFromShovel[sid].get(i);
			if (flow[route][1] > 0) {
				double allocationLoss = (simTime - simLastDispatchOnRoute[route][1]) * flow[route][1];
				double allocated = Math.max(0,simAllocatedOnRoute[route][1] - allocationLoss);
				double desired = meanRouteTime[route][1] * flow[route][1];
				double ratio = allocated / desired;
				if (bestRoute < 0 || ratio < bestValue) {
					bestRoute = route;
					bestValue = ratio;
				}
			}
		}
		return bestRoute;
	}

	public void event(StateChange change) {
		simTime = change.getTime();
		int tid = change.getTruck();
		int route = change.getInformation(0);
		TruckLocation prevLoc = ff.getSimLoc(tid);
		switch (change.getTarget()) {
			case EMPTYING: {
				atCrusher[tid] = true;
				break;
			}
			case FILLING: {
				atCrusher[tid] = false;
				break;
			}
			case APPROACHING_TL_CS:
			case TRAVEL_TO_SHOVEL: {
				int sid = routeShovels[route];
				assignedShovel[tid] = sid;
				if (prevLoc == TruckLocation.WAITING) {
					simAllocatedOnRoute[route][0] = Math.max(0,simAllocatedOnRoute[route][0] - (simTime - simLastDispatchOnRoute[route][0]) * 
						flow[route][0]) + 1;
					simAllocatedToShovel[sid] = Math.max(0,simAllocatedToShovel[sid] - (simTime - simLastDispatchToShovel[sid]) * 
						shovelFlow[sid]) + 1;
					simLastDispatchOnRoute[route][0] = simTime;
					simLastDispatchToShovel[sid] = simTime;
				}
				break;
			}
			case APPROACHING_TL_SS:
			case TRAVEL_TO_CRUSHER: {
				assignedCrusher[tid] = routeCrushers[route];
				if (prevLoc == TruckLocation.LEAVING_SHOVEL) {
					simAllocatedOnRoute[route][1] = Math.max(0,simAllocatedOnRoute[route][1] - (simTime - simLastDispatchOnRoute[route][1]) *
						flow[route][1]) + 1;
					simLastDispatchOnRoute[route][1] = simTime;
				}
				break;
			}
		}
		ff.event(change);
	}

	@Override
	public double lightEvent(int light, TrafficLight change, double simTime, double[] progress) {
		double t = super.lightEvent(light,change,simTime,progress);
		ff.lightEvent(light,change,simTime,simTime + t,progress);
		return t;
	}

	@Override
	public void reset() {
		super.reset();
		ff.reset();
		simTime = 0;
		for (int i=0; i<numTrucks; i++) {
			atCrusher[i] = true;
			assignedCrusher[i] = initialCrushers[i];
			assignedShovel[i] = -1;
		}
		for (int i=0; i<numShovels; i++) {
			simLastDispatchToShovel[i] = 0;
			simAllocatedToShovel[i] = 0;
		}
		for (int i=0; i<numRoutes; i++) {
			for (int j=0; j<2; j++) {
				simLastDispatchOnRoute[i][j] = 0;
				simAllocatedOnRoute[i][j] = 0;
			}
		}
	}

	public int[] getInitialCrushers() {
		return initialCrushers;
	}
	
}