package mines.ea.chrom;

import mines.ea.gene.Genotype;

/**
 * A chromosome where current fitness is best given.
 */
public class OptimisticChromosome<G extends Genotype> extends Chromosome<G> {

	private boolean isSet;		//whether the fitness has been set.
	private double fitness;		//fitness of the chromosome.
	private boolean maximising;	//whether fitness is maximising.

	/**
	 * Constructs a chromosome with 0 unset fitness and age 0,
	 * with the given genotype.
	 *
	 * @param	genome		the genotype of this chromosome.
	 * @param	maximising	whether fitness is maximising.
	 */
	public OptimisticChromosome(G genome, boolean maximising) {
		super(genome);
		isSet = false;
		fitness = 0;
		this.maximising = maximising;
	}

	/**
	 * Set the fitness, overriding any previous value.
	 *
	 * @param	f	the new fitness
	 * @throws	IllegalArgumentException	if f is negative.
	 */
	@Override
	public void setFitness(double f) {
		if (f < 0) {
			throw new IllegalArgumentException(String.format("Non-negative fitness required: %f",f));
		}
		fitness = f;
		isSet = true;
	}

	/**
	 * Offer a new fitness value,
	 * setting the fitness to be the better of the old and new values.
	 *
	 * @param	f	the new fitness
	 * @throws	IllegalArgumentException	if f is negative.
	 */
	public void giveFitness(double f) {
		if (f < 0) {
			throw new IllegalArgumentException(String.format("Non-negative fitness required: %f",f));
		}
		if (!isSet) {
			fitness = f;
		}
		else if (maximising) {
			fitness = Math.max(fitness,f);
		}
		else {
			fitness = Math.min(fitness,f);
		}
		isSet = true;
	}

	/**
	 * Get the fitness value of this chromosome.
	 *
	 * @return	the current fitness.
	 */
	@Override
	public double getFitness() {
		return fitness;
	}

}