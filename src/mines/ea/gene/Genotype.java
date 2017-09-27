package mines.ea.gene;

/**
 * Genotype interface.
 */
public interface Genotype {

	/**
	 * Equality test.
	 * Expected to be based on genes.
	 *
	 * @param	other	the Object to compare with
	 * @return	true if other is equal to this,
	 *			false otherwise
	 */
	public boolean equals(Object other);

	/**
	 * Create a new identical genotype.
	 *
	 * @return	a Genotype with identical genes to this.
	 */
	public Genotype clone();

	/**
	 * Get the hash code based on the gene values.
	 * Should satisfy the general contract of hash codes.
	 *
	 * @return	the hash code.
	 */
	public int hashCode();

	/**
	 * Get the string form of the gene values.
	 *
	 * @param	a String format.
	 */
	public String toString();

}