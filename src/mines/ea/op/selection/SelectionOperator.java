package mines.ea.op.selection;

import mines.ea.gene.Genotype;
import mines.ea.chrom.Chromosome;
import java.util.*;

/**
 * Abstract selection operator used for reproduction and survival selection.
 */
public abstract class SelectionOperator<G extends Genotype, C extends Chromosome<G>> {

	protected ArrayList<C> pool;	//the selection pool.
	private HashSet<G> genomeSet;	//the set of genomes.

	/**
	 * Selection operator constructor.
	 */
	protected SelectionOperator() {
		this.pool = new ArrayList<>();
		genomeSet = new HashSet<>();
	}

	/**
	 * Load a new selection pool.
	 *
	 * @param	pool	the pool of candidates.
	 */
	public void loadPool(List<C> pool) {
		this.pool.clear();
		this.pool.addAll(pool);
		genomeSet.clear();
		for (C c : pool) {
			genomeSet.add(c.getGenotype());
		}
	}

	/**
	 * Create a list of surviving chromosomes from the selection pool.
	 *
	 * @param	selectionSize	the number of survivors.
	 * @return	a list of chromosomes.
	 */
	public abstract List<C> performSurvivalSelection(int selectionSize);

	/**
	 * Select a single parent chromosome.
	 * If a refresh is not requested, 
	 * the chromosome should be distinct from all previous output since the last refresh.
	 *
	 * @param	refresh	whether to clear the output history.
	 * @return	a chromosome.
	 */
	public abstract C performReproductionSelection(boolean refresh);

	/**
	 * Get whether one of the chromosomes in the selection pool has a certain genotype.
	 *
	 * @param	genome	the genotype.
	 * @return	true if the selection pool contains the genotype,
	 *			false otherwise.
	 */
	public boolean contains(G genome) {
		return genomeSet.contains(genome);
	}

	/**
	 * Get whether at least n chromosomes are still unselected since the last refresh.
	 *
	 * @param	n	the number of chromosomes needed.
	 * @return	true if performReproductionSelection(false) can be called n more times,
	 *			false otherwise
	 */
	public abstract boolean hasUnselected(int n);

}