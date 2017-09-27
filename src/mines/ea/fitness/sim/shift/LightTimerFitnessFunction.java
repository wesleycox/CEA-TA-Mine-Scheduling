package mines.ea.fitness.sim.shift;

import mines.ea.fitness.FitnessFunction;
import mines.ea.gene.FloatingArrayGenotype;
import mines.util.TimeDistribution;
import mines.sim.*;
import mines.sol.lp.FlowCycleLightTimerController;
import java.util.*;

/**
 * Fitness function for cyclic light schedules over entire shifts.
 * Uses FCS for truck scheduling.
 */
public class LightTimerFitnessFunction extends SimpleSimFitnessFunction4 implements FitnessFunction<FloatingArrayGenotype> {

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

	private int cycleLength;	//length of cyclic schedules

	private int numSamples;	//number of simulations per fitness evaluation.
	private double runtime;	//shift length.

	// private int numOneWay;		//number of one-lane roads
	// private int[] lightIndexes;	//indexes of light associated with one-lane roads.

	private double[][] lightSchedule;	//cyclic light schedule

	private PairQueue[] upcomingDests;
	//pairs in FCS specifying upcoming routes outgoing from each crusher location in the current simulation.
	private PairQueue[] upcomingReturns;	//pairs in FCS specifying upcoming routes outgoing from each shovel in the current simulation.
	private int[] lightChanges;				//the number of changes to green for each light in the current simulation.

	private boolean initialised;	//whether the fitness function has been initialised yet.

	/**
	 * Simulator constructor.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
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
	 * @param cycleLength			the number of genes per traffic light.
	 * @param initialCrushers		an array of the initial locations of trucks.
	 */
	public LightTimerFitnessFunction(int numTrucks, int numShovels, int numCrusherLocs, int[] numCrushers, int numRoads, 
		double[] emptyTimesMean, double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[][] roadTravelTimesMean, 
		double[][] roadTravelTimesSD, double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections,
		int[] routeLengths, int[] routeShovels, int[] routeCrushers, TimeDistribution tgen, double[][] flow, int cycleLength, 
		int[] initialCrushers) {
		super(numTrucks,numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,
			roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeShovels,
			routeCrushers,tgen);

		this.flow = flow;

		this.cycleLength = cycleLength;

		numSamples = 1;
		runtime = 500;

		// numOneWay = 0;
		// lightIndexes = new int[numRoads];
		// for (int i=0; i<numRoads; i++) {
		// 	if (isOneWay[i]) {
		// 		lightIndexes[i] = numOneWay;
		// 		numOneWay++;
		// 	}
		// 	else {
		// 		lightIndexes[i] = -1;
		// 	}
		// }

		int numOneWay = getNumOneWay();
		lightSchedule = new double[numOneWay][cycleLength];

		upcomingDests = new PairQueue[numCrusherLocs];
		for (int i=0; i<numCrusherLocs; i++) {
			upcomingDests[i] = new PairQueue();
		}
		upcomingReturns = new PairQueue[numShovels];
		for (int i=0; i<numShovels; i++) {
			upcomingReturns[i] = new PairQueue();
		}
		lightChanges = new int[numOneWay];

		setInitialCrushers(initialCrushers);

		initialised = false;
	}

	/**
	 * Set the number of simulations to run per fitness evaluation.
	 * Can only be used before initialisation.
	 *
	 * @param	numSamples	the number of simulations.
	 * @return	this object.
	 * @throw	IllegalArgumentException	if numSamples is non-positive.
	 * @throws	IllegalStateException		if already initialised.
	 */
	public LightTimerFitnessFunction setNumSamples(int numSamples) {
		if (!initialised) {
			if (numSamples <= 0) {
				throw new IllegalArgumentException(String.format("Positive number of samples required: %d",numSamples));
			}
			this.numSamples = numSamples;
			return this;
		}
		else {
			throw new IllegalStateException("Function already initialised");
		}
	}

	/**
	 * Set the shift length.
	 * Can only be used before initialisation.
	 *
	 * @param	runtime	the shift length.
	 * @return	this object.
	 * @throw	IllegalArgumentException	if runtime is non-positive.
	 * @throws	IllegalStateException		if already initialised.
	 */
	public LightTimerFitnessFunction setRuntime(double runtime) {
		if (!initialised) {
			if (runtime <= 0) {
				throw new IllegalArgumentException(String.format("Positive runtime required: %f",runtime));
			}
			this.runtime = runtime;
			return this;
		}
		else {
			throw new IllegalStateException("Function already initialised");
		}
	}

	/**
	 * Initialise this object for use.
	 * Can only be used once.
	 *
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public LightTimerFitnessFunction initialise() {
		if (!initialised) {
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Function already initialised");
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
		TruckLocation loc = getTruckLoc(tid);
		if (loc == TruckLocation.WAITING || loc == TruckLocation.EMPTYING) {
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
	 * Get the amount of time for a light to remain green,
	 * from the schedule.
	 *
	 * @param	road	the one-lane road changing light state.
	 * @param	change	the new light state.
	 * @return	0 if change is to yellow,
	 *			the next schedule item otherwise.
	 */
	protected double getNextLight(int road, TrafficLight change) {
		switch (change) {
			case YR:
			case RY: {
				return 0;
			}
			case GR:
			case RG: {
				// int lIndex = lightIndexes[road];
				int lIndex = getLightIndex(road);
				lightChanges[lIndex]++;
				if (lightChanges[lIndex] <= 0) {
					return 0;
				}
				return lightSchedule[lIndex][(lightChanges[lIndex] - 1) % cycleLength];
			}
			default: {
				throw new IllegalArgumentException(String.format("Unrecognised light state %s",change));
			}
		}
	}

	/**
	 * Evaluate the fitness of a genotype,
	 * by simulating the schedule it represents.
	 *
	 * @param	genome	an FloatingArrayGenotype schedule.
	 * @return	the average production over all simulations,
	 *			for the given schedule.
	 * @throws	IllegalStateException	if not yet initialised,
	 *									or the fitness index is invalid.
	 */
	public double getFitness(FloatingArrayGenotype genome) {
		if (initialised) {
			int numCrusherLocs = getNumCrusherLocs();
			int numShovels = getNumShovels();
			int numRoutes = getNumRoutes();
			int numOneWay = getNumOneWay();
			double[] array = genome.getArray();
			for (int i=0; i<numOneWay; i++) {
				for (int j=0; j<cycleLength; j++) {
					lightSchedule[i][j] = array[i * cycleLength + j] + 0.1;
				}
			}
			double total = 0;
			for (int i=0; i<numSamples; i++) {
				reReady();
				for (int j=0; j<numOneWay; j++) {
					lightChanges[j] = FlowCycleLightTimerController.INITIAL_LC;
				}
				for (int j=0; j<numCrusherLocs; j++) {
					upcomingDests[j].clear();
				}
				for (int j=0; j<numShovels; j++) {
					upcomingReturns[j].clear();
				}
				for (int j=0; j<numRoutes; j++) {
					if (flow[j][0] > 0) {
						upcomingDests[getRouteCrusher(j)].add(new Pair(j,1.0 / flow[j][0]));
					}
					if (flow[j][1] > 0) {
						upcomingReturns[getRouteShovel(j)].add(new Pair(j,1.0 / flow[j][1]));
					}
				}
				simulate(runtime);
				double sample = getNumEmpties();
				total += sample / numSamples;
			}
			return total;
		}
		else {
			throw new IllegalStateException("Function not initialised");
		}
	}

	public boolean isMaximising() {
		return true;
	}
}