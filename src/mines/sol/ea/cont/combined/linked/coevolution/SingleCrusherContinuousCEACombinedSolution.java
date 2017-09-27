package mines.sol.ea.cont.combined.linked.coevolution;

import mines.sol.*;
import mines.util.TimeDistribution;
import mines.ea.op.gene.*;
import mines.sim.MineParameters4;
import mines.ea.fitness.sim.cont.hetero.SingleCrusherCombinedCoevolutionaryFitnessFunction;
import mines.ea.gene.build.GenotypeBuilder;
import mines.ea.gene.*;
import mines.ea.gene.build.array.BoundedArrayBuilder;
import mines.ea.gene.build.farray.UnboundedFloatingArrayBuilder;
import mines.ea.op.selection.*;
import mines.ea.chrom.OptimisticChromosome;
import mines.ea.op.gene.array.BoundedListOperator;
import mines.ea.op.gene.farray.UnboundedFloatingArrayOperator;
import mines.ea.alg.*;
import java.util.*;

/**
 * Solution class for cooperative coevolution combined scheduler controller.
 */
public class SingleCrusherContinuousCEACombinedSolution extends TimerBasedSolution {

	private TimeDistribution tgen;	//the distribution used by the fitness function.

	private int numSamples;			//number of samples per fitness evaluation.
	private double lookAheadFactor;	//the multiplier of maximum cycle time to set the initial time horizon used by the fitness function.
	private int fitnessIndex;		//the index used by the fitness function to specify fitness metric.

	private double xoProbDispatch;						//the crossover probability for integer genotypes.
	private CrossoverKind xoKindDispatch;				//the crossover method for integer genotypes.
	private double vmProbDispatch;						//the value mutation probability for integer genotypes.
	private double insertProbDispatch;					//the insertion mutation probability for integer genotypes.
	private double deleteProbDispatch;					//the deletion mutation probability for integer genotypes.
	private double flipProbDispatch;					//the flip mutation probability for integer genotypes.
	private boolean allowDuplicateOffspringDispatch;	//whether to allow duplicate integer genotypes.

	private double xoProbLights;					//the crossover probability for floating point genotypes.
	private CrossoverKind[] xoKindsLights;			//the set of crossover methods to use for floating point genotypes.
	private double vmProbLights;					//the mutation probability for floating point genotypes.
	private double mStrengthLights;					//the standard deviation for gaussian mutation on floating point genotypes.
	private boolean allowDuplicateOffspringLights;	//whether to allow duplicate floating point genotypes.

	private int numCollaborators;		//the minimum number of collaborations per chromosome per round.
	private int popSizeDispatch;		//the population size for integer genotype chromosomes.
	private int popSizeLights;			//the population size for floating point genotype chromosomes.
	private int numOffspringDispatch;	//the number of offspring per generation for integer genotype chromosomes.
	private int numOffspringLights;		//the number of offspring per generation for floating point genotype chromosomes.
	private boolean allowSurvivors;		//whether to allow survivors between generations.
	private int maxGen;					//the maximum number of generations.
	private int conCutoff;				//the maximum number of generations without the required improvement before terminating.
	private double improvement;			//the required improvement.

	private double updateInterval;	//the period between schedule updates.

	private boolean initialised;	//whether this has been initialised yet.

