package mines.ea.chrom;

import mines.ea.gene.Genotype;

/**
 * Chromosome class for use in evolutionary algorithms.
 */
public class Chromosome<G extends Genotype> implements Comparable<Chromosome> {

	private G genome;		//genotype of this chromosome.
	private double fitness;	//fitness of the chromosome.
	private int age;		//number of generations this chromosome has survived.

	/**
	 * Constructs a chromosome with 0 fitness and age 0,
	 * with the given genotype.
	 *
	 * @param	genome	the genotype of this chromosome.
	 */
	public Chromosome(G genome) {
		this.genome = genome;
		fitness = 0;
		age = 0;
	}

	/**
	 * Get the fitness value of this chromosome.
	 *
	 * @return	the current fitness.
	 */
	public double getFitness() {
		return fitness;
	}

	/**
	 * Get the age of this chromosome.
	 *
	 * @return	the current age.
	 */
	public int getAge() {
		return age;
	}

	/**
	 * Set the fitness value for this chromosome.
	 *
	 * @param	f	the new fitness value.
	 * @throws	IllegalArgumentException	if negative fitness.
	 */
	public void setFitness(double f) {
		if (f < 0) {
			throw new IllegalArgumentException(String.format("Non-negative fitness required: %f",f));
		}
		fitness = f;
	}

	/**
	 * Increment the age of this chromosome by 1.
	 */
	public void incrementAge() {
		age++;
	}

	/**
	 * Get the genotype of this chromosome.
	 *
	 * @return	the genotype.
	 */
	public G getGenotype() {
		return genome;
	}

	/**
	 * Tests equality of genotypes.
	 *
	 * @param	other	the Object to compare to.
	 * @return	true if equal, false otherwise.
	 */
	public boolean equals(Object o) {
		try {
			@SuppressWarnings("unchecked")
			Chromosome<G> other = (Chromosome<G>) o;
			return other.getGenotype().equals(this.genome);
		}
		catch (ClassCastException cce) {
			return false;
		}
	}

	/**
	 * Get the hash code of the genotype.
	 *
	 * @return	the hash code.
	 */
	public int hashCode() {
		return genome.hashCode();
	}

	/**
	 * Get a string format of this chromosome.
	 *
	 * @return	a String of the form Aa-Ff-Gg,
	 *			where a is the age,
	 *			f is the fitness,
	 *			and g is a string form of the genotype.
	 */
	public String toString() {
		return String.format("A%d-F%f-G%s",getAge(),getFitness(),genome.toString());
	}

	/**
	 * Compares this chromosome with another based on fitness.
	 *
	 * @param	other	the Chromosome to compare to
	 * @return	negative if this has lower fitness than the other, 
	 *			positive if this has higher fitness than the other, 
	 *			0 if fitness is equal.
	 */
	public int compareTo(Chromosome other) {
		return Double.compare(this.getFitness(),other.getFitness());
	}

}