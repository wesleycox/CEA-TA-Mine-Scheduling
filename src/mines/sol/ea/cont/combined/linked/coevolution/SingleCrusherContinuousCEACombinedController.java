package mines.sol.ea.cont.combined.linked.coevolution;

import mines.sol.SingleCrusherContinuousCombinedController;
import mines.ea.fitness.sim.cont.SimFitnessFunction4;
import mines.ea.alg.HeterogeneousCoevolutionaryAlgorithm;
import mines.ea.gene.*;
import mines.ea.chrom.ChromosomePairing;
import java.util.*;

/**
 * Controller that uses cooperative coevolution to evolve dispatch and light schedules at the same time periodically.
 */
public class SingleCrusherContinuousCEACombinedController extends SingleCrusherContinuousCombinedController {

	private int numOneWay;																	//number of one-lane roads.
	private int[] lightScheduleLengths;														//number of floating point genes per traffic light.
	private HeterogeneousCoevolutionaryAlgorithm<ArrayGenotype,FloatingArrayGenotype> cea;	//coevolutionary algorithm.

	/**
	 * Controller constructor.
	 *
	 * @param 	numTrucks				the number of trucks.
	 * @param	numOneWay				the number of one-lane roads.
	 * @param	lightIndexes			the light indexes for roads.
	 * @param	lightScheduleLengths	the number of floating point genes per traffic light.
	 * @param	initialDispatchSchedule	the dispatch schedule to use at shift-start.
	 * @param	ff						the fitness function used to evaluate schedules.
	 * @param	cea						the coevolutionary algorithm to evolve schedules.
	 * @param	updateInterval			the period between schedule updates.
	 */
	public SingleCrusherContinuousCEACombinedController(int numTrucks, int numOneWay, int[] lightIndexes, int[] lightScheduleLengths, 
		int[] initialDispatchSchedule, SimFitnessFunction4 ff, HeterogeneousCoevolutionaryAlgorithm<ArrayGenotype,FloatingArrayGenotype> cea, 
		double updateInterval) {
		super(numTrucks,numOneWay,lightIndexes,initialDispatchSchedule,ff,updateInterval);

		this.numOneWay = numOneWay;
		// this.lightScheduleLengths = lightScheduleLengths;
		if (lightScheduleLengths.length < numOneWay) {
			throw new IllegalArgumentException(String.format("Genes must be assigned for each traffic light: %s length %d",
				Arrays.toString(lightScheduleLengths),lightScheduleLengths.length));
		}
		this.lightScheduleLengths = Arrays.copyOf(lightScheduleLengths,numOneWay);
		this.cea = cea;
	}

	/**
	 * Clear the existing schedule and evolve a new one.
	 */
	protected void updateSchedule() {
		ChromosomePairing<ArrayGenotype,FloatingArrayGenotype> best = cea.run();
		clearDispatchSchedule();
		int[] schedule = best.getFirst().getGenotype().getArray();
		for (int i : schedule) {
			scheduleDispatch(i);
		}
		double[] array = best.getSecond().getGenotype().getArray();
		int look = 0;
		for (int i=0; i<numOneWay; i++) {
			clearLightSchedule(i);
			for (int j=0; j<lightScheduleLengths[i]; j++) {
				scheduleLight(i,array[look] + 0.1);
				look++;
			}
		}
		updated();
	}
	
}