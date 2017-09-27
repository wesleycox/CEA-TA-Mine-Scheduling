package mines.sol.ea.cont.dispatch.fixed;

import mines.sol.SingleCrusherContinuousDispatchWTimerController;
import mines.ea.alg.EvolutionaryAlgorithm;
import mines.ea.gene.ArrayGenotype;
import mines.ea.fitness.sim.cont.SimFitnessFunction4WTimer;

/**
 * Controller that uses an evolutionary algorithm to evolve dispatch schedules periodically.
 * Cyclic light schedules are used.
 */
public class SingleCrusherContinuousEAFLDispatchWTimerController extends SingleCrusherContinuousDispatchWTimerController {

	private EvolutionaryAlgorithm<ArrayGenotype> ea;	//the evolutionary algorithm.

	/**
	 * Controller constructor.
	 *
	 * @param 	numTrucks		the number of trucks.
	 * @param	numOneWay		the number of one-lane roads.
	 * @param	lightIndexes	the light indexes for roads.
	 * @param	updateInterval	the period between schedule updates.
	 * @param	lightSchedule	the cyclic light schedule.
	 * @param	initialSchedule	the dispatch schedule to use at shift-start.
	 * @param	ff				the fitness function used to evaluate schedules.
	 * @param	ea				the evolutionary algorithm to evolve schedules.
	 */
	public SingleCrusherContinuousEAFLDispatchWTimerController(int numTrucks, int numOneWay, int[] lightIndexes, double updateInterval, 
		double[][] lightSchedule, int[] initialSchedule, SimFitnessFunction4WTimer ff, EvolutionaryAlgorithm<ArrayGenotype> ea) {
		super(numTrucks,numOneWay,lightIndexes,updateInterval,lightSchedule,initialSchedule,ff);

		this.ea = ea;
	}

	/**
	 * Evolve a new schedule.
	 *
	 * @return	an array of route indexes.
	 */
	protected int[] getDispatchSchedule() {
		return ea.run().getGenotype().getArray();
	}

}