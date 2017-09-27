package mines.ea.fitness.sim.cont;

import mines.util.TimeDistribution;
import mines.sim.TrafficLight;
import mines.sol.lp.FlowCycleLightTimerController;

/**
 * Simulator class for a complex road network that can initialise a simulation from a stored state.
 * Uses cyclic timers for light scheduling.
 * Intended to be extended as a fitness function.
 */
public abstract class SimFitnessFunction4WTimer extends SimFitnessFunction4 {

	private double[][] lightSchedule;	//the cyclic light schedule.

	private int[] simLightChanges;	//the number of changes to green for each light in stored state.

	private int[] lightChanges;	//the number of changes to green for each light in the current simulation.

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
	 * @param lightSchedule			a 2D array specifying the cyclic light schedule.
	 */
	public SimFitnessFunction4WTimer(int numTrucks, int numShovels, int numCrusherLocs, int[] numCrushers, int numRoads, 
		double[] emptyTimesMean, double[] emptyTimesSD, double[] fillTimesMean, double[] fillTimesSD, double[][] roadTravelTimesMean, 
		double[][] roadTravelTimesSD, double fullSlowdown, boolean[] isOneWay, int numRoutes, int[][] routeRoads, int[][] routeDirections,
		int[] routeLengths, int[] routeShovels, int[] routeCrushers, TimeDistribution tgen, double[][] lightSchedule) {
		super(numTrucks,numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,
			roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeShovels,
			routeCrushers,tgen);

		int numOneWay = getNumOneWay();

		this.lightSchedule = lightSchedule;

		simLightChanges = new int[numOneWay];

		lightChanges = new int[numOneWay];
	}

	/**
	 * Get the amount of time for a light to remain green,
	 * from the schedule.
	 * May schedule greedy mode initially.
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
				if (lightChanges[lIndex] <= 0) {
					return 0;
				}
				return lightSchedule[lIndex][(lightChanges[lIndex] - 1) % lightSchedule[lIndex].length];
			}
			default: {
				throw new IllegalArgumentException(String.format("Unrecognised light state %s",change));
			}
		}
	}

	/**
	 * Update the stored state for the traffic lights,
	 * including the number of changes to green.
	 * Should be used after each change in traffic lights.
	 * 
	 * @param	light		the road which changed light state.
	 * @param	change		the new TrafficLight value.
	 * @param	time		the time of the change.
	 * @param	schedule	the time of the next change if change is green.
	 * @param	progress	the progress of each truck,
	 *						null if unchanged from previous updates.
	 */
	@Override
	public void lightEvent(int road, TrafficLight change, double time, double schedule, double[] progress) {
		super.lightEvent(road,change,time,schedule,progress);
		switch (change) {
			case GR:
			case RG: {
				int lIndex = getLightIndex(road);
				simLightChanges[lIndex]++;
				break;
			}
		}
	}

	/**
	 * Resets the stored state to the start of a shift.
	 */
	@Override
	public void reset() {
		super.reset();
		int numOneWay = getNumOneWay();
		for (int i=0; i<numOneWay; i++) {
			simLightChanges[i] = FlowCycleLightTimerController.INITIAL_LC;
		}
	}

	/**
	 * Readies the simulator for initialisation based on the stored state.
	 * Should be run once if the stored state has changed.
	 */
	@Override
	protected void reReady() {
		super.reReady();
		int numOneWay = getNumOneWay();
		System.arraycopy(simLightChanges,0,lightChanges,0,numOneWay);
	}

}