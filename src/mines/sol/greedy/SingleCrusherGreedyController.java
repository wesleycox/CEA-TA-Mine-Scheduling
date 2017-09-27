package mines.sol.greedy;

import mines.sol.TimerBasedController;
import mines.ea.fitness.sim.cont.SimFitnessFunction4WTimer;
import mines.util.TimeDistribution;
import mines.sim.*;
import java.util.*;

/**
 * Controller for the greedy heuristics MTST, MTSWT, MSWT, and MTTWT1.
 * Heuristic values are estimated by forward simulation.
 * Cyclic light schedules are used.
 */
public class SingleCrusherGreedyController extends TimerBasedController {

	private static final double INFINITY = 1e9;

	/**
	 * The stored-state simulator for estimating heuristic values.
	 */
	private static class GreedySimulator extends SimFitnessFunction4WTimer {

		private int currentDispatch;	//the truck to greedily optimise for - the observed truck.
		private boolean dispatched;
		private int[] assignment;		//the schedule to simulate.

		public GreedySimulator(int numTrucks, int numShovels, int numCrusherLocs, int[] numCrushers, int numRoads, double[] emptyTimesMean, 
			double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[][] roadTravelTimesMean, 
			double[][] roadTravelTimesSD, double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections,
			int[] routeLengths, int[] routeShovels, int[] routeCrushers, TimeDistribution tgen, double[][] lightSchedule) {
			super(numTrucks,numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,
				roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeShovels,
				routeCrushers,tgen,lightSchedule);
		}

		/**
		 * Get the next assignment from a fixed-truck-assignment schedule,
		 * or a termination request if the observed truck is leaving the shovel.
		 */
		protected int nextRoute(int tid) {
			if (tid == currentDispatch) {
				if (dispatched) {
					return -1;
				}
				dispatched = true;
			}
			return assignment[tid];
		}

		/**
		 * Estimate a heuristic value for the observed truck with a given schedule.
		 *
		 * @param	currentDispatch	the observed truck.
		 * @param	assignment		the FTA schedule.
		 * @param	numSamples		the number of forward simulations to run.
		 * @param	hKind			the heuristic metric.
		 */
		public double getValue(int currentDispatch, int[] assignment, int numSamples, HeuristicKind hKind) {
			this.currentDispatch = currentDispatch;
			this.assignment = assignment;
			if (!isReady()) {
				ready();
			}
			double total = 0;
			for (int i=0; i<numSamples; i++) {
				reReady();
				dispatched = false;
				simulate(INFINITY);
				switch (hKind) {
					case MTST: {
						total += getLastServiceStart(currentDispatch) - getSimTime();
						break;
					}
					case MTSWT: {
						total += getServiceWaitingTime(currentDispatch);
						break;
					}
					case MSWT: {
						total += getServiceAvailableTime(currentDispatch);
						break;
					}
					case MTTWT1: {
						total += getTotalWaitingTime(currentDispatch);
						break;
					}
					default: {
						throw new IllegalArgumentException(String.format("Unsupported heuristic kind %s",hKind));
					}
				}
			}
			return total / numSamples;
		}

	}

	private int numTrucks;	//the number of trucks.
	private int numRoutes;	//the number of routes.

	private int numSamples;			//the number of forward simulations per heuristic metric estimate.
	private HeuristicKind hKind;	//the heuristic metric.

	private GreedySimulator ff;	//the simulator for heuristic values.

	private boolean[] atCrusher;	//whether each truck is at the crusher.
	private int[] currentRoute;		//the current assignment of each truck.

