package mines.sol;

import mines.sol.lp.FlowCycleController;
import mines.ea.fitness.sim.cont.SimFitnessFunction4WFlowDispatch;
import mines.util.DoubleQueue;
import mines.sim.*;
import java.util.*;

/**
 * FCS controller with updating light schedules.
 */
public abstract class ContinuousLightsWFlowDispatchController extends FlowCycleController {

	private int numOneWay;		//number of one-lane roads.
	private int[] lightIndexes;	//light indexes for roads.

	private double updateInterval;	//period between updating light schedules.

	private SimFitnessFunction4WFlowDispatch ff;	//simulator with current state.

	private DoubleQueue[] lightSchedule;	//current light schedule.

	private double lastUpdate;	//time light schedule was last updated.

	/**
	 * Controller constructor.
	 *
	 * @param 	numTrucks		the number of trucks.
	 * @param 	numCrusherLocs	the number of crusher locations.
	 * @param 	numShovels		the number of shovels.
	 * @param 	numRoutes		the number of routes.
	 * @param 	routeCrushers	an array of the crusher at the start of each route.
	 * @param 	routeShovels	an array of the shovel at the end of each route.
	 * @param 	numOneWay		the number of one-lane roads.
	 * @param	lightIndexes	an array of light indexes for roads.
	 * @param	flow			a 2D array of the desired haulage rates on each route in both directions.
	 * @param	initialCrushers	an array of the initial locations of trucks.
	 * @param	updateInterval	the period between updating light schedules.
	 * @param	ff				the fitness function which can be used to assess light schedules.
	 */
	public ContinuousLightsWFlowDispatchController(int numTrucks, int numCrusherLocs, int numShovels, int numRoutes, int[] routeCrushers, 
		int[] routeShovels, int numOneWay, int[] lightIndexes, double[][] flow, int[] initialCrushers, double updateInterval, 
		SimFitnessFunction4WFlowDispatch ff) {
		super(numTrucks,numCrusherLocs,numShovels,numRoutes,routeCrushers,routeShovels,flow,initialCrushers);

		this.numOneWay = numOneWay;
		// this.lightIndexes = lightIndexes;
		this.lightIndexes = Arrays.copyOf(lightIndexes,lightIndexes.length);

		this.updateInterval = updateInterval;

		this.ff = ff;

		lightSchedule = new DoubleQueue[numOneWay];
		for (int i=0; i<numOneWay; i++) {
			lightSchedule[i] = new DoubleQueue();
		}
	}

	@Override
	public void event(StateChange change) {
		super.event(change);
		ff.event(change);
	}

	/**
	 * Get the next value from the schedule,
	 * updating it first if enough time has passed since the last update.
	 *
	 * @param	light		the road index of the one-lane road that is about to change state.
	 * @param	change		the new state,
	 *						if green then it has just been yellow in opposite direction,
	 *						if yellow then it has been green, 
	 *						and can remain green if a positive value is returned.
	 * @param	simTime		the current simulation time.
	 * @param	progress	the current progress of trucks as in a StateChange,
	 *						null if unchanged.
	 * @return	if changing to yellow,
	 *			return 0;
	 *			if changing to green,
	 *			the next value from the schedule.
	 * @throws IllegalArgumentException	if change is invalid.
	 */
	@Override
	public double lightEvent(int light, TrafficLight change, double simTime, double[] progress) {
		switch (change) {
			case YR:
			case RY: {
				ff.lightEvent(light,change,simTime,simTime,progress);
				return 0;
			}
			case GR:
			case RG: {
				int lIndex = lightIndexes[light];
				if (simTime > lastUpdate + updateInterval || lightSchedule[lIndex].isEmpty()) {
					updateSchedule(simTime);
				}
				double greenTime = lightSchedule[lIndex].poll();
				ff.lightEvent(light,change,simTime,simTime + greenTime,progress);
				return greenTime;
			}
			default: {
				throw new IllegalArgumentException(String.format("Unrecognised light state %s",change));
			}
		}
	}

	/**
	 * Clear the old schedule and update it.
	 *
	 * @param	simTime	the current simulation time.
	 */
	private void updateSchedule(double simTime) {
		double[][] schedule = getLightSchedule();
		for (int i=0; i<numOneWay; i++) {
			lightSchedule[i].clear();
			for (double d : schedule[i]) {
				lightSchedule[i].add(d);
			}
		}
		lastUpdate = simTime;
	}

	/**
	 * Get the upcoming light schedule.
	 *
	 * @return	a 2D array,
	 *			where each 1D array is a list of values representing a schedule for a traffic light.
	 */
	protected abstract double[][] getLightSchedule();

	@Override
	public void reset() {
		super.reset();
		ff.reset();
		lastUpdate = -updateInterval - 1;
		for (int i=0; i<numOneWay; i++) {
			lightSchedule[i].clear();
		}
	}

}