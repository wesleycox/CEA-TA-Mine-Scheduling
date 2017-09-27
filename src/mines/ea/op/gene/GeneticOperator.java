package mines.ea.op.gene;

import mines.ea.gene.Genotype;
import mines.ea.op.selection.SelectionOperator;
import mines.ea.chrom.Chromosome;
import java.util.ArrayList;

/**
 * Reproduction operators including crossover and mutation.
 */
public interface GeneticOperator<G extends Genotype> {

	/**
	 * Produce a list of offspring,
	 * using the provided selection method for choosing parents,
	 * and applying operators.
	 *
	 * @param	selector		a selection scheme with a pool of parent chromosomes.
	 * @param	numOffspring	the minimum number of children required.
	 * @return	an ArrayList of children genotypes.
	 */
	public ArrayList<G> performOperation(SelectionOperator<G,? extends Chromosome<G>> selector, int numOffspring);

}