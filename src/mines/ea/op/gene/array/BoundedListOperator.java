package mines.ea.op.gene.array;

import mines.ea.op.gene.*;
import mines.ea.gene.ArrayGenotype;
import mines.ea.op.selection.*;
import mines.ea.chrom.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Reproduction operators for fixed-length integer genotypes treated as lists.
 */
public class BoundedListOperator implements GeneticOperator<ArrayGenotype> {

	private int length;		//the number of genes.
	private int maxValue;	//the maximum value, exclusive, for all genes.

	private double xoProb;						//the probability of crossover.
	private CrossoverKind xoKind;				//the crossover method.
	private double vmProb;						//the probability of value mutation.
	private double insertProb;					//the probability of insertion mutation.
	private double deleteProb;					//the probability of deletion mutation.
	private double flipProb;					//the probability of flip mutation.
	private boolean allowDuplicateOffspring;	//whether to allow duplicate offspring.

	private boolean initialised;	//whether this operator has been initialised yet.

	/**
	 * Operator constructor.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param	length		the number of genes.
	 * @param	maxValue	the maximum value, exclusive, for all genes.
	 */
	public BoundedListOperator(int length, int maxValue) {
		this.length = length;
		this.maxValue = maxValue;

		xoProb = 0.99;
		xoKind = CrossoverKind.SINGLE_POINT;
		vmProb = 0.01;
		insertProb = 0.01;
		deleteProb = 0.01;
		flipProb = 0;
		allowDuplicateOffspring = false;

		initialised = false;
	}

