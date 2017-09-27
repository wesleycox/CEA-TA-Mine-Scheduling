package mines.ea.chrom;

import mines.ea.gene.Genotype;
import java.util.*;

/**
 * A grouping of chromosomes of two different genotypes.
 */
public class ChromosomePairing<A extends Genotype, B extends Genotype> {

	private List<? extends Chromosome<A>> first;	//the list of first chromosomes.
	private List<? extends Chromosome<B>> second;	//the list of second chromosomes.

	/**
	 * Constructs a pairing with one chromosome of each type.
	 *
	 * @param	first	the first chromosome.
	 * @param	second	the second chromosome.
	 */
	public ChromosomePairing(Chromosome<A> first, Chromosome<B> second) {
		this.first = Collections.singletonList(first);
		this.second = Collections.singletonList(second);
	}


	/**
	 * Constructs a pairing with multiple chromosomes of each type.
	 *
	 * @param	first	the list of first chromosomes.
	 * @param	second	the list of second chromosomes.
	 */
	public ChromosomePairing(List<? extends Chromosome<A>> first, List<? extends Chromosome<B>> second) {
		// this.first = first;
		// this.second = second;
		this.first = new ArrayList<>(first);
		this.second = new ArrayList<>(second);
	}

	/**
	 * Get the first chromosome of the first type.
	 *
	 * @return	a Chromosome<A>
	 */
	public Chromosome<A> getFirst() {
		return getFirst(0);
	}

	/**
	 * Get a chromosome of the first type.
	 *
	 * @param	ind	the index of the chromosome from the first list.
	 * @return	a Chromosome<A>
	 */
	public Chromosome<A> getFirst(int ind) {
		return first.get(ind);
	}

	/**
	 * Get the first chromosome of the second type.
	 *
	 * @return	a Chromosome<B>
	 */
	public Chromosome<B> getSecond() {
		return getSecond(0);
	}

	/**
	 * Get a chromosome of the second type.
	 *
	 * @param	ind	the index of the chromosome from the second list.
	 * @return	a Chromosome<B>
	 */
	public Chromosome<B> getSecond(int ind) {
		return second.get(ind);
	}

	/**
	 * Get a string form of the pairing.
	 *
	 * @return	a String form of this pairing of the first L1-L2,
	 *			where Ln is the string form of each chromosome in the nth list,
	 *			separated by -'s.
	 */
	public String toString() {
		return String.format("%s-%s",toString(first),toString(second));
	}

	private static <T extends Genotype> String toString(List<? extends Chromosome<T>> list) {
		boolean start = false;
		StringBuilder build = new StringBuilder();
		for (Chromosome<T> c : list) {
			if (start) {
				build.append("-");
			}
			start = true;
			build.append(c.getGenotype().toString());
		}
		return build.toString();
	}
}