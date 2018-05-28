package mines.ea.fitness.sim.cont.farray;

import mines.ea.fitness.sim.cont.SimFitnessFunction4WFlowDispatch;
import mines.ea.fitness.FitnessFunction;
import mines.ea.gene.FloatingArrayGenotype;
import mines.util.TimeDistribution;
import mines.sim.*;
import mines.sol.lp.FlowCycleLightTimerController;
import java.util.*;

/**
 * Fitness function for fixed-length light schedules.
 * Uses FCS for truck scheduling.
 */
public class LightsWFlowDispatchFitnessFunction extends SimFitnessFunction4WFlowDispatch implements FitnessFunction<FloatingArrayGenotype> {

	private int numSamples;		//number of simulations per fitness evaluation.
	private double lookAhead;	//initial time horizon.
	private int fitnessIndex;	//index of fitness metric, 0 for MTTWT, 1 for MATCT, 2 for MCIT.

	private int[] scheduleLengths;				//number of genes/schedule length per light.
	private double[][] defaultLightSchedule;	//default cyclic light schedule.

	private double[][] lightSchedule;	//light schedule for each light.
	private double endtime;				//endpoint of initial time horizon.

	private int[] lightChanges;		//the number of changes to green for each light in the current simulation.
	private boolean[] scheduled;
	//whether each truck has been dispatched in current simulation but not returned after initial time horizon.
	private int numScheduled;		//number of trucks dispatched in current simulation but not returned after initial time horizon.

	private boolean initialised;	//whether the fitness function has been initialised yet.

