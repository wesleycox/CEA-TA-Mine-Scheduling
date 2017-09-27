package mines.ea.gene.build;

import mines.ea.gene.Genotype;
import java.util.Random;

/**
 * Interface for constructing random genotypes to initialise a population in an evolutionary algorithm.
 */
public interface GenotypeBuilder<G extends Genotype> {

	/**
	 * Get a random genotype.
	 *
	 * @param	rng	the RNG to use.
	 * @return	a random genotype.
	 */
	public G getRandomGenotype(Random rng);
}