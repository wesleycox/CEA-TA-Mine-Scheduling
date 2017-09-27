package mines.ea.alg;

import mines.ea.gene.Genotype;
import mines.ea.chrom.Chromosome;
import java.util.*;

/**
 * Evolutionary algorithm class.
 */
public interface EvolutionaryAlgorithm<G extends Genotype> {

	/**
	 * Run the evolutionary algorithm.
	 * 
	 * @return	the best chromosome.
	 */
	public Chromosome<G> run();

	/**
	 * Get the population of the final generation of the last run of the EA.
	 * 
	 * @return	a list of chromosomes from the final population.
	 */
	public List<Chromosome<G>> getPopulation();

}