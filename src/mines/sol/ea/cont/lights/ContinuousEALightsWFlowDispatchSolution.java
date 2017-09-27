package mines.sol.ea.cont.lights;

import mines.sol.*;
import mines.util.TimeDistribution;
import mines.ea.op.gene.*;
import mines.sim.MineParameters4;
import mines.ea.gene.build.GenotypeBuilder;
import mines.ea.gene.FloatingArrayGenotype;
import mines.ea.gene.build.farray.UnboundedFloatingArrayBuilder;
import mines.ea.fitness.sim.cont.farray.LightsWFlowDispatchFitnessFunction;
import mines.ea.op.selection.*;
import mines.ea.chrom.RollingChromosome;
import mines.ea.op.gene.farray.UnboundedFloatingArrayOperator;
import mines.ea.alg.*;
import java.util.*;

/**
 * Solution class for evolutionary light scheduler controller.
 */
public class ContinuousEALightsWFlowDispatchSolution extends TimerBasedSolution {

	private TimeDistribution tgen;	//the distribution used by the fitness function.

	private double lookAheadFactor;
	//the multiplier of maximum cycle time to set the initial time horizon used by the fitness function.
	private double updateInterval;				//the period between schedule updates.
	private int fitnessIndex;					//the index used by the fitness function to specify fitness metric.
	private boolean allowDuplicateOffspring;	//whether to allow duplicate genotypes.
	private int popSize;						//the population size.
	private int numOffspring;					//the number of offspring per generation.
	private int bucketSize;						//the chromosome fitness bucket size.
	private int resampleRate;					//the generation period between fitness resampling, 0 if no resampling.
	private int resampleSize;					//the number of samples per fitness resampling period.
	private int maxGen;							//the maximum number of generations.
	private int conCutoff;						//the maximum number of generations without the required improvement before terminating.
	private double improvement;					//the required improvement.
	private double elitism;						//the proportion of elite chromosomes to have guaranteed survival.
	private boolean allowSurvivors;				//whether to allow survivors between generations.
	private double xoProb;						//the crossover probability.
	private CrossoverKind[] xoKinds;			//the set of crossover methods to use.
	private double vmProb;						//the mutation probability.
	private double mStrength;					//the standard deviation for gaussian mutation.

	private boolean initialised;	//whether this has been initialised yet.
	
	public ContinuousEALightsWFlowDispatchSolution(MineParameters4 params, TimeDistribution tgen) {
		super(params,/*0,true*/500,false);

		this.tgen = tgen;

		lookAheadFactor = 1.0;
		updateInterval = 15;
		fitnessIndex = 0;
		allowDuplicateOffspring = true;
		popSize = 100;
		numOffspring = 100;
		elitism = 0;
		allowSurvivors = true;
		bucketSize = 20;
		resampleRate = 1;
		resampleSize = 1;
		maxGen = 999;
		conCutoff = 99;
		improvement = 0.005;
		xoProb = 1.0;
		xoKinds = new CrossoverKind[]{CrossoverKind.BLX_A};
		vmProb = 1.0;
		mStrength = 0.05;

		initialised = false;
	}

