package mines.ea.fitness;

import mines.ea.gene.Genotype;

/**
 * Fitness function class for use in evolutionary algorithms.
 */
public interface FitnessFunction<G extends Genotype> {

	/**
	 * Evaluate the fitness of a genotype representing a solution.
	 * 
	 * @param	genome	the genotype to evaluate.
	 * @return	the fitness of the genotype.
	 */
	public double getFitness(G genome);

	/**
	 * Get whether the objective function is maximising or not.
	 *
	 * @return	true if high fitness is better,
	 *			false otherwise.
	 */
	public boolean isMaximising();
}