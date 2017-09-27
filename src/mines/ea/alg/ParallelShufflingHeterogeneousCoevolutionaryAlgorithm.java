package mines.ea.alg;

import mines.ea.gene.Genotype;
import mines.ea.gene.build.GenotypeBuilder;
import mines.ea.fitness.CoevolutionaryHeterogeneousFitnessFunction;
import mines.ea.op.selection.SelectionOperator;
import mines.ea.chrom.*;
import mines.ea.op.gene.GeneticOperator;
import mines.util.DoubleList;
import mines.system.Debugger;
import java.util.*;

/**
 * A two-population coevolutionary algorithm using parallel shuffling for collaboration selection.
 * Each round,
 * both populations reproduce,
 * then the selection pools of both genotypes are randomly sorted to be paired.
 * Fitness of a chromosome is the best fitness of a pairing it has participated in.
 *
 * Based on parallel shuffling method proposed in:
 *
 * Sean Luke, Keith Sullivan, and Faisal Abidi. 
 * Large scale empirical analysis of cooperative coevolution.
 * In Proceedings of the 13th annual conference companion on Genetic and evolutionary computation, pages 151--152. ACM, 2011.
 */
public class ParallelShufflingHeterogeneousCoevolutionaryAlgorithm<G extends Genotype, H extends Genotype> implements 
	HeterogeneousCoevolutionaryAlgorithm<G,H> {

	private static final int DEBUG_INTERVAL = 10;	//the period between debugging messages.

	private int minNumCollaborators;	//the minimum number of collaborations for each chromosome per round.
	private int popSize1;				//surviving population size of first genotype.
	private int popSize2;				//surviving population size of second genotype.
	private int numOffspring1;			//number of offspring per generation of first genotype.
	private int numOffspring2;			//number of offspring per generation of second genotype.
	private int maxGen;					//maximum number of generations.
	private int conCutoff;				//number of generations allowed without required improvement.
	private double improvement;			//required improvement.
	private boolean allowSurvivors;		//whether to allow non-elite chromosomes to survive between generations.

	private GenotypeBuilder<G> gBuilder1;										//random genotype generator for first genotype.
	private GenotypeBuilder<H> gBuilder2;										//random genotype generator for second genotype.
	private CoevolutionaryHeterogeneousFitnessFunction<G,H> ff;					//fitness function.
	private SelectionOperator<G,OptimisticChromosome<G>> selectorReproduction1;	//selection operator for reproduction for first genotype.
	private SelectionOperator<H,OptimisticChromosome<H>> selectorReproduction2;	//selection operator for reproduction for second genotype.
	private SelectionOperator<G,OptimisticChromosome<G>> selectorSurvival1;		//selection operator for survival for first genotype.
	private SelectionOperator<H,OptimisticChromosome<H>> selectorSurvival2;		//selection operator for survival for second genotype.
	private GeneticOperator<G> operator1;										//mutation and crossover operator for first genotype.
	private GeneticOperator<H> operator2;										//mutation and crossover operator for second genotype.

	private Random rng;			//RNG.
	private boolean maximising;	//whether fitness is maximising or not.
	private int maxPopSize;		//maximum of two population sizes.

	private ArrayList<OptimisticChromosome<G>> population1;	//current population of first chromosomes.
	private ArrayList<OptimisticChromosome<H>> population2;	//current population of second chromosomes.

	private boolean initialised;	//whether this algorithm has been initialised yet.

	/**
	 * Constructor to set fundamental variables.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param	minNumCollaborators		the minimum number of collaborations per chromosome per round.
	 * @param	gBuilder1				the first random genotype generator.
	 * @param	gBuilder2				the second random genotype generator.
	 * @param	ff						the fitness function.
	 * @param	selectorReproduction1	the first selection operator for reproduction.
	 * @param	selectorReproduction2	the second selection operator for reproduction.
	 * @param	selectorSurvival1		the first selection operator for survival.
	 * @param	selectorSurvival2		the second selection operator for survival.
	 * @param	operator1				the first mutation and crossover operator.
	 * @param	operator2				the second mutation and crossover operator.
	 */
	public ParallelShufflingHeterogeneousCoevolutionaryAlgorithm(int minNumCollaborators, GenotypeBuilder<G> gBuilder1, 
		GenotypeBuilder<H> gBuilder2, CoevolutionaryHeterogeneousFitnessFunction<G,H> ff, SelectionOperator<G,
		OptimisticChromosome<G>> selectorReproduction1, SelectionOperator<H,OptimisticChromosome<H>> selectorReproduction2, 
		SelectionOperator<G,OptimisticChromosome<G>> selectorSurvival1, SelectionOperator<H,OptimisticChromosome<H>> selectorSurvival2, 
		GeneticOperator<G> operator1, GeneticOperator<H> operator2) {
		this.minNumCollaborators = minNumCollaborators;

		this.gBuilder1 = gBuilder1;
		this.gBuilder2 = gBuilder2;
		this.ff = ff;
		this.selectorReproduction1 = selectorReproduction1;
		this.selectorReproduction2 = selectorReproduction2;
		this.selectorSurvival1 = selectorSurvival1;
		this.selectorSurvival2 = selectorSurvival2;
		this.operator1 = operator1;
		this.operator2 = operator2;

		rng = new Random();
		maximising = ff.isMaximising();

		popSize1 = 100;
		popSize2 = 100;
		numOffspring1 = 100;
		numOffspring2 = 100;
		maxGen = 999;
		conCutoff = 99;
		improvement = 0.0;
		allowSurvivors = true;

		initialised = false;
	}

	/**
	 * Set the population and offspring sizes.
	 * Can only be used before initialisation.
	 *
	 * @param	popSize1		the first population size.
	 * @param	popSize2		the second population size.
	 * @param	numOffspring1	the number of offspring per generation of first genotype.
	 * @param	numOffspring2	the number of offspring per generation of second genotype.
	 * @param	allowSurvivors	whether to allow non-elite chromosomes to survive between generations.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @throws	IllegalArgumentException	if non-positive arguments are given.
	 */
	public ParallelShufflingHeterogeneousCoevolutionaryAlgorithm<G,H> setStrategyParams(int popSize1, int popSize2, int numOffspring1, 
		int numOffspring2, boolean allowSurvivors) {
		if (!initialised) {
			if (popSize1 <= 0 || popSize2 <= 0) {
				throw new IllegalArgumentException(String.format("Positive population sizes required: %d %d",popSize1,popSize2));
			}
			this.popSize1 = popSize1;
			this.popSize2 = popSize2;
			if (numOffspring1 <= 0 || numOffspring2 <= 0) {
				throw new IllegalArgumentException(String.format("Positive offspring sizes required: %d %d",numOffspring1,numOffspring2));
			}
			this.numOffspring1 = numOffspring1;
			this.numOffspring2 = numOffspring2;
			this.allowSurvivors = allowSurvivors;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
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
	public ParallelShufflingHeterogeneousCoevolutionaryAlgorithm<G,H> setTerminationParams(int maxGen, int conCutoff, double improvement) {
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
	public ParallelShufflingHeterogeneousCoevolutionaryAlgorithm<G,H> initialise() {
		if (!initialised) {
			maxPopSize = Math.max(popSize1,popSize2);
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Algorithm already initialised");
		}
	}

	/**
	 * Run the coevolutionary algorithm and return the best observed pairing of chromosomes.
	 * The operation is as follows:
	 * Each population is initialised randomly.
	 * Two populations have fitness assigned by:
	 * Shuffling each population,
	 * reshuffling if every chromosome has been used,
	 * pairing each chromosome with a corresponding chromosome from the other population,
	 * until every chromosome in the larger population has participated in the minimum number of collaborations.
	 * Fitness of a chromosome is the best fitness of any pairing it has participated in.
	 * For each round,
	 * each population produces offspring by their genetic operator,
	 * and the selection pools are evaluated for fitness,
	 * then chromosomes are chosen for survival.
	 * The algorithm terminates if the maximum generation is reached,
	 * or less than the required improvement is seen for several generations.
	 *
	 * @return	the best observed chromosome pairing.
	 * @throws	IllegalStateException if not initialised.
	 */
	public ChromosomePairing<G,H> run() {
		if (initialised) {
			population1 = new ArrayList<>(popSize1);
			for (int i=0; i<popSize1; i++) {
				G randGen = gBuilder1.getRandomGenotype(rng);
				OptimisticChromosome<G> randChrom = new OptimisticChromosome<>(randGen,maximising);
				randChrom.incrementAge();
				population1.add(randChrom);
			}
			population2 = new ArrayList<>(popSize2);
			for (int i=0; i<popSize2; i++) {
				H randGen = gBuilder2.getRandomGenotype(rng);
				OptimisticChromosome<H> randChrom = new OptimisticChromosome<>(randGen,maximising);
				randChrom.incrementAge();
				population2.add(randChrom);
			}
			DoubleList allBestFitnesses = new DoubleList();
			int gen = 0;
			ChromosomePairing<G,H> best = null;
			double bestFitness = (maximising ? 0 : Double.MAX_VALUE);
			for (int i=0; i<minNumCollaborators; i++) {
				Collections.shuffle(population1);
				Collections.shuffle(population2);
				for (int j=0; j<maxPopSize; j++) {
					OptimisticChromosome<G> c1 = population1.get((maxPopSize * i + j) % popSize1);
					OptimisticChromosome<H> c2 = population2.get((maxPopSize * i + j) % popSize2);
					double fitness = ff.getFitness(c1.getGenotype(),c2.getGenotype());
					c1.giveFitness(fitness);
					c2.giveFitness(fitness);
					if (maximising) {
						if (fitness > bestFitness) {
							best = new ChromosomePairing<>(c1,c2);
							bestFitness = fitness;
						}
					}
					else if (fitness < bestFitness) {
						best = new ChromosomePairing<>(c1,c2);
						bestFitness = fitness;
					}
				}
			}
			allBestFitnesses.add(bestFitness);
			Debugger.print(String.format("%d-%f-%s\n",gen,bestFitness,best));
			for (gen=1; gen<maxGen; gen++) {
				ArrayList<OptimisticChromosome<G>> selectionPool1 = new ArrayList<>();
				selectorReproduction1.loadPool(population1);
				ArrayList<G> offspring1 = operator1.performOperation(selectorReproduction1,numOffspring1);
				for (G g : offspring1) {
					OptimisticChromosome<G> oc = new OptimisticChromosome<>(g,maximising);
					oc.incrementAge();
					selectionPool1.add(oc);
				}
				if (allowSurvivors) {
					for (OptimisticChromosome<G> survivor : population1) {
						survivor.incrementAge();
						selectionPool1.add(survivor);
					}
				}
				int poolSize1 = selectionPool1.size();
				ArrayList<OptimisticChromosome<H>> selectionPool2 = new ArrayList<>();
				selectorReproduction2.loadPool(population2);
				ArrayList<H> offspring2 = operator2.performOperation(selectorReproduction2,numOffspring2);
				for (H h : offspring2) {
					OptimisticChromosome<H> oc = new OptimisticChromosome<>(h,maximising);
					oc.incrementAge();
					selectionPool2.add(oc);
				}
				if (allowSurvivors) {
					for (OptimisticChromosome<H> survivor : population2) {
						survivor.incrementAge();
						selectionPool2.add(survivor);
					}
				}
				int poolSize2 = selectionPool2.size();
				int maxPoolSize = Math.max(poolSize1,poolSize2);
				for (int i=0; i<minNumCollaborators; i++) {
					Collections.shuffle(selectionPool1);
					Collections.shuffle(selectionPool2);
					for (int j=0; j<maxPoolSize; j++) {
						OptimisticChromosome<G> c1 = selectionPool1.get((maxPoolSize * i + j) % poolSize1);
						OptimisticChromosome<H> c2 = selectionPool2.get((maxPoolSize * i + j) % poolSize2);
						double fitness = ff.getFitness(c1.getGenotype(),c2.getGenotype());
						c1.giveFitness(fitness);
						c2.giveFitness(fitness);
						if (maximising) {
							if (fitness > bestFitness) {
								best = new ChromosomePairing<>(c1,c2);
								bestFitness = fitness;
							}
						}
						else if (fitness < bestFitness) {
							best = new ChromosomePairing<>(c1,c2);
							bestFitness = fitness;
						}
					}
				}
				selectorSurvival1.loadPool(selectionPool1);
				population1.clear();
				population1.addAll(selectorSurvival1.performSurvivalSelection(popSize1));
				selectorSurvival2.loadPool(selectionPool2);
				population2.clear();
				population2.addAll(selectorSurvival2.performSurvivalSelection(popSize2));
				allBestFitnesses.add(bestFitness);
				if (bestFitness == 0 && !maximising) {
					break;
				}
				if (gen > conCutoff) {
					double oldFitness = allBestFitnesses.get(gen - conCutoff);
					double ratio = (maximising ? bestFitness / oldFitness : oldFitness / bestFitness);
					if (ratio <= 1 + improvement) {
						break;
					}
				}
				if (gen % DEBUG_INTERVAL == 0) {
					Debugger.print(String.format("%d-%f-%s\n",gen,bestFitness,best));
				}
			}
			Debugger.print(String.format("%d-%f-%s\n",gen,bestFitness,best));
			return best;
		}
		else {
			throw new IllegalStateException("Algorithm not initialised");
		}
	}

	/**
	 * Get the population of the first genotype of the final generation of the last run of the CEA.
	 * 
	 * @param	index	unused.
	 *
	 * @return	the first list of chromosomes from the final population.
	 */
	public List<? extends Chromosome<G>> getFirstPopulation(int index) {
		return population1;
	}

	/**
	 * Get the population of the second genotype of the final generation of the last run of the CEA.
	 * 
	 * @param	index	unused.
	 *
	 * @return	the second list of chromosomes from the final population.
	 */
	public List<? extends Chromosome<H>> getSecondPopulation(int index) {
		return population2;
	}
	
}