	/**
	 * Set the update period.
	 *
	 * @param	updateInterval	the period between schedule updates.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 */
	public ContinuousEALightsWFlowDispatchSolution setUpdateInterval(double updateInterval) {
		if (!initialised) {
			this.updateInterval = updateInterval;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the fitness function parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	lookAheadFactor	the look ahead factor -
	 *							the initial time horizon of the fitness function will be the maximum expected cycle time,
	 *							multiplied by the look ahead factor.
	 * @param	fitnessIndex	the index used by the fitness function to specify fitness metric.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @throws	IllegalArgumentException	if numSamples or lookAheadFactor are non-positive.
	 * @see	SingleCrusherFLDispatchWTimerFitnessFunction
	 */
	public ContinuousEALightsWFlowDispatchSolution setFitnessParams(double lookAheadFactor, int fitnessIndex) {
		if (!initialised) {
			if (lookAheadFactor <= 0) {
				throw new IllegalArgumentException(String.format("Look ahead factor must be positive: %f",lookAheadFactor));
			}
			this.lookAheadFactor = lookAheadFactor;
			this.fitnessIndex = fitnessIndex;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set whether to allow duplicate genotypes.
	 * Can only be used before initialisation.
	 *
	 * @param	allowDuplicateOffspring	whether to allow integer genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	UnboundedFloatingArrayOperator
	 */
	public ContinuousEALightsWFlowDispatchSolution setAllowDuplicateOffspring(boolean allowDuplicateOffspring) {
		if (!initialised) {
			this.allowDuplicateOffspring = allowDuplicateOffspring;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the EA population parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	popSize			the population size for integer genotype chromosomes.
	 * @param	numOffspring	the number of offspring per generation for integer genotype chromosomes.
	 * @param	elitism			the proportion of elite chromosomes to have guaranteed survival.
	 * @param	allowSurvivors	whether to allow survivors between generations.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @throws	IllegalArgumentException	if sizes are infeasible.
	 * @see	RollingEvolutionaryAlgorithm
	 */
	public ContinuousEALightsWFlowDispatchSolution setStrategyParams(int popSize, int numOffspring, double elitism, 
		boolean allowSurvivors) {
		if (!initialised) {
			if (popSize <= 0) {
				throw new IllegalArgumentException(String.format("Population size must be positive: %d",popSize));
			}
			this.popSize = popSize;
			if (numOffspring <= 0) {
				throw new IllegalArgumentException(String.format("Offspring size must be positive: %d",numOffspring));
			}
			this.numOffspring = numOffspring;
			this.elitism = elitism;
			if (!allowSurvivors && numOffspring < popSize) {
				throw new IllegalArgumentException(String.format("If surviving chromosomes not allowed then offspring size must be" + 
					" at least population size: %d %d",popSize,numOffspring));
			}
			this.allowSurvivors = allowSurvivors;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the resampling parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	bucketSize		the fitness bucket size.
	 * @param	resampleRate	the period between fitness resampling,
	 *							0 if no resampling.
	 * @param	resampleSize	the number of reevaluations per resampling period.
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 * @see	RollingEvolutionaryAlgorithm
	 */
	public ContinuousEALightsWFlowDispatchSolution setSamplingParams(int bucketSize, int resampleRate, int resampleSize) {
		if (!initialised) {
			this.bucketSize = bucketSize;
			this.resampleRate = resampleRate;
			this.resampleSize = resampleSize;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the EA termination parameters,
	 * with improvement of 0.0 (termination once no improvement observed).
	 *
	 * @see setTerminationParams(int,int,double)
	 */
	public ContinuousEALightsWFlowDispatchSolution setTerminationParams(int maxGen, int conCutoff) {
		return setTerminationParams(maxGen,conCutoff,0.0);
	}

	/**
	 * Set the EA termination parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	maxGen		the maximum number of generations.
	 * @param	concCutoff	the maximum number of generations without the required improvement before terminating.
	 * @param	improvement	the required improvement.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	RollingEvolutionaryAlgorithm
	 */
	public ContinuousEALightsWFlowDispatchSolution setTerminationParams(int maxGen, int conCutoff, double improvement) {
		if (!initialised) {
			this.maxGen = maxGen;
			this.conCutoff = conCutoff;
			this.improvement = improvement;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the crossover parameters for floating point genotypes.
	 * Can only be used before initialisation.
	 *
	 * @param	xoProb	the crossover probability for integer genotypes.
	 * @param	xoKind	the crossover method for integer genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	UnboundedFloatingArrayOperator
	 */
	public ContinuousEALightsWFlowDispatchSolution setXOParams(double xoProb, CrossoverKind[] xoKinds) {
		if (!initialised) {
			this.xoProb = xoProb;
			// this.xoKinds = xoKinds;
			this.xoKinds = Arrays.copyOf(xoKinds,xoKinds.length);
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the mutation parameters for floating point genotypes.
	 * Can only be used before initialisation.
	 *
	 * @param	vmProb		the mutation probability for floating point genotypes.
	 * @param	mStrength	the standard deviation for gaussian mutation on floating point genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	UnboundedFloatingArrayOperator
	 */
	public ContinuousEALightsWFlowDispatchSolution setMutationParams(double vmProb, double mStrength) {
		if (!initialised) {
			this.vmProb = vmProb;
			this.mStrength = mStrength;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Initialise this object for use.
	 * Can only be used once.
	 *
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public ContinuousEALightsWFlowDispatchSolution initialise() {
		if (!initialised) {
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	public ContinuousEALightsWFlowDispatchController getController() {
		if (initialised) {
			double lookAhead = lookAheadFactor * maxRouteTime;

			int[] scheduleLengths = new int[numOneWay];
			int genomeLength = 0;
			for (int i=0; i<numOneWay; i++) {
				int road = reverseLightIndexes.get(i);
				scheduleLengths[i] = (int) Math.ceil(lookAhead * 2.0 / (roadTravelTimesMean[road][0] + roadTravelTimesMean[road][1]));
				genomeLength += scheduleLengths[i];
			}

			double[] averageValues = new double[genomeLength];
			int place = 0;
			for (int i=0; i<numOneWay; i++) {
				int road = reverseLightIndexes.get(i);
				double av = (roadTravelTimesMean[road][0] + roadTravelTimesMean[road][1]) / 2.0;
				for (int j=0; j<scheduleLengths[i]; j++) {
					averageValues[place] = av;
					place++;
				}
			}

			GenotypeBuilder<FloatingArrayGenotype> gBuilder = new UnboundedFloatingArrayBuilder(genomeLength,averageValues);
			LightsWFlowDispatchFitnessFunction ff = new LightsWFlowDispatchFitnessFunction(numTrucks,numShovels,numCrusherLocs,
				numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,roadTravelTimesMean,roadTravelTimesSD,fullSlowdown,
				isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeShovels,routeCrushers,tgen,flow,scheduleLengths,lightSchedule)
				.setNumSamples(1)
				.setLookAhead(lookAhead)
				.setFitnessIndex(fitnessIndex)
				.initialise();
			boolean maximising = ff.isMaximising();
			SelectionOperator<FloatingArrayGenotype,RollingChromosome<FloatingArrayGenotype>> selectorReproduction = new 
				FitnessProportionateReproductionOperator<>(maximising);
			SelectionOperator<FloatingArrayGenotype,RollingChromosome<FloatingArrayGenotype>> selectorSurvival = new 
				// KTournamentSelectionOperator<>(maximising,2);
				RankedSurvivalOperator<>(maximising);
			GeneticOperator<FloatingArrayGenotype> operator = new UnboundedFloatingArrayOperator(genomeLength,maximising)
				.setXOParams(xoProb,xoKinds)
				.setMutationParams(vmProb,mStrength)
				.setAllowDuplicateOffspring(allowDuplicateOffspring)
				.initialise();
			EvolutionaryAlgorithm<FloatingArrayGenotype> ea = new RollingEvolutionaryAlgorithm<>(gBuilder,ff,selectorReproduction,
				selectorSurvival,operator)
				.setStrategyParams(popSize,numOffspring,elitism,allowSurvivors)
				.setSamplingParams(bucketSize,resampleRate,resampleSize)
				.setTerminationParams(maxGen,conCutoff,improvement)
				.initialise();
			return new ContinuousEALightsWFlowDispatchController(numTrucks,numCrusherLocs,numShovels,numRoutes,routeCrushers,routeShovels,
				numOneWay,lightIndexes,flow,initialCrushers,updateInterval,scheduleLengths,ff,ea);
		}
		else {
			throw new IllegalStateException("Solution not initialised");
		}
	}

	public String getSolutionName() {
		return "EA light scheduling w/ flow dispatch";
	}
}