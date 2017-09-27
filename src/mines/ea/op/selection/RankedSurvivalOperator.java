package mines.ea.op.selection;

import mines.ea.gene.Genotype;
import mines.ea.chrom.Chromosome;
import java.util.*;

/**
 * Elitist selection operator.
 */
public class RankedSurvivalOperator<G extends Genotype, C extends Chromosome<G>> extends SelectionOperator<G,C> {

	private Comparator<Chromosome> comp;	//chromosome comparison based on fitness.

	private int poolSize;	//the number of chromosomes in the selection pool.

	/**
	 * Selection operator constructor.
	 *
	 * @param	maximising	whether the fitness function is maximising or not.
	 */
	public RankedSurvivalOperator(boolean maximising) {
		super();
		comp = new Comparator<Chromosome>() {
			public int compare(Chromosome c1, Chromosome c2) {
				int diff = (maximising ? -1 : 1) * c1.compareTo(c2);
				return (diff == 0 ? (c2.getAge() - c1.getAge()) : diff);
			}
		};
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
		Collections.sort(pool,comp);
	}

	/**
	 * Create a list of surviving chromosomes from the selection pool.
	 * The best chromosomes are chosen (elitist selection).
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
			return pool.subList(0,selectionSize);
		}
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	public C performReproductionSelection(boolean refresh) {
		throw new UnsupportedOperationException("Operator only supports survival selection");
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	public boolean hasUnselected(int n) {
		throw new UnsupportedOperationException("Operator only supports survival selection");
	}
	
}