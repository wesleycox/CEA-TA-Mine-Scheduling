package mines.sol.ea.cont.dispatch.fixed;

import mines.sol.TimerBasedSolution;
import mines.util.TimeDistribution;
import mines.ea.op.gene.*;
import mines.sim.MineParameters4;
import mines.ea.gene.build.GenotypeBuilder;
import mines.ea.gene.ArrayGenotype;
import mines.ea.gene.build.array.BoundedArrayBuilder;
import mines.ea.fitness.sim.cont.array.dispatch.SingleCrusherFLDispatchWTimerFitnessFunction;
import mines.ea.op.selection.*;
import mines.ea.chrom.RollingChromosome;
import mines.ea.op.gene.array.BoundedListOperator;
import mines.ea.alg.*;

/**
 * Solution class for evolutionary dispatch scheduler controller.
 */
public class SingleCrusherContinuousEAFLListDispatchWTimerSolution extends TimerBasedSolution {

	private TimeDistribution tgen;	//the distribution used by the fitness function.

	private double lookAheadFactor;
	//the multiplier of maximum cycle time to set the initial time horizon used by the fitness function.
	private int fitnessIndex;					//the index used by the fitness function to specify fitness metric.
	private double xoProb;						//the crossover probability.
	private CrossoverKind xoKind;				//the crossover method.
	private double vmProb;						//the value mutation probability.
	private double insertProb;					//the insertion mutation probability.
	private double deleteProb;					//the deletion mutation probability.
	private double flipProb;					//the flip mutation probability.
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
	private double updateInterval;				//the period between schedule updates.

	private boolean initialised;	//whether this has been initialised yet.

