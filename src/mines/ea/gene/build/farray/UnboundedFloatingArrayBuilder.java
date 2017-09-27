package mines.ea.gene.build.farray;

import mines.ea.gene.build.GenotypeBuilder;
import mines.ea.gene.FloatingArrayGenotype;
import java.util.*;

/**
 * Random genotype builder for non-negative real coded genotypes.
 */
public class UnboundedFloatingArrayBuilder implements GenotypeBuilder<FloatingArrayGenotype> {

	private int length;				//number of genes.
	private double[] averageValues;	//the average value per gene.

	/**
	 * Genotype builder constructor.
	 *
	 * @param	length			the number of genes.
	 * @param	averageValues	the average value per gene.
	 */
	public UnboundedFloatingArrayBuilder(int length, double[] averageValues) {
		this.length = length;
		this.averageValues = Arrays.copyOf(averageValues,length);
	}

	/**
	 * Get a random genotype of fixed length, 
	 * where each gene value is chosen from an exponential distribution,
	 * with the provided average.
	 *
	 * @param	rng	the RNG to use.
	 * @return	a random FloatingArrayGenotype.
	 */
	public FloatingArrayGenotype getRandomGenotype(Random rng) {
		double[] array = new double[length];
		for (int i=0; i<length; i++) {
			array[i] = averageValues[i] * -Math.log(rng.nextDouble());
		}
		return new FloatingArrayGenotype(array);
	}
}