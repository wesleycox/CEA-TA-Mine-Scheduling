package mines.ea.alg;

import mines.ea.gene.Genotype;
import mines.ea.gene.build.GenotypeBuilder;
import mines.ea.fitness.FitnessFunction;
import mines.ea.op.selection.SelectionOperator;
import mines.ea.chrom.*;
import mines.ea.op.gene.GeneticOperator;
import mines.util.DoubleList;
import mines.system.Debugger;
import java.util.*;

/**
 * An evolutionary algorithm for stochastic fitness functions.
 * Fitness is maintained as a rolling average,
 * by continuously adding fitness evaluations to a fitness bucket,
 * and removing old values from the bucket.
 */
public class RollingEvolutionaryAlgorithm<G extends Genotype> implements EvolutionaryAlgorithm<G> {

	private static final int DEBUG_INTERVAL = 10;	//the period between debugging messages.

	private int popSize;			//surviving population size.
	private int numOffspring;		//number of offspring per generation.
	private int bucketSize;			//size of fitness bucket.
	private int resampleRate;		//number of generations between reevaluations.
	private int resampleSize;		//number of evaluations per reevaluation period.
	private int maxGen;				//maximum number of generations.
	private int conCutoff;			//number of generations allowed without required improvement.
	private double improvement;		//required improvement.
	private double elitism;			//portion of best chromosomes guaranteed to survive.
	private boolean allowSurvivors;	//whether to allow non-elite chromosomes to survive between generations.

	private GenotypeBuilder<G> gBuilder;									//random genotype generator.
	private FitnessFunction<G> ff;											//fitness function.
	private SelectionOperator<G,RollingChromosome<G>> selectorReproduction;	//selection operator for reproduction.
	private SelectionOperator<G,RollingChromosome<G>> selectorSurvival;		//selection operator for survival.
	private GeneticOperator<G> operator;									//mutation and crossover operator.

	private Random rng;						//RNG.
	private Comparator<Chromosome> comp;	//fitness comparison.
	private boolean maximising;				//whether fitness is maximising or not.

	private ArrayList<RollingChromosome<G>> population;	//current population.

	private boolean initialised;	//whether this algorithm has been initialised yet.

	/**
	 * Constructor to set fundamental variables.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param	gBuilder				the random genotype generator.
	 * @param	ff						the fitness function.
	 * @param	selectorReprodcution	the selection operator for reproduction.
	 * @param	selectorSurvival		the selection operator for survival.
	 * @param	operator				the mutation and crossover operator.
	 */
	public RollingEvolutionaryAlgorithm(GenotypeBuilder<G> gBuilder, FitnessFunction<G> ff, 
		SelectionOperator<G,RollingChromosome<G>> selectorReproduction, SelectionOperator<G,RollingChromosome<G>> selectorSurvival, 
		GeneticOperator<G> operator) {
		this.gBuilder = gBuilder;
		this.ff = ff;
		this.selectorReproduction = selectorReproduction;
		this.selectorSurvival = selectorSurvival;
		this.operator = operator;

		rng = new Random();
		maximising = ff.isMaximising();
		comp = new Comparator<Chromosome>() {
			public int compare(Chromosome c1, Chromosome c2) {
				int diff = (maximising ? -1 : 1) * c1.compareTo(c2);
				return (diff == 0 ? (c2.getAge() - c1.getAge()) : diff);
			}
		};

		popSize = 100;
		numOffspring = 100;
		bucketSize = 1;
		resampleRate = 0;
		resampleSize = 0;
		maxGen = 999;
		conCutoff = 99;
		improvement = 0.0;
		elitism = 0.0;
		allowSurvivors = false;

		initialised = false;
	}

