package mines.sol;

import mines.util.*;
import mines.sim.*;
import mines.ea.op.gene.CrossoverKind;
import mines.lp.LPFlowConstructor;
import mines.ea.gene.build.GenotypeBuilder;
import mines.ea.gene.FloatingArrayGenotype;
import mines.ea.gene.build.farray.UnboundedFloatingArrayBuilder;
import mines.ea.fitness.FitnessFunction;
import mines.ea.fitness.sim.shift.LightTimerFitnessFunction;
import mines.ea.op.selection.*;
import mines.ea.chrom.RollingChromosome;
import mines.ea.op.gene.GeneticOperator;
import mines.ea.op.gene.farray.UnboundedFloatingArrayOperator;
import mines.ea.alg.RollingEvolutionaryAlgorithm;
import mines.system.Debugger;
import java.util.*;

/**
 * Solution class containing additional variables relevant to light scheduling and flow-based dispatch scheduling.
 */
public abstract class TimerBasedSolution extends Solution4 {

	private static class Pair implements Comparable<Pair> {

		int i;
		double d;

		public Pair(int i, double d) {
			this.i = i;
			this.d = d;
		}

		public int compareTo(Pair other) {
			int dd = Double.compare(this.d,other.d);
			return (dd == 0 ? this.i - other.i : dd);
		}
	}

	private static class PairQueue extends PriorityQueue<Pair> {}

	protected double runtime;	//the expected shift length.

	protected int numOneWay;				//number of one-lane roads.
	protected int[] lightIndexes;			//light indexes for each road.
	protected IntList reverseLightIndexes;	//road indexes for each light.

	protected double[][] flow;			//desired haulage rates on routes in both directions.
	protected double[][] roadFlow;		//haulage rates on roads in both directions.
	protected int[] initialCrushers;	//initial truck locations, based on flow through a crusher.
	protected int[][] initialSchedule;	//initial dispatch schedule out of each crusher, based on FCS.
	protected double[][] lightSchedule;	//default cyclic light schedule.
	protected double[] routeTime;		//expected route cycle time, including expected waiting times at traffic lights.
	protected double maxRouteTime;		//maximum expected cycle time.

	/**
	 * Solution constructor,
	 * with the one-lane restriction set to false (the recommended option).
	 *
	 * @see TimerBasedSolution(MineParameters4,double,boolean,boolean)
	 */
	public TimerBasedSolution(MineParameters4 params, double runtime, boolean allGreedy) {
		this(params,runtime,allGreedy,false);
	}

	/**
	 * Solution constructor.
	 *
	 * @param	params				the simulation parameters.
	 * @param	runtime				the shift length,
	 *								can be 0 if allGreedy is true.
	 * @param	allGreedy			whether to use greedy mode instead of cyclic light schedules,
	 *								which will set the light schedule values to 0.
	 * @param	oneWayRestriction	whether to restrict access to one-lane roads to a single direction when calculating haulage rates -
	 *								setting this to true will only produce a valid plan if shovels have multiple outgoing routes.
	 */
	public TimerBasedSolution(MineParameters4 params, double runtime, boolean allGreedy, boolean oneWayRestriction) {
		super(params);

		this.runtime = runtime;

		setIdealFlow(oneWayRestriction);

		setRoadFlow();

		int[] numAtCrusher = setInitialCrushers();

		setOneWayParams();

		setInitialSchedule(numAtCrusher);

		setLightSchedule(allGreedy);

		setRouteTimes();
		// System.out.printf("%s\n",Arrays.toString(routeTime));
	}

	/**
	 * Use linear programming to calculate the 'ideal' haulage rates.
	 *
	 * @param	oneWayRestriction	whether to restrict access to one-lane roads to a single direction when calculating haulage rates -
	 *								false is recommended.
	 */
	private void setIdealFlow(boolean oneWayRestriction) {
		double[] scaledEmptyTimesMean = new double[numCrusherLocs];
		for (int i=0; i<numCrusherLocs; i++) {
			scaledEmptyTimesMean[i] = emptyTimesMean[i] / numCrushers[i];
		}
		LPFlowConstructor lp = new LPFlowConstructor(numTrucks,numCrusherLocs,numShovels,numRoads,scaledEmptyTimesMean,fillTimesMean,
			roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeCrushers,
			routeShovels)
			.setOneWayRestriction(oneWayRestriction);
		// flow = lp.getFlow(null);
		flow = lp.getFlow(Debugger.isDebug() ? "debug" : null);
	}

