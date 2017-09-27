package mines.ea.fitness;

import mines.ea.gene.Genotype;

/**
 * Fitness function class for use in coevolutionary algorithms with multiple genotypes.
 */
public interface CoevolutionaryHeterogeneousFitnessFunction<G extends Genotype, H extends Genotype> {

	/**
	 * Evaluate the fitness of a pairing of partial-solution genotypes that form a complete solution.
	 * 
	 * @param	genome1	the first genotype.
	 * @param	genome2	the second genotype.
	 * @return	the fitness of the pairing.
	 */
	public double getFitness(G genome1, H genome2);

	/**
	 * Get whether the objective function is maximising or not.
	 *
	 * @return	true if high fitness is better,
	 *			false otherwise.
	 */
	public boolean isMaximising();
}