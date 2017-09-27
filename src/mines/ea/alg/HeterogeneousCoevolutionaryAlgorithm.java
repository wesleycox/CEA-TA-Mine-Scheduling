package mines.ea.alg;

import mines.ea.gene.Genotype;
import mines.ea.chrom.*;
import java.util.*;

/**
 * Coevolutionary algorithm class for two different genotypes.
 */
public interface HeterogeneousCoevolutionaryAlgorithm<G extends Genotype, H extends Genotype> {

	/**
	 * Run the evolutionary algorithm.
	 * 
	 * @return	the best observed pairing of chromosomes.
	 */
	public ChromosomePairing<G,H> run();

	/**
	 * Get a population of the first genotype of the final generation of the last run of the CEA.
	 * 
	 * @param	index	the index of the population if multiple populations per genotype are used.
	 *
	 * @return	a list of chromosomes from the final population.
	 */
	public List<? extends Chromosome<G>> getFirstPopulation(int index);

	/**
	 * Get a population of the second genotype of the final generation of the last run of the CEA.
	 * 
	 * @param	index	the index of the population if multiple populations per genotype are used.
	 *
	 * @return	a list of chromosomes from the final population.
	 */
	public List<? extends Chromosome<H>> getSecondPopulation(int index);

}