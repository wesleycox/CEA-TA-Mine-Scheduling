package mines.ea.op.selection;

import mines.ea.gene.Genotype;
import mines.ea.chrom.Chromosome;
import java.util.*;

/**
 * Fitness proportionate selection operator.
 * 
 * Uses the stochastic acceptance method described in:
 *
 * Lipowski, A., & Lipowska, D. (2012). 
 * Roulette-wheel selection via stochastic acceptance. 
 * Physica A: Statistical Mechanics and its Applications, 391(6), 2193-2196.
 */
public class FitnessProportionateReproductionOperator<G extends Genotype, C extends Chromosome<G>> extends SelectionOperator<G,C> {

	private boolean maximising;	//whether the fitness function is maximising or not.

	private Random rng;	//the RNG.

	private int[] indexes;						//the indexes of chromosomes from the selection pool.
	private int poolSize;						//the number of chromosomes in the selection pool.
	private int parentNum;						//the number of distinct parents outputted since last refresh.
	private TreeMap<Double,Integer> fitnesses;	//the number of each fitness.

	/**
	 * Selection operator constructor.
	 *
	 * @param	maximising	whether the fitness function is maximising or not.
	 */
	public FitnessProportionateReproductionOperator(boolean maximising) {
		super();

		this.maximising = maximising;

		rng = new Random();

		fitnesses = new TreeMap<>();
	}

	/**
	 * Create a list of surviving chromosomes from the selection pool.
	 * Roulette-wheel selection with stochastic acceptance is used.
	 *
	 * @param	selectionSize	the number of survivors.
	 * @return	a list of chromosomes -
	 *			the entire selection pool if the pool size is less than the selection size.
	 */
	public List<C> performSurvivalSelection(int selectionSize) {
		// throw new UnsupportedOperationException("Operator only supports reproduction selection");
		if (poolSize <= selectionSize) {
			return new ArrayList<>(pool);
		}
		else {
			ArrayList<C> out = new ArrayList<>(selectionSize);
			for (int i=0; i<selectionSize; i++) {
				out.add(performReproductionSelection(i == 0));
			}
			return out;
		}
	}

	/**
	 * Load a new selection pool.
	 *
	 * @param	p	the pool of candidates.
	 */
	@Override
	public void loadPool(List<C> p) {
		super.loadPool(p);
		poolSize = this.pool.size();
		indexes = new int[poolSize];
		for (int i=0; i<poolSize; i++) {
			indexes[i] = i;
		}
		parentNum = 0;
		fitnesses.clear();
		for (int i=0; i<poolSize; i++) {
			double f = pool.get(i).getFitness();
			fitnesses.put(f,fitnesses.getOrDefault(f,0) + 1);
		}
	}

	/**
	 * Select a single parent chromosome.
	 * Roulette-wheel selection with stochastic acceptance is used.
	 * If a refresh is not requested, 
	 * the chromosome will be distinct from all previous output since the last refresh.
	 *
	 * @param	refresh	whether to clear the output history.
	 * @return	a chromosome.
	 * @throws	IllegalArgumentException	if all chromosomes have been returned since the last refresh.
	 */
	public C performReproductionSelection(boolean refresh) {
		if (refresh) {
			for (int i=0; i<parentNum; i++) {
				double f = pool.get(indexes[i]).getFitness();
				fitnesses.put(f,fitnesses.getOrDefault(f,0) + 1);
			}
			parentNum = 0;
		}
		if (parentNum >= poolSize) {
			throw new IllegalArgumentException("Returned all elements without refresh");
		}
		double bestFitness = (maximising ? fitnesses.lastKey() : fitnesses.firstKey());
		while (true) {
			int rInd = rng.nextInt(poolSize - parentNum) + parentNum;
			C possible = pool.get(indexes[rInd]);
			double possibleFitness = possible.getFitness();
			double acceptance;
			if (maximising) {
				acceptance = possibleFitness / bestFitness;
			}
			else if (bestFitness == 0) {
				acceptance = (possibleFitness == 0 ? 1 : 0);
			}
			else {
				acceptance = bestFitness / possibleFitness;
			}
			if (rng.nextDouble() < acceptance) {
				int temp = indexes[rInd];
				indexes[rInd] = indexes[parentNum];
				indexes[parentNum] = temp;
				int count = fitnesses.get(possibleFitness);
				if (count == 1) {
					fitnesses.remove(possibleFitness);
				}
				else {
					fitnesses.put(possibleFitness,count - 1);
				}
				parentNum++;
				return possible;
			}
		}
	}

	/**
	 * Get whether at least n chromosomes are still unselected since the last refresh.
	 *
	 * @param	n	the number of chromosomes needed.
	 * @return	true if performReproductionSelection(false) can be called n more times,
	 *			false otherwise
	 */
	public boolean hasUnselected(int n) {
		return poolSize - parentNum >= n;
	}
	
}