	/**
	 * Constructor to set fundamental variables.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param	params		the simulation parameters.
	 * @param	runtime		the expected shift length.
	 * @param	allGreedy	whether to use greedy rules for light schedules.
	 * @param	tgen		the distribution used by the fitness function.
	 */
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution(MineParameters4 params, double runtime, boolean allGreedy, 
		TimeDistribution tgen) {
		super(params,runtime,allGreedy);
		if (numCrusherLocs != 1) {
			throw new IllegalArgumentException(String.format("Illegal number of crusher locations %d",numCrusherLocs));
		}

		this.tgen = tgen;

		lookAheadFactor = 1.0;
		fitnessIndex = 0;
		xoProb = 0.99;
		xoKind = CrossoverKind.SINGLE_POINT;
		vmProb = 0.01;
		insertProb = 0.01;
		deleteProb = 0.01;
		flipProb = 0;
		allowDuplicateOffspring = false;
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
		updateInterval = 15;

		initialised = false;
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
	 * @throws	IllegalArgumentException	if lookAheadFactor is non-positive.
	 * @see	SingleCrusherFLDispatchWTimerFitnessFunction
	 */
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution setFitnessParams(double lookAheadFactor, int fitnessIndex) {
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
	 * Set the crossover parameters for integer genotypes.
	 * Can only be used before initialisation.
	 *
	 * @param	xoProb	the crossover probability for integer genotypes.
	 * @param	xoKind	the crossover method for integer genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	BoundedListOperator
	 */
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution setXOParams(double xoProb, CrossoverKind xoKind) {
		if (!initialised) {
			this.xoProb = xoProb;
			this.xoKind = xoKind;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the mutation parameters,
	 * with a flip mutation probability of 0.
	 *
	 * @see setMutationParams(double,double,double,double)
	 */
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution setMutationParams(double vmProb, double insertProb, double deleteProb) {
		return setMutationParams(vmProb,insertProb,deleteProb,0.0);
	}

	/**
	 * Set the mutation parameters for integer genotypes.
	 * Can only be used before initialisation.
	 *
	 * @param	vmProb		the value mutation probability for integer genotypes.
	 * @param	insertProb	the insertion mutation probability for integer genotypes.
	 * @param	deleteProb	the deletion mutation probability for integer genotypes.
	 * @param	flipProb	the flip mutation probability for integer genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	BoundedListOperator
	 */
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution setMutationParams(double vmProb, double insertProb, double deleteProb, 
		double flipProb) {
		if (!initialised) {
			this.vmProb = vmProb;
			this.insertProb = insertProb;
			this.deleteProb = deleteProb;
			this.flipProb = flipProb;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set whether to allow duplicate integer genotypes.
	 * Can only be used before initialisation.
	 *
	 * @param	allowDuplicateOffspring	whether to allow duplicate integer genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	BoundedListOperator
	 */
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution setAllowDuplicateOffspring(boolean allowDuplicateOffspring) {
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
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution setStrategyParams(int popSize, int numOffspring, double elitism, 
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
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution setSamplingParams(int bucketSize, int resampleRate, int resampleSize) {
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
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution setTerminationParams(int maxGen, int conCutoff) {
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
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution setTerminationParams(int maxGen, int conCutoff, double improvement) {
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
	 * Set the update period.
	 *
	 * @param	updateInterval	the period between schedule updates.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 */
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution setUpdateInterval(double updateInterval) {
		if (!initialised) {
			this.updateInterval = updateInterval;
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
	public SingleCrusherContinuousEAFLListDispatchWTimerSolution initialise() {
		if (!initialised) {
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	public SingleCrusherContinuousEAFLDispatchWTimerController getController() {
		if (initialised) {
			double lookAhead = lookAheadFactor * maxRouteTime;

			int genomeLength = (int) Math.ceil(lookAhead / (emptyTimesMean[0] / numCrushers[0]));
			int[] maxValues = new int[genomeLength];
			for (int i=0; i<genomeLength; i++) {
				maxValues[i] = numRoutes;
			}

			GenotypeBuilder<ArrayGenotype> gBuilder = new BoundedArrayBuilder(genomeLength,maxValues);
			SingleCrusherFLDispatchWTimerFitnessFunction ff = new SingleCrusherFLDispatchWTimerFitnessFunction(numTrucks,numShovels,
				numCrusherLocs,numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,roadTravelTimesMean,
				roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeShovels,routeCrushers,
				tgen,lightSchedule,genomeLength)
				.setNumSamples(1)
				.setLookAhead(lookAhead)
				.setFitnessIndex(fitnessIndex)
				.initialise();
			boolean maximising = ff.isMaximising();
			SelectionOperator<ArrayGenotype,RollingChromosome<ArrayGenotype>> selectorReproduction = new 
				FitnessProportionateReproductionOperator<>(maximising);
			SelectionOperator<ArrayGenotype,RollingChromosome<ArrayGenotype>> selectorSurvival = new 
				RankedSurvivalOperator<>(maximising);
			GeneticOperator<ArrayGenotype> operator = new BoundedListOperator(genomeLength,numRoutes)
				.setXOParams(xoProb,xoKind)
				.setMutationParams(vmProb,insertProb,deleteProb,flipProb)
				.setAllowDuplicateOffspring(allowDuplicateOffspring)
				.initialise();
			EvolutionaryAlgorithm<ArrayGenotype> ea = new RollingEvolutionaryAlgorithm<>(gBuilder,ff,selectorReproduction,selectorSurvival,
				operator)
				.setStrategyParams(popSize,numOffspring,elitism,allowSurvivors)
				.setSamplingParams(bucketSize,resampleRate,resampleSize)
				.setTerminationParams(maxGen,conCutoff,improvement)
				.initialise();
			return new SingleCrusherContinuousEAFLDispatchWTimerController(numTrucks,numOneWay,lightIndexes,updateInterval,lightSchedule,
				initialSchedule[0],ff,ea);
		}
		else {
			throw new IllegalStateException("Solution not initialised");
		}
	}

	public String getSolutionName() {
		return "Continuous dispatch (FL-List) by EA w/ timer";
	}

}