	/**
	 * Set the crossover parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	xoProb	the crossover probability.
	 * @param	xoKind	the crossover method -
	 *					valid methods are SINGLE_POINT and UNIFORM.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 */
	public BoundedListOperator setXOParams(double xoProb, CrossoverKind xoKind) {
		if (!initialised) {
			this.xoProb = xoProb;
			this.xoKind = xoKind;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Set the mutation parameters will flip mutation probability of 0.
	 * Can only be used before initialisation.
	 *
	 * @see setMutationParams(double,double,double,double)
	 */
	public BoundedListOperator setMutationParams(double vmProb, double insertProb, double deleteProb) {
		return setMutationParams(vmProb,insertProb,deleteProb,0.0);
	}

	/**
	 * Set the mutation parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	vmProb		the value mutation probability per gene.
	 * @param	insertProb	the insertion mutation probability per gene position.
	 * @param	deleteProb	the deletion mutation probability per gene position.
	 * @param	flipProb	the flip mutation probability per gene position.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 */
	public BoundedListOperator setMutationParams(double vmProb, double insertProb, double deleteProb, double flipProb) {
		if (!initialised) {
			this.vmProb = vmProb;
			this.insertProb = insertProb;
			this.deleteProb = deleteProb;
			this.flipProb = flipProb;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Set whether to allow duplicate offspring.
	 * Can only be used before initialisation.
	 *
	 * @param	allowDuplicateOffspring	whether to allow duplicate offspring.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 */
	public BoundedListOperator setAllowDuplicateOffspring(boolean allowDuplicateOffspring) {
		if (!initialised) {
			this.allowDuplicateOffspring = allowDuplicateOffspring;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Initialise this object for use.
	 * Can only be used once.
	 *
	 * @return	this object.
	 * @throws	IllegalStateException if already initialised.
	 */
	public BoundedListOperator initialise() {
		if (!initialised) {
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Produce a list of offspring from a pool of parents by applying crossover and mutation.
	 * Crossover and each form of mutation are performed independently.
	 * Selection uses the provided selection scheme.
	 *
	 * Crossover is either single-point or uniform.
	 *
	 * Value mutation alters the value of genes, 
	 * each with a specified probability.
	 *
	 * Insertion mutation inserts new genes, 
	 * each with a specified probability, 
	 * and deletes the last gene for each insertion.
	 *
	 * Deletion mutation deletes genes, 
	 * each with a specified probability, 
	 * and inserts a random gene in the last position for each deletion.
	 *
	 * Flip mutation swaps adjacent genes,
	 * each with a specified probability.
	 *
	 * @param	selector		the selection operator with the pool of parent chromosomes.
	 * @param	numOffspring	the number of children to create.
	 * @return	an ArrayList of children chromosomes.
	 * @throws	IllegalStateException	if not initialised, 
	 *									or an invalid crossover method has been supplied.
	 */
	public ArrayList<ArrayGenotype> performOperation(SelectionOperator<ArrayGenotype,? extends Chromosome<ArrayGenotype>> selector, 
		int numOffspring) {
		if (initialised) {
			HashSet<ArrayGenotype> seen = new HashSet<>();
			ArrayList<ArrayGenotype> out = new ArrayList<>(numOffspring);
			while (out.size() < numOffspring) {
				ArrayGenotype[] parents = new ArrayGenotype[]{selector.performReproductionSelection(true).getGenotype(),
					selector.performReproductionSelection(false).getGenotype()};
				ArrayGenotype[] children = new ArrayGenotype[2];
				if (ThreadLocalRandom.current().nextDouble() < xoProb) {
					switch (xoKind) {
						case SINGLE_POINT: {
							performSinglePointCrossover(parents,children);
							break;
						}
						case UNIFORM: {
							performUniformCrossover(parents,children);
							break;
						}
						default: {
							throw new IllegalStateException(String.format("Invalid crossover kind %s",xoKind));
						}
					}
				}
				else {
					for (int i=0; i<2; i++) {
						children[i] = parents[i].clone();
					}
				}
				for (int i=0; i<2; i++) {
					if (deleteProb > 0) {
						children[i] = performDeletions(children[i]);
					}
					if (vmProb > 0) {
						children[i] = performValueMutation(children[i]);
					}
					if (flipProb > 0) {
						children[i] = performFlips(children[i]);
					}
					if (insertProb > 0) {
						children[i] = performInsertions(children[i]);
					}
					if (!allowDuplicateOffspring) {
						if (!seen.contains(children[i]) && !selector.contains(children[i])) {
							out.add(children[i]);
							seen.add(children[i]);
						}
					}
					else {
						out.add(children[i]);
					}
				}
			}
			return out;
		}
		else {
			throw new IllegalStateException("Operator not initialised");
		}
	}

	/**
	 * Performs single point crossover on two integer genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @param	children	the array to store the 2 children genotypes.
	 */
	public void performSinglePointCrossover(ArrayGenotype[] parents, ArrayGenotype[] children) {
		int[][] pStrings = new int[2][];
		for (int i=0; i<2; i++) {
			pStrings[i] = parents[i].getArray();
		}
		int split = ThreadLocalRandom.current().nextInt(length - 1) + 1;
		int[][] cStrings = new int[2][length];
		for (int i=0; i<2; i++) {
			for (int j=0; j<split; j++) {
				cStrings[i][j] = pStrings[i][j];
			}
			for (int j=split; j<length; j++) {
				cStrings[i][j] = pStrings[1 - i][j];
			}
			children[i] = new ArrayGenotype(cStrings[i]);
		}
	}

	/**
	 * Performs uniform crossover on two integer genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @param	children	the array to store the 2 children genotypes.
	 */
	public void performUniformCrossover(ArrayGenotype[] parents, ArrayGenotype[] children) {
		int[][] pStrings = new int[2][];
		for (int i=0; i<2; i++) {
			pStrings[i] = parents[i].getArray();
		}
		int[][] cStrings = new int[2][length];
		for (int i=0; i<length; i++) {
			int first = ThreadLocalRandom.current().nextInt(2);
			cStrings[0][i] = pStrings[first][i];
			cStrings[1][i] = pStrings[1 - first][i];
		}
		for (int i=0; i<2; i++) {
			children[i] = new ArrayGenotype(cStrings[i]);
		}
	}

	/**
	 * Create a child by randomly altering gene values,
	 * with a specified probability per gene.
	 *
	 * @param	parent	the genotype to mutate.
	 * @return	the mutated child.
	 */
	public ArrayGenotype performValueMutation(ArrayGenotype parent) {
		if (vmProb <= 0) {
			return parent;
		}
		int[] childString = parent.getArray();
		for (int i=0; i<length; i++) {
			if (ThreadLocalRandom.current().nextDouble() < vmProb) {
				childString[i] = (childString[i] + ThreadLocalRandom.current().nextInt(maxValue - 1) + 1) % maxValue;
			}
		}
		return new ArrayGenotype(childString);
	}

	/**
	 * Create a child by inserting random genes,
	 * with a specified probability per gene position,
	 * and deleting the final gene per insertion.
	 *
	 * @param	parent	the genotype to mutate.
	 * @return	the mutated child.
	 */
	public ArrayGenotype performInsertions(ArrayGenotype parent) {
		if (insertProb <= 0) {
			return parent;
		}
		int[] pString = parent.getArray();
		int[] cString = new int[length];
		int look = 0;
		for (int i=0; i<length; i++) {
			if (ThreadLocalRandom.current().nextDouble() < insertProb) {
				cString[i] = ThreadLocalRandom.current().nextInt(maxValue);
			}
			else {
				cString[i] = pString[look];
				look++;
			}
		}
		return new ArrayGenotype(cString);
	}

	/**
	 * Create a child by deleting genes,
	 * with a specified probability per gene position,
	 * and inserting random genes into the final position per deletion.
	 *
	 * @param	parent	the genotype to mutate.
	 * @return	the mutated child.
	 */
	public ArrayGenotype performDeletions(ArrayGenotype parent) {
		if (deleteProb <= 0) {
			return parent;
		}
		int[] childString = parent.getArray();
		int place = 0;
		for (int i=0; i<length; i++) {
			if (ThreadLocalRandom.current().nextDouble() > deleteProb) {
				childString[place] = childString[i];
				place++;
			}
		}
		for (int i=place; i<length; i++) {
			childString[i] = ThreadLocalRandom.current().nextInt(maxValue);
		}
		return new ArrayGenotype(childString);
	}

	/**
	 * Create a child by swapping adjacent genes,
	 * with a specified probability per gene position.
	 *
	 * @param	parent	the genotype to mutate.
	 * @return	the mutated child.
	 */
	public ArrayGenotype performFlips(ArrayGenotype parent) {
		if (flipProb <= 0) {
			return parent;
		}
		int[] childString = parent.getArray();
		for (int i=0; i<length - 1; i++) {
			if (ThreadLocalRandom.current().nextDouble() < flipProb) {
				int temp = childString[i];
				childString[i] = childString[i + 1];
				childString[i + 1] = temp;
			}
		}
		return new ArrayGenotype(childString);
	}

}