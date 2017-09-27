package mines.sol;

import mines.sim.*;

/**
 * Controller class for scheduling trucks and traffic lights.
 */
public interface Controller4 {

	/**
	 * Get the next route for a truck.
	 * Must be valid for the trucks current location.
	 *
	 * @param	tid	a truck index.
	 * @return	a valid route index.
	 */
	public int nextRoute(int tid);

	/**
	 * Update stored state information about the current simulation.
	 * The information variables in the StateChange should hold the assigned route of a transitioning truck first,
	 * and the index on the route second.
	 *
	 * @param	change	a StateChange.
	 */
	public void event(StateChange change);

	/**
	 * Update stored state information about traffic lights in the current simulation.
	 * Gets the amount of time for a light to remain green.
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
	 *			a positive value to remain green,
	 *			or 0 if in greedy mode or otherwise;
	 *			if changing to green,
	 *			a positive value to schedule the next change,
	 *			or 0 to initiate greedy mode.
	 */
	public double lightEvent(int light, TrafficLight change, double simTime, double[] progress);

	/**
	 * Reset the controller to the initial start-of-shift state.
	 */
	public void reset();

	/**
	 * Get the initial locations of trucks.
	 *
	 * @return	an array of crusher indexes,
	 *			or null for the default.
	 */
	public default int[] getInitialCrushers() {
		return null;
	}
	
}