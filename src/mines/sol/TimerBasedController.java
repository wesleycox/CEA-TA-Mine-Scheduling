package mines.sol;

import mines.sim.TrafficLight;
import mines.sol.lp.FlowCycleLightTimerController;
import java.util.*;

/**
 * Base controller for cyclic light scheduling.
 * Requires extension with a dispatching algorithm.
 */
public abstract class TimerBasedController implements Controller4 {

	private int numOneWay;		//number of one-lane roads.
	private int[] lightIndexes;	//light indexes for roads.

	private double[][] lightSchedule;	//cyclic light schedule.

	private int[] lightChanges;	//number of outputs from the cyclic schedule in the current simulation.

	/**
	 * Controller constructor.
	 *
	 * @param	numOneWay		the number of one-lane roads.
	 * @param	lightIndexes	the light indexes for roads.
	 * @param	lightSchedule	the cyclic light schedule.
	 */
	public TimerBasedController(int numOneWay, int[] lightIndexes, double[][] lightSchedule) {
		this.numOneWay = numOneWay;
		this.lightIndexes = Arrays.copyOf(lightIndexes,lightIndexes.length);

		if (lightSchedule.length != numOneWay) {
			throw new IllegalArgumentException(String.format("Number of one-way roads does not match schedules: %d %s",numOneWay,
				Arrays.deepToString(lightSchedule)));
		}
		this.lightSchedule = new double[numOneWay][];
		for (int i=0; i<numOneWay; i++) {
			this.lightSchedule[i] = Arrays.copyOf(lightSchedule[i],lightSchedule[i].length);
		}

		lightChanges = new int[numOneWay];
	}

	/**
	 * Get the next value from the cyclic light schedule.
	 * Initially, greedy mode will be used.
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
	 *			the next value from the schedule,
	 *			unless still using greedy mode.
	 * @throws IllegalArgumentException	if change is invalid.
	 */
	public double lightEvent(int light, TrafficLight change, double simTime, double[] progress) {
		switch (change) {
			case YR:
			case RY: {
				return 0;
			}
			case GR:
			case RG: {
				int lIndex = lightIndexes[light];
				lightChanges[lIndex]++;
				if (lightChanges[lIndex] <= 0) {
					return 0;
				}
				else {
					return lightSchedule[lIndex][(lightChanges[lIndex] - 1) % lightSchedule[lIndex].length];
				}
			}
			default: {
				throw new IllegalArgumentException(String.format("Unrecognised light state %s",change));
			}
		}
	}

	public void reset() {
		for (int i=0; i<numOneWay; i++) {
			lightChanges[i] = FlowCycleLightTimerController.INITIAL_LC;
		}
	}
	
}