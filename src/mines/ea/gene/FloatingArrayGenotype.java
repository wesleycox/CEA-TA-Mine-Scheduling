package mines.ea.gene;

import java.util.*;

/**
 * Real coded string genotype.
 */
public class FloatingArrayGenotype implements Genotype {

	private double[] string;	//genotype array.

	/**
	 * Genotype constructor.
	 *
	 * @param	string	the array genotype.
	 */
	public FloatingArrayGenotype(double[] string) {
		this.string = Arrays.copyOf(string,string.length);
	}

	/**
	 * Equality test based on genes.
	 *
	 * @param	other	the Object to compare with
	 * @return	true if other is a FloatingArrayGenotype with equal genes to this,
	 *			false otherwise
	 */
	public boolean equals(Object other) {
		if (other instanceof FloatingArrayGenotype) {
			return Arrays.equals(((FloatingArrayGenotype) other).string,this.string);
		}
		return false;
	}

	/**
	 * Create a new identical genotype.
	 *
	 * @return	a FloatingArrayGenotype with identical genes to this.
	 */
	public FloatingArrayGenotype clone() {
		return new FloatingArrayGenotype(string);
	}

	/**
	 * Get the hash code for the genotype array.
	 *
	 * @return	the hash code.
	 */
	public int hashCode() {
		return Arrays.hashCode(string);
	}

	/**
	 * Get the string form of the genotype array.
	 *
	 * @param	a String format of an array.
	 */
	public String toString() {
		return String.format("%s",Arrays.toString(string));
	}

	/**
	 * Get a copy of the genotype array.
	 *
	 * @return	the genotype array.
	 */
	public double[] getArray() {
		return Arrays.copyOf(string,string.length);
	}
}