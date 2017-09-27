package mines.sol.ea.cont.lights;

import mines.sol.ContinuousLightsWFlowDispatchController;
import mines.ea.alg.EvolutionaryAlgorithm;
import mines.ea.gene.FloatingArrayGenotype;
import mines.ea.fitness.sim.cont.SimFitnessFunction4WFlowDispatch;

/**
 * Controller that uses an evolutionary algorithm to evolve light schedules periodically.
 * FCS is used for truck scheduling.
 */
public class ContinuousEALightsWFlowDispatchController extends ContinuousLightsWFlowDispatchController {

	private int numOneWay;	//number of one-lane roads.

	private int[] scheduleLengths;	//number of floating point genes per traffic light.

	private EvolutionaryAlgorithm<FloatingArrayGenotype> ea;	//the evolutionary algorithm.

	/**
	 * Controller constructor.
	 *
	 * @param 	numTrucks		the number of trucks.
	 * @param 	numCrusherLocs	the number of crusher locations.
	 * @param 	numShovels		the number of shovels.
	 * @param 	numRoutes		the number of routes.
	 * @param 	routeCrushers	an array of the crusher at the start of each route.
	 * @param 	routeShovels	an array of the shovel at the end of each route.
	 * @param	numOneWay		the number of one-lane roads.
	 * @param	lightIndexes	the light indexes for roads.
	 * @param	flow			a 2D array of the desired haulage routes on each route in each direction.
	 * @param	initialCrushers	an array of the initial location of each truck.
	 * @param	updateInterval	the period between schedule updates.
	 * @param	scheduleLengths	the number of genes per traffic light.
	 * @param	ff				the fitness function used to evaluate schedules.
	 * @param	ea				the evolutionary algorithm to evolve schedules.
	 */
	public ContinuousEALightsWFlowDispatchController(int numTrucks, int numCrusherLocs, int numShovels, int numRoutes, int[] routeCrushers, 
		int[] routeShovels, int numOneWay, int[] lightIndexes, double[][] flow, int[] initialCrushers, double updateInterval,
		int[] scheduleLengths, SimFitnessFunction4WFlowDispatch ff, EvolutionaryAlgorithm<FloatingArrayGenotype> ea) {
		super(numTrucks,numCrusherLocs,numShovels,numRoutes,routeCrushers,routeShovels,numOneWay,lightIndexes,flow,initialCrushers,
			updateInterval,ff);

		this.numOneWay = numOneWay;

		this.scheduleLengths = scheduleLengths;

		this.ea = ea;
	}

	/**
	 * Evolve a new schedule.
	 *
	 * @return	a 2D array,
	 *			where each 1D array is a list of values representing a schedule for a traffic light.
	 */
	protected double[][] getLightSchedule() {
		double[] array = ea.run().getGenotype().getArray();
		double[][] schedule = new double[numOneWay][];
		int look = 0;
		for (int i=0; i<numOneWay; i++) {
			schedule[i] = new double[scheduleLengths[i]];
			for (int j=0; j<scheduleLengths[i]; j++) {
				schedule[i][j] = array[look] + 0.1;
				look++;
			}
		}
		return schedule;
	}
}