	/**
	 * Constructor to set fundamental variables.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param	params	the simulation parameters.
	 * @param	tgen	the distribution used by the fitness function.
	 */
	public SingleCrusherContinuousCEACombinedSolution(MineParameters4 params, TimeDistribution tgen) {
		super(params,500,false);
		if (numCrusherLocs != 1) {
			throw new IllegalArgumentException(String.format("Illegal number of crusher locations %d",numCrusherLocs));
		}

		this.tgen = tgen;

		numSamples = 20;
		lookAheadFactor = 1.0;
		fitnessIndex = 0;

		xoProbDispatch = 0.99;
		xoKindDispatch = CrossoverKind.SINGLE_POINT;
		vmProbDispatch = 0.01;
		insertProbDispatch = 0.01;
		deleteProbDispatch = 0.01;
		flipProbDispatch = 0.0;
		allowDuplicateOffspringDispatch = false;

		xoProbLights = 1.0;
		xoKindsLights = new CrossoverKind[]{CrossoverKind.BLX_A};
		vmProbLights = 1.0;
		mStrengthLights = 0.05;
		allowDuplicateOffspringLights = false;

		numCollaborators = 1;
		popSizeDispatch = 100;
		popSizeLights = 100;
		numOffspringDispatch = 100;
		numOffspringLights = 100;
		allowSurvivors = true;
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
	 * @param	numSamples		number of samples per fitness evaluation.
	 * @param	lookAheadFactor	the look ahead factor -
	 *							the initial time horizon of the fitness function will be the maximum expected cycle time,
	 *							multiplied by the look ahead factor.
	 * @param	fitnessIndex	the index used by the fitness function to specify fitness metric.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @throws	IllegalArgumentException	if numSamples or lookAheadFactor are non-positive.
	 * @see	SingleCrusherCombinedCoevolutionaryFitnessFunction
	 */
	public SingleCrusherContinuousCEACombinedSolution setFitnessParams(int numSamples, double lookAheadFactor, int fitnessIndex) {
		if (!initialised) {
			if (numSamples <= 0) {
				throw new IllegalArgumentException(String.format("Number of samples must be positive: %d",numSamples));
			}
			this.numSamples = numSamples;
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
	 * @param	xoProbDispatch	the crossover probability for integer genotypes.
	 * @param	xoKindDispatch	the crossover method for integer genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	BoundedListOperator
	 */
	public SingleCrusherContinuousCEACombinedSolution setDispatchXOParams(double xoProbDispatch, CrossoverKind xoKindDispatch) {
		if (!initialised) {
			this.xoProbDispatch = xoProbDispatch;
			this.xoKindDispatch = xoKindDispatch;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the mutation parameters for integer genotypes.
	 * Can only be used before initialisation.
	 *
	 * @param	vmProbDispatch		the value mutation probability for integer genotypes.
	 * @param	insertProbDispatch	the insertion mutation probability for integer genotypes.
	 * @param	deleteProbDispatch	the deletion mutation probability for integer genotypes.
	 * @param	flipProbDispatch	the flip mutation probability for integer genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	BoundedListOperator
	 */
	public SingleCrusherContinuousCEACombinedSolution setDispatchMutationParams(double vmProbDispatch, double insertProbDispatch, 
		double deleteProbDispatch, double flipProbDispatch) {
		if (!initialised) {
			this.vmProbDispatch = vmProbDispatch;
			this.insertProbDispatch = insertProbDispatch;
			this.deleteProbDispatch = deleteProbDispatch;
			this.flipProbDispatch = flipProbDispatch;
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
	 * @param	allowDuplicateOffspringDispatch	whether to allow duplicate integer genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	BoundedListOperator
	 */
	public SingleCrusherContinuousCEACombinedSolution setDispatchAllowDuplicateOffspring(boolean allowDuplicateOffspringDispatch) {
		if (!initialised) {
			this.allowDuplicateOffspringDispatch = allowDuplicateOffspringDispatch;
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
	 * @param	xoProbLights	the crossover probability for integer genotypes.
	 * @param	xoKindsLights	the crossover method for integer genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	UnboundedFloatingArrayOperator
	 */
	public SingleCrusherContinuousCEACombinedSolution setLightsXOParams(double xoProbLights, CrossoverKind[] xoKindsLights) {
		if (!initialised) {
			this.xoProbLights = xoProbLights;
			// this.xoKindsLights = xoKindsLights;
			this.xoKindsLights = Arrays.copyOf(xoKindsLights,xoKindsLights.length);
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
	 * @param	vmProbLights	the mutation probability for floating point genotypes.
	 * @param	mStrengthLights	the standard deviation for gaussian mutation on floating point genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	UnboundedFloatingArrayOperator
	 */
	public SingleCrusherContinuousCEACombinedSolution setLightsMutationParams(double vmProbLights, double mStrengthLights) {
		if (!initialised) {
			this.vmProbLights = vmProbLights;
			this.mStrengthLights = mStrengthLights;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set whether to allow duplicate floating point genotypes.
	 * Can only be used before initialisation.
	 *
	 * @param	allowDuplicateOffspringLights	whether to allow duplicate floating point genotypes.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	UnboundedFloatingArrayOperator
	 */
	public SingleCrusherContinuousCEACombinedSolution setLightsAllowDuplicateOffspring(boolean allowDuplicateOffspringLights) {
		if (!initialised) {
			this.allowDuplicateOffspringLights = allowDuplicateOffspringLights;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the minimum number of collaborations per chromosome per round.
	 * Can only be used before initialisation.
	 *
	 * @param	numCollaborators	the minimum number of collaborations per chromosome per round.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @throws	IllegalArgumentException	if numCollaborators is non-positive.
	 * @see	ParallelShufflingHeterogeneousCoevolutionaryAlgorithm
	 */
	public SingleCrusherContinuousCEACombinedSolution setCollaborationParams(int numCollaborators) {
		if (!initialised) {
			if (numCollaborators <= 0) {
				throw new IllegalArgumentException(String.format("Number of collaborations must be positive: %d",numCollaborators));
			}
			this.numCollaborators = numCollaborators;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the CEA population parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	popSizeDispatch			the population size for integer genotype chromosomes.
	 * @param	popSizeLights			the population size for floating point genotype chromosomes.
	 * @param	numOffspringDispatch	the number of offspring per generation for integer genotype chromosomes.
	 * @param	numOffspringLights		the number of offspring per generation for floating point genotype chromosomes.
	 * @param	allowSurvivors			whether to allow survivors between generations.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @throws	IllegalArgumentException	if sizes are infeasible.
	 * @see	ParallelShufflingHeterogeneousCoevolutionaryAlgorithm
	 */
	public SingleCrusherContinuousCEACombinedSolution setStrategyParams(int popSizeDispatch, int popSizeLights, int numOffspringDispatch, 
		int numOffspringLights, boolean allowSurvivors) {
		if (!initialised) {
			if (popSizeDispatch <= 0) {
				throw new IllegalArgumentException(String.format("Population size must be positive: %d",popSizeDispatch));
			}
			this.popSizeDispatch = popSizeDispatch;
			if (popSizeLights <= 0) {
				throw new IllegalArgumentException(String.format("Population size must be positive: %d",popSizeLights));
			}
			this.popSizeLights = popSizeLights;
			if (numOffspringDispatch <= 0) {
				throw new IllegalArgumentException(String.format("Offspring size must be positive: %d",numOffspringDispatch));
			}
			this.numOffspringDispatch = numOffspringDispatch;
			if (numOffspringLights <= 0) {
				throw new IllegalArgumentException(String.format("Offspring size must be positive: %d",numOffspringLights));
			}
			this.numOffspringLights = numOffspringLights;
			if (!allowSurvivors) {
				if (numOffspringDispatch < popSizeDispatch) {
					throw new IllegalArgumentException(String.format("If surviving chromosomes not allowed then offspring size must be" + 
						" at least population size: %d %d",popSizeDispatch,numOffspringDispatch));
				}
				if (numOffspringLights < popSizeLights) {
					throw new IllegalArgumentException(String.format("If surviving chromosomes not allowed then offspring size must be" + 
						" at least population size: %d %d",popSizeLights,numOffspringLights));
				}
			}
			this.allowSurvivors = allowSurvivors;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	/**
	 * Set the CEA termination parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	maxGen		the maximum number of generations.
	 * @param	concCutoff	the maximum number of generations without the required improvement before terminating.
	 * @param	improvement	the required improvement.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	ParallelShufflingHeterogeneousCoevolutionaryAlgorithm
	 */
	public SingleCrusherContinuousCEACombinedSolution setTerminationParams(int maxGen, int conCutoff, double improvement) {
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
	public SingleCrusherContinuousCEACombinedSolution setUpdateInterval(double updateInterval) {
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
	public SingleCrusherContinuousCEACombinedSolution initialise() {
		if (!initialised) {
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Solution already initialised");
		}
	}

	public SingleCrusherContinuousCEACombinedController getController() {
		if (initialised) {
			double lookAhead = lookAheadFactor * maxRouteTime;

			int genomeLengthDispatch = (int) Math.ceil(lookAhead / (emptyTimesMean[0] / numCrushers[0]));
			int[] maxValuesDispatch = new int[genomeLengthDispatch];
			for (int i=0; i<genomeLengthDispatch; i++) {
				maxValuesDispatch[i] = numRoutes;
			}

			int[] lightScheduleLengths = new int[numOneWay];
			int genomeLengthLights = 0;
			for (int i=0; i<numOneWay; i++) {
				int road = reverseLightIndexes.get(i);
				lightScheduleLengths[i] = (int) Math.ceil(lookAhead * 2.0 / (roadTravelTimesMean[road][0] + roadTravelTimesMean[road][1]));
				genomeLengthLights += lightScheduleLengths[i];
			}
			double[] averageValuesLights = new double[genomeLengthLights];
			int place = 0;
			for (int i=0; i<numOneWay; i++) {
				int road = reverseLightIndexes.get(i);
				double av = (roadTravelTimesMean[road][0] + roadTravelTimesMean[road][1]) / 2.0;
				for (int j=0; j<lightScheduleLengths[i]; j++) {
					averageValuesLights[place] = av;
					place++;
				}
			}

			SingleCrusherCombinedCoevolutionaryFitnessFunction ff = new SingleCrusherCombinedCoevolutionaryFitnessFunction(numTrucks,
				numShovels,numCrusherLocs,numCrushers,numRoads,emptyTimesMean,emptyTimesSD,fillTimesMean,fillTimesSD,roadTravelTimesMean,
				roadTravelTimesSD,fullSlowdown,isOneWay,numRoutes,routeRoads,routeDirections,routeLengths,routeShovels,routeCrushers,tgen,
				genomeLengthDispatch,lightScheduleLengths,lightSchedule)
				.setNumSamples(numSamples)
				.setLookAhead(lookAhead)
				.setFitnessIndex(fitnessIndex)
				.initialise();
			boolean maximising = ff.isMaximising();

			GenotypeBuilder<ArrayGenotype> gBuilderDispatch = new BoundedArrayBuilder(genomeLengthDispatch,maxValuesDispatch);
			GenotypeBuilder<FloatingArrayGenotype> gBuilderLights = new UnboundedFloatingArrayBuilder(genomeLengthLights,averageValuesLights);
			SelectionOperator<ArrayGenotype,OptimisticChromosome<ArrayGenotype>> selectorReproductionDispatch = new 
				FitnessProportionateReproductionOperator<>(maximising);
			SelectionOperator<FloatingArrayGenotype,OptimisticChromosome<FloatingArrayGenotype>> selectorReproductionLights = new 
				FitnessProportionateReproductionOperator<>(maximising);
			SelectionOperator<ArrayGenotype,OptimisticChromosome<ArrayGenotype>> selectorSurvivalDispatch = new RankedSurvivalOperator<>(
				maximising);
			SelectionOperator<FloatingArrayGenotype,OptimisticChromosome<FloatingArrayGenotype>> selectorSurvivalLights = new 
				RankedSurvivalOperator<>(maximising);
			GeneticOperator<ArrayGenotype> operatorDispatch = new BoundedListOperator(genomeLengthDispatch,numRoutes)
				.setXOParams(xoProbDispatch,xoKindDispatch)
				.setMutationParams(vmProbDispatch,insertProbDispatch,deleteProbDispatch,flipProbDispatch)
				.setAllowDuplicateOffspring(allowDuplicateOffspringDispatch)
				.initialise();
			GeneticOperator<FloatingArrayGenotype> operatorLights = new UnboundedFloatingArrayOperator(genomeLengthLights,maximising)
				.setXOParams(xoProbLights,xoKindsLights)
				.setMutationParams(vmProbLights,mStrengthLights)
				.setAllowDuplicateOffspring(allowDuplicateOffspringLights)
				.initialise();

			HeterogeneousCoevolutionaryAlgorithm<ArrayGenotype,FloatingArrayGenotype> cea = new 
			ParallelShufflingHeterogeneousCoevolutionaryAlgorithm<>(numCollaborators,gBuilderDispatch,gBuilderLights,ff,
				selectorReproductionDispatch,selectorReproductionLights,selectorSurvivalDispatch,selectorSurvivalLights,operatorDispatch,
				operatorLights)
				.setStrategyParams(popSizeDispatch,popSizeLights,numOffspringDispatch,numOffspringLights,allowSurvivors)
				.setTerminationParams(maxGen,conCutoff,improvement)
				.initialise();

			return new SingleCrusherContinuousCEACombinedController(numTrucks,numOneWay,lightIndexes,lightScheduleLengths,
				initialSchedule[0],ff,cea,updateInterval);
		}
		else {
			throw new IllegalStateException("Solution not initialised");
		}
	}

	public String getSolutionName() {
		return "Combined scheduling by CEA";
	}
	
}