	/**
	 * Controller constructor.
	 *
	 * @param 	numTrucks			the number of trucks.
	 * @param 	numShovels			the number of shovels.
	 * @param 	numCrusherLocs		the number of crusher locations.
	 * @param 	numCrushers			an array of the number of crushers at each location.
	 * @param 	numRoads			the number of roads.
	 * @param 	emptyTimesMean		an array of average emptying times for each crusher.
	 * @param 	emptyTimesSD		an array of standard deviations of emptying times for each crusher.
	 * @param 	fillTimesMean		an array of average filling times for each shovel.
	 * @param 	fillTimesSD			an array of standard deviations of filling times for each shovel.
	 * @param 	roadTravelTimesMean	a 2D array of average travelling times on each road in both directions.
	 * @param 	roadTravelTimesSD	a 2D array of standard deviations of travelling time on each road in both directions.
	 * @param 	fullSlowdown		the travel time increase for travelling full.
	 * @param 	isOneWay			an array specifying whether each road is one-lane.
	 * @param 	numRoutes			the number of routes.
	 * @param 	routeRoads			a 2D array listing the roads comprising each route.
	 * @param 	routeDirections		a 2D array listing the directions travelled on each road in each route.
	 * @param 	routeLengths		an array of the number of roads in each route.
	 * @param 	routeShovels		an array of the shovel at the end of each route.
	 * @param 	routeCrushers		an array of the crusher at the start of each route.
	 * @param 	tgen				a TimeDistrubtion specifying the distribution used for generating all stochastic values.
	 * @param	numSamples			the number of forward simulations per heuristic metric estimate.
	 * @param	numOneWay			the number of one-lane roads.
	 * @param	lightIndexes		an array of light indexes for each road.
	 * @param 	lightSchedule		a 2D array specifying the cyclic light schedule.
	 */
	public SingleCrusherGreedyController(int numTrucks, int numShovels, int numCrusherLocs, int[] numCrushers, int numRoads, 
		double[] emptyTimesMean, double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[][] roadTravelTimesMean, 
		double[][] roadTravelTimesSD, double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections,
		int[] routeLengths, int[] routeShovels, int[] routeCrushers, TimeDistribution tgen, int numSamples, HeuristicKind hKind, 
		int numOneWay, int[] lightIndexes, double[][] lightSchedule) {
		super(numOneWay,lightIndexes,lightSchedule);
		this.numTrucks = numTrucks;
		this.numRoutes = numRoutes;

		this.numSamples = numSamples;
		this.hKind = hKind;

		ff = new GreedySimulator(numTrucks,numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,
			fillTimesSD,roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,
			routeShovels,routeCrushers,tgen,lightSchedule);

		atCrusher = new boolean[numTrucks];
		currentRoute = new int[numTrucks];

		// reset();
	}

	/**
	 * If a truck is at the crusher,
	 * run forward simulations for each assignment, 
	 * to greedily optimise a metric for the requesting truck.
	 * Otherwise get the current assignment
	 *
	 * @param	tid	the requesting truck index.
	 * @return	a valid route index.
	 */
	public int nextRoute(int tid) {
		if (atCrusher[tid]) {
			int[] assignment = Arrays.copyOf(currentRoute,numTrucks);
			double[] values = new double[numRoutes];
			for (int i=0; i<numRoutes; i++) {
				assignment[tid] = i;
				values[i] = ff.getValue(tid,assignment,numSamples,hKind);
			}
			currentRoute[tid] = minIndex(values);
		}
		return currentRoute[tid];
	}

	/**
	 * Get the index of the minimum value in an array.
	 *
	 * @param	array an array of doubles.
	 * @param	the index of the minimum value.
	 */
	private int minIndex(double[] array) {
		int min = 0;
		for (int i=1; i<array.length; i++) {
			if (array[i] < array[min]) {
				min = i;
			}
		}
		return min;
	}

	public void event(StateChange change) {
		ff.event(change);
		int tid = change.getTruck();
		TruckLocation loc = change.getTarget();
		switch (loc) {
			case EMPTYING: {
				atCrusher[tid] = true;
				break;
			}
			case FILLING: {
				atCrusher[tid] = false;
				break;
			}
		}
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
		for (int i=0; i<numTrucks; i++) {
			atCrusher[i] = true;
			currentRoute[i] = -2;
		}
	}
	
}