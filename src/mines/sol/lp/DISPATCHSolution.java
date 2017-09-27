package mines.sol.lp;

import mines.sol.TimerBasedSolution;
import mines.sim.*;
import mines.util.IntList;

/**
 * Class for creating DISPATCH-based controllers.
 */
public class DISPATCHSolution extends TimerBasedSolution {

	private boolean dispatchByShovel;
	private boolean oneWayRestriction;
	private boolean allGreedy;

	/**
	 * Solution constructor.
	 *
	 * @param	params				the simulation parameters.
	 * @param 	dispatchByShovel	whether to dispatch by considering shovels -
	 *								the original algorithm is ambiguous about whether to consider the net allocation to each shovel,
	 *								or to consider each route as distinct -
	 *								leading to two different interpretations,
	 *								(in the single-crusher case the two interpretations are equivalent).
	 * @param	oneWayRestriction	whether to restrict access to one-lane roads to a single direction when calculating haulage rates -
	 *								setting this to true will only produce a valid plan if shovels have multiple outgoing routes
	 *								(false is recommended).
	 * @param	runtime				the shift length,
	 *								can be 0 if allGreedy is true.
	 * @param	allGreedy			whether to use greedy mode instead of cyclic light schedules,
	 *								which will set the light schedule values to 0.
	 */
	public DISPATCHSolution(MineParameters4 params, boolean dispatchByShovel, boolean oneWayRestriction, double runtime,
		boolean allGreedy) {
		super(params,runtime,allGreedy,oneWayRestriction);
		this.dispatchByShovel = dispatchByShovel;
		this.oneWayRestriction = oneWayRestriction;
		this.allGreedy = allGreedy;
	}

	public DISPATCHController getController() {
		double[] scaledEmptyTimesMean = new double[numCrusherLocs];
		for (int i=0; i<numCrusherLocs; i++) {
			scaledEmptyTimesMean[i] = emptyTimesMean[i] / numCrushers[i];
		}
		double totalDiggingRate = 0;
		double requiredTrucks = 0;
		double[] shovelFlow = new double[numShovels];
		double[] requiredShovelAllocation = new double[numShovels];
		double[] minTimeToShovel = new double[numShovels];
		int[][] crusherToShovelRoute = new int[numCrusherLocs][numShovels];
		double[][] meanRouteTime = new double[numRoutes][2];
		IntList[] routesFromShovel = new IntList[numShovels];
		double[] crusherFlow = new double[numCrusherLocs];
		for (int i=0; i<numShovels; i++) {
			minTimeToShovel[i] = 1e9;
			routesFromShovel[i] = new IntList();
			for (int j=0; j<numCrusherLocs; j++) {
				crusherToShovelRoute[j][i] = -1;
			}
		}
		for (int i=0; i<numRoutes; i++) {
			int sid = routeShovels[i];
			int cid = routeCrushers[i];
			totalDiggingRate += flow[i][0];
			double minRouteTime = 0;
			for (int j=0; j<routeLengths[i]; j++) {
				int road = routeRoads[i][j];
				int dir = routeDirections[i][j];
				if (isOneWay[road] && !oneWayRestriction) {
					double[] thisLength = new double[]{roadTravelTimesMean[road][dir],roadTravelTimesMean[road][1 - dir] * fullSlowdown};
					double[] thisFlow = new double[]{roadFlow[road][dir],roadFlow[road][1 - dir]};
					double[] thisSD = new double[]{roadTravelTimesSD[road][dir],roadTravelTimesSD[road][1 - dir] * fullSlowdown};
					double[] travelTimes = SharedTimeEstimator.estimateTravelTimes(thisLength,thisFlow,thisSD);
					for (int k=0; k<2; k++) {
						meanRouteTime[i][k] += travelTimes[k];
					}
				}
				else {
					meanRouteTime[i][0] += roadTravelTimesMean[road][dir];
					meanRouteTime[i][1] += roadTravelTimesMean[road][1 - dir] * fullSlowdown;
				}
				minRouteTime += roadTravelTimesMean[road][dir];
			}
			for (int j=0; j<2; j++) {
				requiredTrucks += meanRouteTime[i][j] * flow[i][j];
			}
			requiredTrucks += flow[i][0] * (scaledEmptyTimesMean[cid] + fillTimesMean[sid]);
			shovelFlow[sid] += flow[i][0];
			requiredShovelAllocation[sid] += flow[i][0] * meanRouteTime[i][0];
			minTimeToShovel[sid] = Math.min(minTimeToShovel[sid],minRouteTime);
			if (crusherToShovelRoute[cid][sid] < 0 || meanRouteTime[i][0] < meanRouteTime[crusherToShovelRoute[cid][sid]][0]) {
				crusherToShovelRoute[cid][sid] = i;
			}
			else {
				throw new IllegalStateException("Multiple routes between destinations is not supported");
			}
			routesFromShovel[sid].add(i);
			crusherFlow[cid] += flow[i][0];
		}
		return new DISPATCHController(numTrucks,numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,
			fillTimesSD,roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,
			routeShovels,routeCrushers,dispatchByShovel,flow,totalDiggingRate,requiredTrucks,shovelFlow,requiredShovelAllocation,
			minTimeToShovel,crusherToShovelRoute,meanRouteTime,routesFromShovel,initialCrushers,numOneWay,lightIndexes,lightSchedule);
	}

	public String getSolutionName() {
		return String.format("DISPATCH-%s-%s%s",(dispatchByShovel ? "s" : "r"),(oneWayRestriction ? "owr" : "scale"),
			(allGreedy ? "" : " w/ timer"));
	}
}