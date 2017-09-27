package mines.ea.op.selection;

import mines.ea.gene.Genotype;
import mines.ea.chrom.Chromosome;
import java.util.*;

/**
 * Tournament selection operator with tournament sizes of any value.
 */
public class KTournamentSelectionOperator<G extends Genotype, C extends Chromosome<G>> extends SelectionOperator<G,C> {

	private int k;	//tournament size.

	private Comparator<Chromosome> comp;	//chromosome comparison based on fitness.
	private Random rng;						//the RNG.

	private int[] indexes;	//the indexes of chromosomes from the selection pool.
	private int poolSize;	//the number of chromosomes in the selection pool.
	private int parentNum;	//the number of distinct parents outputted since last refresh.

	/**
	 * Selection operator constructor.
	 *
	 * @param	maximising	whether the fitness function is maximising or not.
	 * @param	k			the tournament size.
	 * @throws	IllegalArgumentException	if k is less than 2.
	 */
	public KTournamentSelectionOperator(boolean maximising, int k) {
		super();
		comp = new Comparator<Chromosome>() {
			public int compare(Chromosome c1, Chromosome c2) {
				int diff = (maximising ? -1 : 1) * c1.compareTo(c2);
				return (diff == 0 ? (c2.getAge() - c1.getAge()) : diff);
			}
		};
		rng = new Random();
		if (k < 2) {
			throw new IllegalArgumentException(String.format("Tournament size must be at least 2: %d",k));
		}
		this.k = k;
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
	}

	/**
	 * Create a list of surviving chromosomes from the selection pool.
	 * Tournament selection with a fixed tournament size is used.
	 *
	 * @param	selectionSize	the number of survivors.
	 * @return	a list of chromosomes -
	 *			the entire selection pool if the pool size is less than the selection size.
	 */
	public List<C> performSurvivalSelection(int selectionSize) {
		if (poolSize <= selectionSize) {
			return new ArrayList<>(pool);
		}
		else {
			ArrayList<C> out = new ArrayList<>(selectionSize);
			for (int i=0; i<selectionSize; i++) {
				int ind = tournament(i);
				swap(i,ind);
				out.add(pool.get(indexes[i]));
			}
			return out;
		}
	}

	/**
	 * Select a single parent chromosome.
	 * Tournament selection with a fixed tournament size is used.
	 * If a refresh is not requested, 
	 * the chromosome will be distinct from all previous output since the last refresh.
	 *
	 * @param	refresh	whether to clear the output history.
	 * @return	a chromosome.
	 * @throws	IllegalArgumentException	if all chromosomes have been returned since the last refresh.
	 */
	public C performReproductionSelection(boolean refresh) {
		if (refresh) {
			parentNum = 0;
		}
		if (parentNum >= poolSize) {
			throw new IllegalArgumentException("Returned all elements without refresh");
		}
		int ind = tournament(parentNum);
		swap(parentNum,ind);
		return pool.get(indexes[parentNum]);
	}

	/**
	 * Perform a tournament on the unselected chromosomes.
	 *
	 * @param	locked	the number of selected chromosomes.
	 * @return	the index for the index array of the winner.
	 */
	private int tournament(int locked) {
		if (locked >= poolSize) {
			throw new IllegalArgumentException("Tournament requested on empty space");
		}
		if (poolSize - locked <= k) {
			int best = locked;
			for (int i=locked + 1; i<poolSize; i++) {
				if (comp.compare(pool.get(indexes[best]),pool.get(indexes[i])) > 0) {
					best = i;
				}
			}
			return best;
		}
		else {
			for (int i=0; i<k; i++) {
				int r = rng.nextInt(poolSize - locked - i) + locked;
				swap(i + locked,r);
			}
			int best = locked;
			for (int i=1; i<k; i++) {
				if (comp.compare(pool.get(indexes[best]),pool.get(indexes[i + locked])) > 0) {
					best = i + locked;
				}
			}
			return best;
		}
	}

	/**
	 * Swap two indexes in the index array.
	 *
	 * @param	i	the first index for the index array.
	 * @param	j	the second index for the index array.
	 */
	private void swap(int i, int j) {
		int temp = indexes[i];
		indexes[i] = indexes[j];
		indexes[j] = temp;
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