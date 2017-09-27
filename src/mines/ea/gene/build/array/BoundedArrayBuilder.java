package mines.ea.gene.build.array;

import mines.ea.gene.ArrayGenotype;
import mines.ea.gene.build.GenotypeBuilder;
import java.util.*;

/**
 * Random genotype builder for integer genotypes bounded by 0 and upper limits.
 */
public class BoundedArrayBuilder implements GenotypeBuilder<ArrayGenotype> {

	private int length;			//number of genes.
	private int[] maxValues;	//the maximum value per gene, exclusive.

	/**
	 * Genotype builder constructor.
	 *
	 * @param	length		the number of genes.
	 * @param	maxValues	the maximum value per gene, exclusive.
	 */
	public BoundedArrayBuilder(int length, int[] maxValues) {
		this.length = length;
		this.maxValues = Arrays.copyOf(maxValues,length);
	}

	/**
	 * Get a random genotype of fixed length, 
	 * where each gene value is uniformly chosen between 0 (inclusive),
	 * and the maximum values (exclusive).
	 *
	 * @param	rng	the RNG to use.
	 * @return	a random ArrayGenotype.
	 */
	public ArrayGenotype getRandomGenotype(Random rng) {
		int[] array = new int[length];
		for (int i=0; i<length; i++) {
			array[i] = rng.nextInt(maxValues[i]);
		}
		return new ArrayGenotype(array);
	}
}