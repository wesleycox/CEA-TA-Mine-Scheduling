package mines.ea.gene;

import java.util.*;

/**
 * Integer string genotype.
 */
public class ArrayGenotype implements Genotype {

	private int[] string;	//genotype array.

	/**
	 * Genotype constructor.
	 *
	 * @param	string	the array genotype.
	 */
	public ArrayGenotype(int[] string) {
		this.string = Arrays.copyOf(string,string.length);
	}

	/**
	 * Equality test based on genes.
	 *
	 * @param	other	the Object to compare with
	 * @return	true if other is an ArrayGenotype with equal genes to this,
	 *			false otherwise
	 */
	public boolean equals(Object other) {
		if (other instanceof ArrayGenotype) {
			return Arrays.equals(((ArrayGenotype) other).string,this.string);
		}
		return false;
	}

	/**
	 * Create a new identical genotype.
	 *
	 * @return	an ArrayGenotype with identical genes to this.
	 */
	public ArrayGenotype clone() {
		return new ArrayGenotype(string);
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
	public int[] getArray() {
		return Arrays.copyOf(string,string.length);
	}

	/**
	 * Get a copy of the genotype array.
	 *
	 * @param	dest	the array to copy onto.
	 */
	public void getArray(int[] dest) {
		System.arraycopy(string,0,dest,0,string.length);
	}

	/**
	 * Get the number of genes.
	 *
	 * @return	the genotype length.
	 */
	public int size() {
		return string.length;
	}
}