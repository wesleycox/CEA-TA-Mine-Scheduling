package mines.sol;

import mines.ea.fitness.sim.cont.SimFitnessFunction4WTimer;
import mines.util.IntQueue;
import mines.sim.*;
import java.util.*;

/**
 * Controller class that updates the dispatch schedule while using cyclic light schedules.
 */
public abstract class SingleCrusherContinuousDispatchWTimerController extends TimerBasedController {

	private int numTrucks;	//number of trucks.

	private double updateInterval;	//period between schedule updates.

	private int[] initialSchedule;	//dispatch schedule at 0 time.

	private SimFitnessFunction4WTimer ff;	//continuous simulator with stored state.

	private double simTime;			//current simulation time.
	private boolean[] atCrusher;	//whether each truck is at the crusher in the current simulation.

	private double lastUpdate;			//time of the last schedule update.
	private IntQueue dispatchSchedule;	//the current truck schedule.
	private int[] scheduledRoute;		//the scheduled route of each truck.

	/**
	 * Controller constructor.
	 *
	 * @param	numTrucks		the number of trucks.
	 * @param	numOneWay		the number of one-lane roads.
	 * @param	lightIndexes	the light indexes for roads.
	 * @param	updateInterval	the period between schedule updates.
	 * @param	lightSchedule	the cyclic light schedule.
	 * @param	initialSchedule	the dispatch schedule to use at shift-start.
	 * @param	ff				a fitness function that can be used to evaluate schedules.
	 * 
	 */
	public SingleCrusherContinuousDispatchWTimerController(int numTrucks, int numOneWay, int[] lightIndexes, double updateInterval, 
		double[][] lightSchedule, int[] initialSchedule, SimFitnessFunction4WTimer ff) {
		super(numOneWay,lightIndexes,lightSchedule);

		this.numTrucks = numTrucks;

		this.updateInterval = updateInterval;

		// this.initialSchedule = initialSchedule;
		this.initialSchedule = Arrays.copyOf(initialSchedule,initialSchedule.length);

		this.ff = ff;

		atCrusher = new boolean[numTrucks];

		dispatchSchedule = new IntQueue();
		scheduledRoute = new int[numTrucks];

		reset();
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
	 * Clear the old dispatch schedule and update it.
	 *
	 * @param	the current simulation time.
	 */
	private void updateSchedule() {
		dispatchSchedule.clear();
		int[] schedule = getDispatchSchedule();
		for (int i : schedule) {
			dispatchSchedule.add(i);
		}
		lastUpdate = simTime;
	}

	/**
	 * Get a new dispatch schedule
	 *
	 * @return	an array of route indexes.
	 */
	protected abstract int[] getDispatchSchedule();

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
		lastUpdate = 0;
		dispatchSchedule.clear();
		for (int i : initialSchedule) {
			dispatchSchedule.add(i);
		}
		for (int i=0; i<numTrucks; i++) {
			atCrusher[i] = true;
			scheduledRoute[i] = -1;
		}
	}
	
}