	/**
	 * Set the population and offspring sizes.
	 * Can only be used before initialisation.
	 *
	 * @param	popSize			the population size.
	 * @param	numOffspring	the number of offspring per generation.
	 * @param	elitism			the portion of best chromosomes to survive by elitism.
	 * @param	allowSurvivors	whether to allow non-elite chromosomes to survive between generations.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @throws	IllegalArgumentException	if non-positive population arguments are given, or elitism greater than 1.
	 */
	public RollingEvolutionaryAlgorithm<G> setStrategyParams(int popSize, int numOffspring, double elitism, boolean allowSurvivors) {
		if (!initialised) {
			if (popSize <= 0) {
				throw new IllegalArgumentException(String.format("Positive population size required: %d",popSize));
			}
			this.popSize = popSize;
			if (numOffspring <= 0) {
				throw new IllegalArgumentException(String.format("Positive offspring size required: %d",numOffspring));
			}
			this.numOffspring = numOffspring;
			if (elitism > 1) {
				throw new IllegalArgumentException(String.format("Elitism less than 1 required: %f",elitism));
			}
			this.elitism = elitism;
			this.allowSurvivors = allowSurvivors;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
	}

	/**
	 * Set the resampling parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	bucketSize		the fitness bucket size.
	 * @param	resampleRate	the period between fitness resampling.
	 * @param	resampleSize	the number of reevaluations per resampling period.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @throws	IllegalArgumentException	if non-positive bucket size is given.
	 */
	public RollingEvolutionaryAlgorithm<G> setSamplingParams(int bucketSize, int resampleRate, int resampleSize) {
		if (!initialised) {
			if (bucketSize <= 0) {
				throw new IllegalArgumentException(String.format("Positive fitness bucket size required: %d",bucketSize));
			}
			this.bucketSize = bucketSize;
			this.resampleRate = resampleRate;
			this.resampleSize = resampleSize;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
	}

	/**
	 * Set the termination parameters with improvement of 0.
	 * Can only be used before initialisation.
	 *
	 * @see	setTerminationParams(int,int,double)
	 */
	public RollingEvolutionaryAlgorithm<G> setTerminationParams(int maxGen, int conCutoff) {
		return setTerminationParams(maxGen,conCutoff,0.0);
	}

	/**
	 * Set the termination parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	maxGen		the maximum number of generations.
	 * @param	conCutoff	the number of generations allowed without required improvement.
	 * @param	improvement	the required improvement.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @throws	IllegalArgumentException	if improvement is negative.
	 */
	public RollingEvolutionaryAlgorithm<G> setTerminationParams(int maxGen, int conCutoff, double improvement) {
		if (!initialised) {
			this.maxGen = maxGen;
			this.conCutoff = conCutoff;
			if (improvement < 0) {
				throw new IllegalArgumentException(String.format("Non-negative improvement required: %f",improvement));
			}
			this.improvement = improvement;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
	}

	/**
	 * Initialise this object for use.
	 * Can only be used once.
	 *
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public RollingEvolutionaryAlgorithm<G> initialise() {
		if (!initialised) {
			population = null;
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
	}

	/**
	 * Run the evolutionary algorithm and return the best chromosome.
	 * The operation is as follows:
	 * The population is initialised randomly,
	 * and each initial chromosome has its fitness bucket filled.
	 * For each generation,
	 * mark the best chromosomes for survival by elitism,
	 * produce offspring by the genetic operator,
	 * and create a new population by selection and elitism.
	 * New chromosomes have their fitness buckets filled,
	 * and surviving chromosomes receive new evaluations every resampling period.
	 * The algorithm terminates if the maximum generation is reached,
	 * or less than the required improvement is seen for several generations.
	 *
	 * @return	the best chromosome.
	 * @throws	IllegalStateException if not initialised.
	 */
	public RollingChromosome<G> run() {
		if (initialised) {
			population = new ArrayList<>(popSize);
			for (int i=0; i<popSize; i++) {
				G randGen = gBuilder.getRandomGenotype(rng);
				RollingChromosome<G> randChrom = new RollingChromosome<>(randGen,bucketSize);
				for (int j=0; j<bucketSize; j++) {
					randChrom.giveFitness(ff.getFitness(randGen));
				}
				randChrom.incrementAge();
				population.add(randChrom);
			}
			Collections.sort(population,comp);
			DoubleList allBestFitnesses = new DoubleList();
			RollingChromosome<G> best = population.get(0);
			double allGensBestFitness = best.getFitness();
			allBestFitnesses.add(allGensBestFitness);
			int gen = 0;
			Debugger.print(String.format("%d-%s\n",gen,best));
			for (gen=1; gen<=maxGen; gen++) {
				ArrayList<RollingChromosome<G>> nextPopulation = new ArrayList<>(popSize);
				int currentPopSize = population.size();
				int survive = Math.max(1,(int) (elitism * currentPopSize));
				for (int i=0; i<survive; i++) {
					RollingChromosome<G> add = population.get(i);
					if (resampleRate > 0 && add.getAge() % resampleRate == 0) {
						for (int j=0; j<resampleSize; j++) {
							add.giveFitness(ff.getFitness(add.getGenotype()));
						}
					}
					add.incrementAge();
					nextPopulation.add(add);
				}
				selectorReproduction.loadPool(population);
				ArrayList<G> offspring = operator.performOperation(selectorReproduction,numOffspring);
				ArrayList<RollingChromosome<G>> selectionPool = new ArrayList<>();
				for (G g : offspring) {
					RollingChromosome<G> rc = new RollingChromosome<>(g,bucketSize);
					for (int i=0; i<bucketSize; i++) {
						rc.giveFitness(ff.getFitness(g));
					}
					rc.incrementAge();
					selectionPool.add(rc);
				}
				if (allowSurvivors) {
					for (int i=survive; i<currentPopSize; i++) {
						RollingChromosome<G> survivor = population.get(i);
						if (resampleRate > 0 && survivor.getAge() % resampleRate == 0) {
							for (int j=0; j<resampleSize; j++) {
								survivor.giveFitness(ff.getFitness(survivor.getGenotype()));
							}
						}
						survivor.incrementAge();
						selectionPool.add(survivor);
					}
				}
				selectorSurvival.loadPool(selectionPool);
				nextPopulation.addAll(selectorSurvival.performSurvivalSelection(popSize - survive));
				population = nextPopulation;
				Collections.sort(population,comp);
				best = population.get(0);
				double currentBestFitness = best.getFitness();
				if ((maximising && currentBestFitness > allGensBestFitness) || (!maximising && currentBestFitness < allGensBestFitness)) {
					allGensBestFitness = currentBestFitness;
				}
				allBestFitnesses.add(allGensBestFitness);
				if (allGensBestFitness == 0 && !maximising) {
					break;
				}
				if (gen > conCutoff) {
					double oldFitness = allBestFitnesses.get(gen - conCutoff);
					double ratio = (maximising ? allGensBestFitness / oldFitness : oldFitness / allGensBestFitness);
					if (ratio <= 1 + improvement) {
						break;
					}
				}
				if (gen % DEBUG_INTERVAL == 0) {
					Debugger.print(String.format("%d(%f)-%s\n",gen,allGensBestFitness,best));
				}
			}
			Debugger.print(String.format("%d-%s\n",gen,best));
			return best;
		}
		else {
			throw new IllegalStateException("Algorithm not initialised");
		}
	}

	/**
	 * Get the population of the final generation of the last run of the EA.
	 * 
	 * @return	a list of chromosomes from the final population.
	 */
	public ArrayList<Chromosome<G>> getPopulation() {
		return new ArrayList<>(population);
	}

}