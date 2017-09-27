package mines.sim;

import java.util.Arrays;

/**
 * Class representing a change in state after a transition occurs.
 * A StateChange object is given to the controller after each transition,
 * to allow it to maintain information about the current state of the mine,
 * for dispatching purposes.
 * 
 * Provides information about the currently transitioning truck,
 * as well as updates on the progress of the other trucks on their transitions.
 */
public class StateChange {

	private Transition trans;	//the last transition.
	private double[] progress;	//the current completion of each truck of its current task.
	private int[] info;			//additional information.

	/**
	 * State change constructor.
	 *
	 * @param	trans		the most recent transition.
	 * @param	progress	the fractional completion of each truck for its current transition;
	 *						for stationary trucks,
	 *						this is the waiting time.
	 * @param	info		any additional integer information.
	 */
	public StateChange(Transition trans, double[] progress, int... info) {
		this.trans = trans;
		this.progress = Arrays.copyOf(progress,progress.length);
		this.info = Arrays.copyOf(info,info.length);
	}

	/**
	 * Get the time of the transition,
	 * i.e. the current simulation time.
	 *
	 * @return	the transition time.
	 */
	public double getTime() {
		return trans.getTime();
	}

	/**
	 * Get the index of the transitioning truck.
	 *
	 * @return	a truck index.
	 */
	public int getTruck() {
		return trans.getIndex();
	}

	/**
	 * Get the new location of the transitioning truck.
	 *
	 * @return	a state from the TA model.
	 */
	public TruckLocation getTarget() {
		return trans.getTarget();
	}

	/**
	 * Get the progress value for a single truck.
	 *
	 * @param	the truck index.
	 * @return	a double value.
	 */
	public double getProgress(int tid) {
		return progress[tid];
	}

	/**
	 * Access the information variables.
	 *
	 * @param	ind	the information index.
	 * @return	an additional information value.
	 */
	public int getInformation(int ind) {
		return info[ind];
	}
}