package mines.sol.lp;

import mines.sol.Controller4;
import mines.sim.*;
import java.util.*;

/**
 * FCS controller with greedy light rules.
 */
public class FlowCycleController implements Controller4 {

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

	private int numTrucks;			//number of trucks.
	private int numCrusherLocs;		//number of crusher locations.
	private int numShovels;			//number of shovels.
	private int numRoutes;			//number of routes.
	private int[] routeCrushers;	//crusher locations connected to each route.
	private int[] routeShovels;		//shovels connected to each route.

	private double[][] flow;		//desired haulage rates along each route in both directions.
	private int[] initialCrushers;	//initial locations of each truck.

	private boolean[] atCrusher;	//whether each truck is at the crusher.
	private int[] assignedCrusher;	//assigned crusher for each truck.
	private int[] assignedShovel;	//assigned shovel for each truck.

	private PairQueue[] upcomingDests;
	private PairQueue[] upcomingReturns;

	/**
	 * Controller constructor.
	 *
	 * @param 	numTrucks		the number of trucks.
	 * @param 	numCrusherLocs	the number of crusher locations.
	 * @param 	numShovels		the number of shovels.
	 * @param 	numRoutes		the number of routes.
	 * @param 	routeCrushers	an array of the crusher at the start of each route.
	 * @param 	routeShovels	an array of the shovel at the end of each route.
	 * @param	flow			a 2D array specifying the desired haulage rates on each route in both directions.
	 * @param	initialCrushers	an array of initial crusher locations for each truck, 
	 *							based on truck flow through each crusher.
	 */
	public FlowCycleController(int numTrucks, int numCrusherLocs, int numShovels, int numRoutes, int[] routeCrushers, int[] routeShovels, 
		double[][] flow, int[] initialCrushers) {
		this.numTrucks = numTrucks;
		this.numCrusherLocs = numCrusherLocs;
		this.numShovels = numShovels;
		this.numRoutes = numRoutes;
		this.routeCrushers = routeCrushers;
		this.routeShovels = routeShovels;

		this.flow = flow;
		this.initialCrushers = initialCrushers;

		atCrusher = new boolean[numTrucks];
		assignedCrusher = new int[numTrucks];
		assignedShovel = new int[numTrucks];

		upcomingDests = new PairQueue[numCrusherLocs];
		for (int i=0; i<numCrusherLocs; i++) {
			upcomingDests[i] = new PairQueue();
		}
		upcomingReturns = new PairQueue[numShovels];
		for (int i=0; i<numShovels; i++) {
			upcomingReturns[i] = new PairQueue();
		}

		// reset();
	}

	/**
	 * Get the next route by FCS.
	 */
	public int nextRoute(int tid) {
		if (atCrusher[tid]) {
			int cid = assignedCrusher[tid];
			Pair next = upcomingDests[cid].poll();
			upcomingDests[cid].add(new Pair(next.i,next.d + 1.0 / flow[next.i][0]));
			return next.i;
		}
		else {
			int sid = assignedShovel[tid];
			Pair next = upcomingReturns[sid].poll();
			upcomingReturns[sid].add(new Pair(next.i,next.d + 1.0 / flow[next.i][1]));
			return next.i;
		}
	}

	public void event(StateChange change) {
		int tid = change.getTruck();
		TruckLocation loc = change.getTarget();
		int route = change.getInformation(0);
		switch (loc) {
			case EMPTYING: {
				atCrusher[tid] = true;
				break;
			}
			case FILLING: {
				atCrusher[tid] = false;
				break;
			}
			case TRAVEL_TO_SHOVEL: {
				assignedShovel[tid] = routeShovels[route];
				break;
			}
			case TRAVEL_TO_CRUSHER: {
				assignedCrusher[tid] = routeCrushers[route];
				break;
			}
		}
	}

	/**
	 * @return 0	to initiate greedy mode.
	 */
	public double lightEvent(int light, TrafficLight change, double simTime, double[] progress) {
		return 0;
	}

	public void reset() {
		for (int i=0; i<numTrucks; i++) {
			atCrusher[i] = true;
			assignedCrusher[i] = initialCrushers[i];
			assignedShovel[i] = -1;
		}
		for (int i=0; i<numCrusherLocs; i++) {
			upcomingDests[i].clear();
		}
		for (int i=0; i<numShovels; i++) {
			upcomingReturns[i].clear();
		}
		for (int i=0; i<numRoutes; i++) {
			if (flow[i][0] > 0) {
				upcomingDests[routeCrushers[i]].add(new Pair(i,1.0 / flow[i][0]));
			}
			if (flow[i][1] > 0) {
				upcomingReturns[routeShovels[i]].add(new Pair(i,1.0 / flow[i][1]));
			}
		}
	}

	/**
	 * Get the truck flow defining this FCS controller.
	 */
	public double[][] getFlow() {
		// return flow;
		double[][] out = new double[numRoutes][];
		for (int i=0; i<numRoutes; i++) {
			out[i] = Arrays.copyOf(flow[i],2);
		}
		return out;
	}

	public int[] getInitialCrushers() {
		// return initialCrushers;
		return (initialCrushers == null ? null : Arrays.copyOf(initialCrushers,numTrucks));
	}
	
}