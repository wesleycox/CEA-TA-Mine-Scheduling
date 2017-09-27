package mines.sol;

import mines.ea.fitness.sim.cont.SimFitnessFunction4;
import mines.ea.gene.*;
import mines.util.*;
import mines.sim.*;
import java.util.*;

/**
 * Controller class that updates both schedules at the same time periodically.
 * Only for use on single-crusher systems.
 */
public abstract class SingleCrusherContinuousCombinedController implements Controller4 {

	private int numTrucks;					//number of trucks.
	private int numOneWay;					//number of one-lane roads.
	private int[] lightIndexes;				//indexes of lights on roads.
	private int[] initialDispatchSchedule;	//dispatch schedule at 0 time.

	private SimFitnessFunction4 ff;	//continuous simulator with stored state.

	private double updateInterval;	//period between schedule updates.

	private double simTime;			//current simulation time.
	private boolean[] atCrusher;	//whether each truck is at the crusher in the current simulation.
	private int[] scheduledRoute;	//the scheduled route of each truck.

	private double lastUpdate;				//time of the last schedule update.
	private DoubleQueue[] lightSchedule;	//the current light schedule.
	private IntQueue dispatchSchedule;		//the current truck schedule.

	/**
	 * Controller constructor.
	 *
	 * @param	numTrucks				the number of trucks.
	 * @param	numOneWay				the number of one-lane roads.
	 * @param	lightIndexes			the light indexes for roads.
	 * @param	initialDispatchSchedule	the dispatch schedule to use at shift-start.
	 * @param	ff						a fitness function that can be used to evaluate schedules.
	 * @param	updateInterval			the period between schedule updates.
	 */
	public SingleCrusherContinuousCombinedController(int numTrucks, int numOneWay, int[] lightIndexes, int[] initialDispatchSchedule, 
		SimFitnessFunction4 ff, double updateInterval) {
		this.numTrucks = numTrucks;
		this.numOneWay = numOneWay;
		// this.lightIndexes = lightIndexes;
		this.lightIndexes = Arrays.copyOf(lightIndexes,lightIndexes.length);
		// this.initialDispatchSchedule = initialDispatchSchedule;
		this.initialDispatchSchedule = Arrays.copyOf(initialDispatchSchedule,initialDispatchSchedule.length);

		this.ff = ff;

		this.updateInterval = updateInterval;

		atCrusher = new boolean[numTrucks];
		scheduledRoute = new int[numTrucks];

		lightSchedule = new DoubleQueue[numOneWay];
		for (int i=0; i<numOneWay; i++) {
			lightSchedule[i] = new DoubleQueue();
		}
		dispatchSchedule = new IntQueue();
	}

	/**
	 * Get the next route from the schedule if at the crusher,
	 * otherwise get the already assigned route,
	 * and updating the schedule first if enough time has passed since the last update.
	 *
	 * @param	tid	the index of the requesting truck.
	 * @return	a route index.
	 */
	public int nextRoute(int tid) {
		if (atCrusher[tid]) {
			if (simTime > lastUpdate + updateInterval || dispatchSchedule.isEmpty()) {
				updateSchedule();
			}
			scheduledRoute[tid] = dispatchSchedule.poll();
		}
		return scheduledRoute[tid];
	}

	/**
	 * Get the next value from the light schedule,
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
	public double lightEvent(int light, TrafficLight change, double simTime, double[] progress) {
		this.simTime = simTime;
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
					updateSchedule();
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
	 * Clear the existing schedule and generate a new one.
	 *
	 * @see clearDispatchSchedule()
	 * @see clearLightSchedule(int)
	 * @see scheduleDispatch(int)
	 * @see scheduleLight(int,double)
	 * @see updated()
	 */
	protected abstract void updateSchedule();

	public void event(StateChange change) {
		ff.event(change);
		simTime = change.getTime();
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

	public void reset() {
		ff.reset();
		simTime = 0;
		for (int i=0; i<numTrucks; i++) {
			atCrusher[i] = true;
			scheduledRoute[i] = -1;
		}
		lastUpdate = 0;
		for (int i=0; i<numOneWay; i++) {
			lightSchedule[i].clear();
		}
		dispatchSchedule.clear();
		for (int i : initialDispatchSchedule) {
			dispatchSchedule.add(i);
		}
	}

	/**
	 * Clear the existing dispatch schedule.
	 */
	protected void clearDispatchSchedule() {
		dispatchSchedule.clear();
	}

	/**
	 * Clear the existing light schedule for one traffic light set.
	 *
	 * @param	ind	a light index.
	 */
	protected void clearLightSchedule(int ind) {
		lightSchedule[ind].clear();
	}

	/**
	 * Append a route to the end of the dispatch schedule.
	 *
	 * @param	dest	a route index.
	 */
	protected void scheduleDispatch(int dest) {
		dispatchSchedule.add(dest);
	}

	/**
	 * Append a time value to the end of a light schedule.
	 *
	 * @param	l		the light index of the traffic light schedule.
	 * @param	gtime	the length of the next green light.
	 */
	protected void scheduleLight(int l, double gtime) {
		lightSchedule[l].add(gtime);
	}

	/**
	 * Declares that the schedule has been updated.
	 * Should be called in updateSchedule()
	 *
	 * @return	the current time.
	 */
	protected double updated() {
		lastUpdate = simTime;
		return simTime;
	}
	
}