	/**
	 * Calculate the haulage rates for each road based on the haulage rates for the routes.
	 */
	private void setRoadFlow() {
		roadFlow = new double[numRoads][2];
		for (int i=0; i<numRoutes; i++) {
			for (int j=0; j<routeLengths[i]; j++) {
				int road = routeRoads[i][j];
				int dir = routeDirections[i][j];
				roadFlow[road][dir] += flow[i][0];
				roadFlow[road][1 - dir] += flow[i][1];
			}
		}
	}

	/**
	 * Set the initial locations for trucks,
	 * based on the truck flow through each crusher.
	 *
	 * @return	an array of the number of trucks starting at each crusher.
	 */
	private int[] setInitialCrushers() {
		double totalDiggingRate = 0;
		double[] crusherFlow = new double[numCrusherLocs];
		for (int i=0; i<numRoutes; i++) {
			int cid = routeCrushers[i];
			totalDiggingRate += flow[i][0];
			crusherFlow[cid] += flow[i][0];
		}
		initialCrushers = new int[numTrucks];
		int placed = 0;
		int[] numAtCrusher = new int[numCrusherLocs];
		for (int i=0; i<numCrusherLocs; i++) {
			int share = (int) (numTrucks * crusherFlow[i] / totalDiggingRate);
			for (int j=0; j<share; j++) {
				initialCrushers[placed] = i;
				placed++;
			}
			numAtCrusher[i] = share;
		}
		for (int i=0; placed<numTrucks; i++) {
			int cid = i % numCrusherLocs;
			if (crusherFlow[cid] > 0) {
				initialCrushers[placed] = cid;
				placed++;
				numAtCrusher[cid]++;
			}
		}
		return numAtCrusher;
	}

	/**
	 * Initialise the light indexes.
	 */
	private void setOneWayParams() {
		numOneWay = 0;
		lightIndexes = new int[numRoads];
		reverseLightIndexes = new IntList();
		for (int i=0; i<numRoads; i++) {
			if (isOneWay[i]) {
				lightIndexes[i] = numOneWay;
				reverseLightIndexes.add(i);
				numOneWay++;
			}
			else {
				lightIndexes[i] = -1;
			}
		}
	}

	/**
	 * Set the initial dispatch schedule to use at shift-start,
	 * for each crusher,
	 * based on FCS.
	 *
	 * @param	numAtCrusher	an array of the number of trucks starting at each crusher.
	 */
	private void setInitialSchedule(int[] numAtCrusher) {
		initialSchedule = new int[numCrusherLocs][];
		PairQueue[] upcomingDests = new PairQueue[numCrusherLocs];
		for (int i=0; i<numCrusherLocs; i++) {
			upcomingDests[i] = new PairQueue();
		}
		for (int i=0; i<numRoutes; i++) {
			if (flow[i][0] > 0) {
				upcomingDests[routeCrushers[i]].add(new Pair(i,1.0 / flow[i][0]));
			}
		}
		for (int i=0; i<numCrusherLocs; i++) {
			initialSchedule[i] = new int[numAtCrusher[i]];
			for (int j=0; j<numAtCrusher[i]; j++) {
				Pair next = upcomingDests[i].poll();
				initialSchedule[i][j] = next.i;
				upcomingDests[i].add(new Pair(next.i,next.d + 1.0 / flow[next.i][0]));
			}
		}
	}