	/**
	 * Simulator constructor.
	 * Sets the initial stored state as the default with all trucks at crushers.
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
	 * @param scheduleLengths		an array specifying the number of genes used for each traffic light.
	 * @param defaultLightSchedule	a 2D array specifying the default cyclic light schedule,
	 *								null if entirely greedy.
	 */
	public LightsWFlowDispatchFitnessFunction(int numTrucks, int numShovels, int numCrusherLocs, int[] numCrushers, int numRoads, 
		double[] emptyTimesMean, double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[][] roadTravelTimesMean, 
		double[][] roadTravelTimesSD, double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections,
		int[] routeLengths, int[] routeShovels, int[] routeCrushers, TimeDistribution tgen, double[][] flow, int[] scheduleLengths, 
		double[][] defaultLightSchedule) {
		super(numTrucks,numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,
			roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeShovels,
			routeCrushers,tgen,flow);

		numSamples = 1;
		lookAhead = 60;
		fitnessIndex = 0;

		int numOneWay = getNumOneWay();

		// this.scheduleLengths = scheduleLengths;
		if (scheduleLengths.length < numOneWay) {
			throw new IllegalArgumentException(String.format("Schedule lengths need to be specified for all traffic lights: %s length %d",
				Arrays.toString(scheduleLengths),scheduleLengths.length));
		}
		this.scheduleLengths = Arrays.copyOf(scheduleLengths,numOneWay);

		lightSchedule = new double[numOneWay][];
		for (int i=0; i<numOneWay; i++) {
			lightSchedule[i] = new double[scheduleLengths[i]];
		}

		this.defaultLightSchedule = (defaultLightSchedule == null ? (new double[numOneWay][2]) : defaultLightSchedule);

		lightChanges = new int[numOneWay];
		scheduled = new boolean[numTrucks];

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
	public LightsWFlowDispatchFitnessFunction setNumSamples(int numSamples) {
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
	 * Set the initial time horizon of each simulation.
	 * Can only be used before initialisation.
	 *
	 * @param	lookAhead	the size of the time horizon.
	 * @return	this object.
	 * @throw	IllegalArgumentException	if lookAhead is non-positive.
	 * @throws	IllegalStateException		if already initialised.
	 */
	public LightsWFlowDispatchFitnessFunction setLookAhead(double lookAhead) {
		if (!initialised) {
			if (lookAhead <= 0) {
				throw new IllegalArgumentException(String.format("Positive look ahead required: %d",lookAhead));
			}
			this.lookAhead = lookAhead;
			return this;
		}
		else {
			throw new IllegalStateException("Function already initialised");
		}
	}

	/**
	 * Set the fitness metric:
	 * 0 for minimise total truck waiting time (MTTWT),
	 * 1 for minimise average truck cycle time (MATCT),
	 * 2 for minimise crusher idle time (MCIT).
	 * Note that the output of MTTWT and MCIT are linearly scaled (multiplied by constant factor).
	 * Can only be used before initialisation.
	 *
	 * @param	fitnessIndex	the index of the fitness metric.
	 * @return	this object.
	 * @throws	IllegalStateException	if already initialised.
	 */
	public LightsWFlowDispatchFitnessFunction setFitnessIndex(int fitnessIndex) {
		if (!initialised) {
			this.fitnessIndex = fitnessIndex;
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
	public LightsWFlowDispatchFitnessFunction initialise() {
		if (!initialised) {
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Function already initialised");
		}
	}

	/**
	 * Get the amount of time for a light to remain green,
	 * from the schedule.
	 * If the schedule has been exhausted,
	 * or the initial time horizon passed,
	 * the default cyclic schedule will be used.
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
				int lIndex = getLightIndex(road);
				lightChanges[lIndex]++;
				if (lightChanges[lIndex] <= scheduleLengths[lIndex] && getCurrentTime() <= endtime) {
					return lightSchedule[lIndex][lightChanges[lIndex] - 1];
				}
				else {
					if ((change == TrafficLight.RG) == (FlowCycleLightTimerController.INITIAL_LC % 2 == 0)) {
						return defaultLightSchedule[lIndex][0];
					}
					else {
						return defaultLightSchedule[lIndex][1];
					}
				}
			}
			default: {
				throw new IllegalArgumentException(String.format("Unrecognised light state %s",change));
			}
		}
	}

	/**
	 * Get the next route using FCS.
	 * A termination request will be returned if all trucks dispatched during initial time horizon have unloaded.
	 *
	 * @param	tid	the truck index needing a dispatch.
	 * @return	a route index,
	 *			or -1 when terminating.
	 */
	@Override
	protected int nextRoute(int tid) {
		TruckLocation currentLoc = getTruckLoc(tid);
		if (currentLoc == TruckLocation.EMPTYING || currentLoc == TruckLocation.WAITING) {
			if (getCurrentTime() > endtime) {
				if (scheduled[tid]) {
					scheduled[tid] = false;
					numScheduled--;
				}
				if (numScheduled == 0) {
					return -1;
				}
				return super.nextRoute(tid);
			}
			else {
				if (!scheduled[tid]) {
					numScheduled++;
					scheduled[tid] = true;
				}
				return super.nextRoute(tid);
			}
		}
		else {
			return super.nextRoute(tid);
		}
	}

	/**
	 * Evaluate the fitness of a genotype,
	 * by simulating the schedule it represents.
	 *
	 * @param	genome	an FloatingArrayGenotype schedule.
	 * @return	the average metric over all simulations,
	 *			for the given schedule.
	 * @throws	IllegalStateException	if not yet initialised,
	 *									or the fitness index is invalid.
	 */
	public double getFitness(FloatingArrayGenotype genome) {
		if (initialised) {
			if (!isReady()) {
				ready();
			}
			int numOneWay = getNumOneWay();
			int numTrucks = getNumTrucks();
			double[] array = genome.getArray();
			int look = 0;
			for (int i=0; i<numOneWay; i++) {
				for (int j=0; j<scheduleLengths[i]; j++) {
					lightSchedule[i][j] = array[look] + 0.1;
					look++;
				}
			}
			double total = 0;
			double simTime = getSimTime();
			endtime = simTime + lookAhead;
			for (int i=0; i<numSamples; i++) {
				reReady();
				for (int j=0; j<numOneWay; j++) {
					lightChanges[j] = 0;
				}
				numScheduled = 0;
				for (int j=0; j<numTrucks; j++) {
					scheduled[j] = false;
				}
				double sample;
				switch (fitnessIndex) {
					case 0: {
						simulate(endtime);
						sample = getTruckIdle(endtime);
						break;
					}
					case 1: {
						simulate(1e9);
						sample = getAverageCycleTimes();
						break;
					}
					case 2: {
						simulate(endtime);
						sample = getCrusherIdleOre(endtime);
						break;
					}
					default: {
						throw new IllegalStateException(String.format("Illegal fitness index %d",fitnessIndex));
					}
				}
				total += sample / numSamples;
			}
			return total;
		}
		else {
			throw new IllegalStateException("Function not initialised");
		}
	}

	/**
	 * Get whether the fitness metric is maximising.
	 *
	 * @return	false	for all metrics.
	 */
	public boolean isMaximising() {
		return false;
	}
	
}