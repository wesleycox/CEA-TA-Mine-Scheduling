package mines.ea.chrom;

import mines.ea.gene.Genotype;
import mines.util.DoubleQueue;

/**
 * A chromosome that sets fitness based on a rolling average of fitness values.
 */
public class RollingChromosome<G extends Genotype> extends Chromosome<G> {

	private DoubleQueue fitnesses;	//the current fitness bucket.
	private double totalFitness;	//the sum of the fitness bucket.
	private int bucketSize;			//the size of the fitness bucket.

	/**
	 * Constructs a chromosome with 0 unset fitness and age 0,
	 * with the given genotype,
	 * with the given fitness bucket size.
	 *
	 * @param	genome		the genotype of this chromosome.
	 * @param	bucketSize	the maximum number of fitnesses in the fitness bucket.
	 */
	public RollingChromosome(G genome, int bucketSize) {
		super(genome);
		fitnesses = new DoubleQueue();
		totalFitness = 0;
		this.bucketSize = bucketSize;
	}

	/**
	 * Empty the fitness bucket and insert a single value.
	 *
	 * @param	f	the new fitness.
	 * @throws	IllegalArgumentException	if f is negative.
	 */
	@Override
	public void setFitness(double f) {
		if (f < 0) {
			throw new IllegalArgumentException(String.format("Non-negative fitness required: %f",f));
		}
		fitnesses.clear();
		fitnesses.add(f);
		totalFitness = f;
	}

	/**
	 * Enter a single value into the fitness bucket.
	 * An old fitness value will be removed if the bucket is full.
	 *
	 * @param	f	the new value.
	 * @throws	IllegalArgumentException	if f is negative.
	 */
	public void giveFitness(double f) {
		if (f < 0) {
			throw new IllegalArgumentException(String.format("Non-negative fitness required: %f",f));
		}
		fitnesses.add(f);
		totalFitness += f;
		while (fitnesses.size() > bucketSize) {
			double old = fitnesses.poll();
			totalFitness -= old;
		}
	}

	/**
	 * Get the average of the fitness bucket.
	 *
	 * @return	the current fitness.
	 */
	@Override
	public double getFitness() {
		return totalFitness / fitnesses.size();
	}

}