	/**
	 * Create a base light schedule.
	 * If allGreedy is true,
	 * the schedule will be completely greedy.
	 * Otherwise a short evolutionary algorithm is run to create a cyclic light schedule.
	 *
	 * @param	allGreedy	whether to create a greedy light schedule.
	 */
	private void setLightSchedule(boolean allGreedy) {
		if (allGreedy) {
			lightSchedule = new double[numOneWay][1];
		}
		else {
			int cycleLength = 2;
			double xoProb = 1.0;
			// CrossoverKind[] xoKinds = UnboundedFloatingArrayOperator.getAvailableCrossoverKinds();
			CrossoverKind[] xoKinds = new CrossoverKind[]{CrossoverKind.BLX_A};
			double vmProb = 1.0;
			// double mStrength = 0.17;
			double mStrength = 0.05;
			boolean allowDuplicateOffspring = false;
			int popSize = 100;
			int numOffspring = 100;
			double elitism = 0;
			boolean allowSurvivors = true;
			int bucketSize = 20;
			int resampleRate = 1;
			int resampleSize = 1;
			int maxGen = 49;
			int conCutoff = 50;
			TimeDistribution tgen = new NormalTimes();

			int genomeLength = cycleLength * numOneWay;
			double[] averageValues = new double[genomeLength];
			for (int i=0; i<numOneWay; i++) {
				int road = reverseLightIndexes.get(i);
				for (int j=0; j<cycleLength; j++) {
					averageValues[i * cycleLength + j] = (roadTravelTimesMean[road][0] + roadTravelTimesMean[road][1]) / 2.0;
				}
			}
			GenotypeBuilder<FloatingArrayGenotype> gBuilder = new UnboundedFloatingArrayBuilder(genomeLength,averageValues);
			FitnessFunction<FloatingArrayGenotype> ff = new LightTimerFitnessFunction(numTrucks,numShovels,numCrusherLocs,numCrushers,
				numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,isOneWay,
				numRoutes,routeRoads,routeDirections,routeLengths,routeShovels,routeCrushers,tgen,flow,cycleLength,initialCrushers)
				.setNumSamples(1)
				.setRuntime(runtime)
				.initialise();
			boolean maximising = ff.isMaximising();
			SelectionOperator<FloatingArrayGenotype,RollingChromosome<FloatingArrayGenotype>> selectorReproduction = new 
			FitnessProportionateReproductionOperator<>(maximising);
			SelectionOperator<FloatingArrayGenotype,RollingChromosome<FloatingArrayGenotype>> selectorSurvival = new 
				KTournamentSelectionOperator<>(maximising,2);
			GeneticOperator<FloatingArrayGenotype> operator = new UnboundedFloatingArrayOperator(genomeLength,maximising)
				.setXOParams(xoProb,xoKinds)
				.setMutationParams(vmProb,mStrength)
				.setAllowDuplicateOffspring(allowDuplicateOffspring)
				.initialise();
			RollingEvolutionaryAlgorithm<FloatingArrayGenotype> ea = new RollingEvolutionaryAlgorithm<>(gBuilder,ff,selectorReproduction,
				selectorSurvival,operator)
				.setStrategyParams(popSize,numOffspring,elitism,allowSurvivors)
				.setSamplingParams(bucketSize,resampleRate,resampleSize)
				.setTerminationParams(maxGen,conCutoff)
				.initialise();

			FloatingArrayGenotype bestGenome = ea.run().getGenotype();
			double[] array = bestGenome.getArray();
			lightSchedule = new double[numOneWay][cycleLength];
			for (int i=0; i<numOneWay; i++) {
				for (int j=0; j<cycleLength; j++) {
					lightSchedule[i][j] = array[i * cycleLength + j] + 0.1;
				}
			}
		}
	}

	/**
	 * Calculate the expected cycle time for each route.
	 */
	private void setRouteTimes() {
		maxRouteTime = 0;
		routeTime = new double[numRoutes];
		for (int i=0; i<numRoutes; i++) {
			routeTime[i] = 0;
			for (int j=0; j<routeLengths[i]; j++) {
				int road = routeRoads[i][j];
				int dir = routeDirections[i][j];
				if (isOneWay[road]) {
					double[] travelTimes = new double[]{roadTravelTimesMean[road][0],roadTravelTimesMean[road][1]};
					double[] travelTimesSD = new double[]{roadTravelTimesSD[road][0],roadTravelTimesSD[road][1]};
					travelTimes[1 - dir] *= fullSlowdown;
					travelTimes[1 - dir] *= fullSlowdown;
					double[] estTravelTimes = SharedTimeEstimator.estimateTravelTimes(travelTimes,roadFlow[road],travelTimesSD);
					routeTime[i] += estTravelTimes[0] + estTravelTimes[1];
				}
				else {
					routeTime[i] += roadTravelTimesMean[road][dir] + roadTravelTimesMean[road][1 - dir] * fullSlowdown;
				}
			}
			routeTime[i] += fillTimesMean[routeShovels[i]] + emptyTimesMean[0] / numCrushers[0];
			maxRouteTime = Math.max(routeTime[i],maxRouteTime);
		}
	}

}