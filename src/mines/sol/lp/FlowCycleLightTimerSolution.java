package mines.sol.lp;

import mines.sol.TimerBasedSolution;
import mines.sim.MineParameters4;

/**
 * Solution class for FCS controllers.
 */
public class FlowCycleLightTimerSolution extends TimerBasedSolution {

	private boolean allGreedy;	//whether to use greedy rules for lights.

	/**
	 * Solution constructor.
	 *
	 * @param	params				the simulation parameters.
	 * @param	runtime				the shift length,
	 *								can be 0 if allGreedy is true.
	 * @param	allGreedy			whether to use greedy mode instead of cyclic light schedules,
	 *								which will set the light schedule values to 0.
	 */
	public FlowCycleLightTimerSolution(MineParameters4 params, double runtime, boolean allGreedy) {
		super(params,runtime,allGreedy);
		this.allGreedy = allGreedy;
	}

	public FlowCycleLightTimerController getController() {
		return new FlowCycleLightTimerController(numTrucks,numCrusherLocs,numShovels,numRoutes,routeCrushers,routeShovels,numOneWay,
			lightIndexes,flow,initialCrushers,lightSchedule);
	}

	public String getSolutionName() {
		return String.format("Cyclic by flow%s",(allGreedy ? "" : " w/ timer"));
	}

}