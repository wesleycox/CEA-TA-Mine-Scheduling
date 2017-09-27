package mines.sol.greedy;

import mines.sol.*;
import mines.util.TimeDistribution;
import mines.sim.MineParameters4;

/**
 * Solution class for greedy heuristic controllers.
 */
public class SingleCrusherGreedySolution extends TimerBasedSolution {

	private TimeDistribution tgen;	//the distribution used by the simulator.
	private int numSamples;			//the number of forward simulations per heuristic metric estimate.
	private HeuristicKind hKind;	//the heuristic metric.
	private boolean allGreedy;		//whether to use greedy rules for light schedules.

	/**
	 * Solution constructor.
	 *
	 * @param	params				the simulation parameters.
	 * @param	TimeDistribution	the distribution used by the simulator.
	 * @param	numSamples			the number of forward simulations per heuristic metric estimate.
	 * @param	hKind				the heuristic metric.
	 * @param	allGreedy			whether to use greedy rules for light schedules,
	 *								false for cyclic light scheduling.
	 */
	public SingleCrusherGreedySolution(MineParameters4 params, TimeDistribution tgen, int numSamples, HeuristicKind hKind, double runtime, 
		boolean allGreedy) {
		super(params,runtime,allGreedy);
		if (numCrusherLocs != 1) {
			throw new IllegalArgumentException(String.format("Illegal number of crusher locations %d",numCrusherLocs));
		}

		this.tgen = tgen;
		this.numSamples = numSamples;
		this.hKind = hKind;
		this.allGreedy = allGreedy;
	}

	public TimerBasedController getController() {
		switch (hKind) {
			case MTST:
			case MTSWT:
			case MSWT:
			case MTTWT1: {
				return new SingleCrusherGreedyController(numTrucks,numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,
					emptyTimesSD,fillTimesMean,fillTimesSD,roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,
					routeDirections,routeLengths,routeShovels,routeCrushers,tgen,numSamples,hKind,numOneWay,lightIndexes,lightSchedule);
			}
			case MTRT:
			case MTCT:
			case MTTWT2: {
				return new SingleCrusherTwoStageGreedyController(numTrucks,numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,
					emptyTimesSD,fillTimesMean,fillTimesSD,roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,
					routeDirections,routeLengths,routeShovels,routeCrushers,tgen,numSamples,hKind,numOneWay,lightIndexes,lightSchedule);
			}
			default: {
				throw new IllegalStateException(String.format("Unsupported heuristic kind %s",hKind));
			}
		}
	}

	public String getSolutionName() {
		return String.format("Greedy-%s (%d sample(s))%s",hKind,numSamples,(allGreedy ? "" : " w/ timer"));
	}
}