package mines.ea.fitness.sim.cont;

import mines.util.TimeDistribution;
import mines.sim.*;
import java.util.*;

/**
 * Simulator class for a complex road network that can initialise a simulation from a stored state.
 * Uses FCS for truck dispatching.
 * Intended to be extended as a fitness function.
 */
public abstract class SimFitnessFunction4WFlowDispatch extends SimFitnessFunction4 {

	private static class Pair implements Comparable<Pair> {

		int i;
		double d;

		public Pair(int i, double d) {
			this.i = i;
			this.d = d;
		}

		public int compareTo(Pair other) {
			int dd = Double.compare(this.d,other.d);
			return (dd == 0 ? this.i - other.i : dd);
		}
	}

	private static class PairQueue extends PriorityQueue<Pair> {}

	private double[][] flow;	//desired haulage rates along each route in both directions.

	private PairQueue[] simUpcomingDests;	//pairs in FCS specifying upcoming routes outgoing from each crusher location in stored state.
	private PairQueue[] simUpcomingReturns;	//pairs in FCS specifying upcoming routes outgoing from each shovel in stored state.

	private PairQueue[] upcomingDests;
	//pairs in FCS specifying upcoming routes outgoing from each crusher location in the current simulation.
	private PairQueue[] upcomingReturns;	//pairs in FCS specifying upcoming routes outgoing from each shovel in the current simulation.

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
	 * @param flow					a 2D array specifying the haulage rates along each route in both directions.
	 */
	public SimFitnessFunction4WFlowDispatch(int numTrucks, int numShovels, int numCrusherLocs, int[] numCrushers, int numRoads, 
		double[] emptyTimesMean, double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[][] roadTravelTimesMean, 
		double[][] roadTravelTimesSD, double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections,
		int[] routeLengths, int[] routeShovels, int[] routeCrushers, TimeDistribution tgen, double[][] flow) {
		super(numTrucks,numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,
			roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeShovels,
			routeCrushers,tgen);

		this.flow = flow;

		simUpcomingDests = new PairQueue[numCrusherLocs];
		for (int i=0; i<numCrusherLocs; i++) {
			simUpcomingDests[i] = new PairQueue();
		}
		simUpcomingReturns = new PairQueue[numShovels];
		for (int i=0; i<numShovels; i++) {
			simUpcomingReturns[i] = new PairQueue();
		}

		upcomingDests = new PairQueue[numCrusherLocs];
		for (int i=0; i<numCrusherLocs; i++) {
			upcomingDests[i] = new PairQueue();
		}
		upcomingReturns = new PairQueue[numShovels];
		for (int i=0; i<numShovels; i++) {
			upcomingReturns[i] = new PairQueue();
		}
	}

	/**
	 * Get the next route for the given truck,
	 * using FCS.
	 * 
	 * @param	tid	the index of the truck requiring routing.
	 * @return	a route index.
	 */
	protected int nextRoute(int tid) {
		TruckLocation currentLoc = getTruckLoc(tid);
		if (currentLoc == TruckLocation.EMPTYING || currentLoc == TruckLocation.WAITING) {
			int cid = getAssignedCrusher(tid);
			Pair next = upcomingDests[cid].poll();
			upcomingDests[cid].add(new Pair(next.i,next.d + 1.0 / flow[next.i][0]));
			return next.i;
		}
		else {
			int sid = getAssignedShovel(tid);
			Pair next = upcomingReturns[sid].poll();
			upcomingReturns[sid].add(new Pair(next.i,next.d + 1.0 / flow[next.i][1]));
			return next.i;
		}
	}

	/**
	 * Resets the stored state to the start of a shift.
	 */
	@Override
	public void reset() {
		super.reset();
		int numCrusherLocs = getNumCrusherLocs();
		int numShovels = getNumShovels();
		int numRoutes = getNumRoutes();
		for (int i=0; i<numCrusherLocs; i++) {
			simUpcomingDests[i].clear();
		}
		for (int i=0; i<numShovels; i++) {
			simUpcomingReturns[i].clear();
		}
		for (int i=0; i<numRoutes; i++) {
			if (flow[i][0] > 0) {
				simUpcomingDests[getRouteCrusher(i)].add(new Pair(i,1.0 / flow[i][0]));
			}
			if (flow[i][1] > 0) {
				simUpcomingReturns[getRouteShovel(i)].add(new Pair(i,1.0 / flow[i][1]));
			}
		}
	}

	/**
	 * Update the stored state,
	 * including advancing the stored FCS schedule after a dispatch.
	 * Should be used after each transition.
	 * 
	 * @param	change	a StateChange specifying the transition that occurred.
	 */
	@Override
	public void event(StateChange change) {
		int tid = change.getTruck();
		TruckLocation target = change.getTarget();
		TruckLocation simLoc = getSimLoc(tid);
		if (simLoc == TruckLocation.WAITING && (target == TruckLocation.TRAVEL_TO_SHOVEL || target == TruckLocation.APPROACHING_TL_CS)) {
			int cid = getSimAssignedCrusher(tid);
			Pair next = simUpcomingDests[cid].poll();
			simUpcomingDests[cid].add(new Pair(next.i,next.d + 1.0 / flow[next.i][0]));
			if (next.i != change.getInformation(0)) {
				throw new IllegalStateException(String.format("Incorrect use of fitness function. Expected route was %d but received %d",
					next.i,change.getInformation(0)));
			}
		}
		else if (simLoc == TruckLocation.LEAVING_SHOVEL && (target == TruckLocation.TRAVEL_TO_CRUSHER || 
			target == TruckLocation.APPROACHING_TL_SS)) {
			int sid = getSimAssignedShovel(tid);
			Pair next = simUpcomingReturns[sid].poll();
			simUpcomingReturns[sid].add(new Pair(next.i,next.d + 1.0 / flow[next.i][1]));
		}
		super.event(change);
	}

	/**
	 * Readies the simulator for initialisation based on the stored state.
	 * Should be run once if the stored state has changed.
	 */
	@Override
	protected void reReady() {
		super.reReady();
		int numCrusherLocs = getNumCrusherLocs();
		int numShovels = getNumShovels();
		for (int i=0; i<numCrusherLocs; i++) {
			upcomingDests[i].clear();
			upcomingDests[i].addAll(simUpcomingDests[i]);
		}
		for (int i=0; i<numShovels; i++) {
			upcomingReturns[i].clear();
			upcomingReturns[i].addAll(simUpcomingReturns[i]);
		}
	}
}