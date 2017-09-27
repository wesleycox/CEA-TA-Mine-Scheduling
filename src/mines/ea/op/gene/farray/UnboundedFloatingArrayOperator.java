package mines.ea.op.gene.farray;

import mines.ea.op.gene.*;
import mines.ea.gene.FloatingArrayGenotype;
import mines.ea.op.selection.*;
import mines.ea.chrom.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Reproduction operators for fixed-length real-coded genotypes.
 *
 * Descriptions of the crossover operators implemented here can be found in:
 *
 * Adewuya, A. A. (1996). 
 * New methods in genetic search with real-valued chromosomes 
 * (Doctoral dissertation, Massachusetts Institute of Technology).
 *
 * Herrera, F., Lozano, M., & Sánchez, A. M. (2003). 
 * A taxonomy for the crossover operator for real-coded genetic algorithms: An experimental study. 
 * International Journal of Intelligent Systems, 18(3), 309-338.
 */
public class UnboundedFloatingArrayOperator implements GeneticOperator<FloatingArrayGenotype> {

	/* The available crossover methods */
	public static final CrossoverKind[] allXOKinds = new CrossoverKind[]{CrossoverKind.SINGLE_POINT,CrossoverKind.UNIFORM,
		CrossoverKind.AVERAGE,CrossoverKind.LINEAR,CrossoverKind.WHOLE_ARITHMETIC,CrossoverKind.BLX_A,CrossoverKind.HEURISTIC,
		CrossoverKind.QUADRATIC,CrossoverKind.LAPLACE,CrossoverKind.GEOMETRIC,CrossoverKind.SIMULATED_BINARY,CrossoverKind.MIN_MAX};

	private int length;						//the number of genes.
	private Comparator<Chromosome> comp;	//chromosome comparison based on fitness.

	private double xoProb;						//crossover probability.
	private CrossoverKind[] xoKinds;			//the set of crossover schemes to use.
	private double vmProb;						//mutation probability per gene.
	private double mStrength;					//the standard deviation for the gaussian mutation.
	private boolean allowDuplicateOffspring;	//whether to allow duplicate offspring.

	private boolean initialised;	//whether this operator has been initialised yet.

	/**
	 * Operator constructor.
	 * Some variables are set to default values and can be altered by other methods before initialisation.
	 * Instances of this class cannot be used until initialisation.
	 *
	 * @param	length		the number of genes.
	 * @param	maximising	whether the fitness of chromosomes is maximising.
	 */
	public UnboundedFloatingArrayOperator(int length, boolean maximising) {
		this.length = length;
		comp = new Comparator<Chromosome>() {
			public int compare(Chromosome c1, Chromosome c2) {
				int diff = (maximising ? -1 : 1) * c1.compareTo(c2);
				return (diff == 0 ? (c2.getAge() - c1.getAge()) : diff);
			}
		};

		xoProb = 1.0;
		xoKinds = new CrossoverKind[]{CrossoverKind.BLX_A};
		vmProb = 1.0;
		mStrength = 0.05;
		allowDuplicateOffspring = false;

		initialised = false;
	}

	/**
	 * Set the crossover parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	xoProb	the crossover probability.
	 * @param	xoKinds	an array of the crossover methods -
	 *					for each crossover, a method is uniformly chosen from this.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 * @see	getAvailableCrossoverKinds()
	 */
	public UnboundedFloatingArrayOperator setXOParams(double xoProb, CrossoverKind[] xoKinds) {
		if (!initialised) {
			this.xoProb = xoProb;
			this.xoKinds = Arrays.copyOf(xoKinds,xoKinds.length);
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Set the mutation parameters.
	 * Can only be used before initialisation.
	 *
	 * @param	vmProb		the value mutation probability per gene.
	 * @param	mStrength	the standard deviation for the gaussian mutation.
	 * @return	this object.
	 * @throws	IllegalStateException		if already initialised.
	 */
	public UnboundedFloatingArrayOperator setMutationParams(double vmProb, double mStrength) {
		if (!initialised) {
			this.vmProb = vmProb;
			this.mStrength = mStrength;
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
	public UnboundedFloatingArrayOperator setAllowDuplicateOffspring(boolean allowDuplicateOffspring) {
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
	public UnboundedFloatingArrayOperator initialise() {
		if (!initialised) {
			initialised = true;
			return this;
		}
		else {
			throw new IllegalStateException("Operator already initialised");
		}
	}

	/**
	 * Get all possible crossover methods used by this operator.
	 *
	 * @return	an array of valid CrossoverKinds
	 */
	public static CrossoverKind[] getAvailableCrossoverKinds() {
		return Arrays.copyOf(allXOKinds,allXOKinds.length);
	}

	/**
	 * Produce a list of offspring from a pool of parents by applying crossover and mutation.
	 * Crossover and then mutation are performed independently.
	 * Selection uses the provided selection scheme.
	 *
	 * Crossover is either single-point,
	 * uniform, average, linear, 
	 * whole arithmetic, BLX-alpha, 
	 * heuristic, quadratic, laplace, 
	 * geometric, simulated binary, 
	 * or min/max.
	 * Each crossover chooses uniformly a crossover method from the provided methods.
	 *
	 * Value mutation alters the value of genes,
	 * by adding normally distributed random values,
	 * each with a specified probability.
	 *
	 * @param	selector		the selection operator with the pool of parent chromosomes.
	 * @param	numOffspring	the number of children to create.
	 * @return	an ArrayList of children chromosomes.
	 * @throws	IllegalStateException	if not initialised, 
	 *									or an invalid crossover method has been supplied.
	 */
	public ArrayList<FloatingArrayGenotype> performOperation(
		SelectionOperator<FloatingArrayGenotype,? extends Chromosome<FloatingArrayGenotype>> selector, int numOffspring) {
		if (initialised) {
			HashSet<FloatingArrayGenotype> seen = new HashSet<>();
			ArrayList<FloatingArrayGenotype> out = new ArrayList<>(numOffspring);
			while (out.size() < numOffspring) {
				FloatingArrayGenotype[] children;
				if (ThreadLocalRandom.current().nextDouble() < xoProb) {
					CrossoverKind xoUse = xoKinds[ThreadLocalRandom.current().nextInt(xoKinds.length)];
					switch (xoUse) {
						case SINGLE_POINT: {
							FloatingArrayGenotype[] parents = new FloatingArrayGenotype[]{
								selector.performReproductionSelection(true).getGenotype(),
								selector.performReproductionSelection(false).getGenotype()};
							children = performSinglePointCrossover(parents);
							break;
						}
						case UNIFORM: {
							FloatingArrayGenotype[] parents = new FloatingArrayGenotype[]{
								selector.performReproductionSelection(true).getGenotype(),
								selector.performReproductionSelection(false).getGenotype()};
							children = performUniformCrossover(parents);
							break;
						}
						case AVERAGE: {
							FloatingArrayGenotype[] parents = new FloatingArrayGenotype[]{
								selector.performReproductionSelection(true).getGenotype(),
								selector.performReproductionSelection(false).getGenotype()};
							children = new FloatingArrayGenotype[]{performAverageCrossover(parents)};
							break;
						}
						case LINEAR: {
							FloatingArrayGenotype[] parents = new FloatingArrayGenotype[]{
								selector.performReproductionSelection(true).getGenotype(),
								selector.performReproductionSelection(false).getGenotype()};
							children = performLinearCrossover(parents);
							break;
						}
						case WHOLE_ARITHMETIC: {
							FloatingArrayGenotype[] parents = new FloatingArrayGenotype[]{
								selector.performReproductionSelection(true).getGenotype(),
								selector.performReproductionSelection(false).getGenotype()};
							children = performWholeArithmeticCrossover(parents);
							break;
						}
						case BLX_A: {
							FloatingArrayGenotype[] parents = new FloatingArrayGenotype[]{
								selector.performReproductionSelection(true).getGenotype(),
								selector.performReproductionSelection(false).getGenotype()};
							children = new FloatingArrayGenotype[]{performBLXCrossover(parents)};
							break;
						}
						case HEURISTIC: {
							children = new FloatingArrayGenotype[]{performHeuristicCrossover(selector.performReproductionSelection(true),
								selector.performReproductionSelection(false))};
							break;
						}
						case QUADRATIC: {
							children = new FloatingArrayGenotype[]{performQuadraticCrossover(selector.performReproductionSelection(true),
								selector.performReproductionSelection(false),selector.performReproductionSelection(false))};
							break;
						}
						case LAPLACE: {
							FloatingArrayGenotype[] parents = new FloatingArrayGenotype[]{
								selector.performReproductionSelection(true).getGenotype(),
								selector.performReproductionSelection(false).getGenotype()};
							children = performLaplaceCrossover(parents);
							break;
						}
						case GEOMETRIC: {
							FloatingArrayGenotype[] parents = new FloatingArrayGenotype[]{
								selector.performReproductionSelection(true).getGenotype(),
								selector.performReproductionSelection(false).getGenotype()};
							children = performGeometricCrossover(parents);
							break;
						}
						case SIMULATED_BINARY: {
							FloatingArrayGenotype[] parents = new FloatingArrayGenotype[]{
								selector.performReproductionSelection(true).getGenotype(),
								selector.performReproductionSelection(false).getGenotype()};
							children = performSimulatedBinaryCrossover(parents);
							break;
						}
						case MIN_MAX: {
							FloatingArrayGenotype[] parents = new FloatingArrayGenotype[]{
								selector.performReproductionSelection(true).getGenotype(),
								selector.performReproductionSelection(false).getGenotype()};
							children = performMinMaxCrossover(parents);
							break;
						}
						default: {
							throw new IllegalStateException(String.format("Invalid crossover kind %s",xoUse));
						}
					}
				}
				else {
					children = new FloatingArrayGenotype[]{selector.performReproductionSelection(true).getGenotype().clone()};
				}
				for (int i=0; i<children.length; i++) {
					if (vmProb > 0) {
						children[i] = performValueMutation(children[i]);
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
	 * Performs single point crossover on two real-coded genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @return	an array of two children genotypes.
	 */
	public FloatingArrayGenotype[] performSinglePointCrossover(FloatingArrayGenotype[] parents) {
		double[][] pString = new double[2][];
		for (int i=0; i<2; i++) {
			pString[i] = parents[i].getArray();
		}
		FloatingArrayGenotype[] children = new FloatingArrayGenotype[2];
		double[][] cString = new double[2][length];
		int split = ThreadLocalRandom.current().nextInt(length - 1) + 1;
		for (int i=0; i<2; i++) {
			for (int j=0; j<split; j++) {
				cString[i][j] = pString[i][j];
			}
			for (int j=split; j<length; j++) {
				cString[i][j] = pString[1 - i][j];
			}
			children[i] = new FloatingArrayGenotype(cString[i]);
		}
		return children;
	}

	/**
	 * Performs uniform crossover on two real-coded genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @return	an array of two children genotypes.
	 */
	public FloatingArrayGenotype[] performUniformCrossover(FloatingArrayGenotype[] parents) {
		double[][] pString = new double[2][];
		for (int i=0; i<2; i++) {
			pString[i] = parents[i].getArray();
		}
		double[][] cString = new double[2][length];
		for (int i=0; i<length; i++) {
			int b = ThreadLocalRandom.current().nextInt(2);
			cString[0][i] = pString[b][i];
			cString[1][i] = pString[1 - b][i];
		}
		FloatingArrayGenotype[] children = new FloatingArrayGenotype[2];
		for (int i=0; i<2; i++) {
			children[i] = new FloatingArrayGenotype(cString[i]);
		}
		return children;
	}

	/**
	 * Performs average crossover on two real-coded genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @return	a single child genotype.
	 */
	public FloatingArrayGenotype performAverageCrossover(FloatingArrayGenotype[] parents) {
		double[][] pString = new double[2][];
		for (int i=0; i<2; i++) {
			pString[i] = parents[i].getArray();
		}
		double[] childString = new double[length];
		for (int i=0; i<length; i++) {
			childString[i] = (pString[0][i] + pString[1][i]) / 2.0;
		}
		return new FloatingArrayGenotype(childString);
	}

	/**
	 * Performs linear crossover on two real-coded genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @return	an array of three children genotypes.
	 */
	public FloatingArrayGenotype[] performLinearCrossover(FloatingArrayGenotype[] parents) {
		double[][] pString = new double[2][];
		for (int i=0; i<2; i++) {
			pString[i] = parents[i].getArray();
		}
		double[][] cString = new double[3][length];
		int[] indexes = new int[]{0,1,2};
		for (int i=0; i<length; i++) {
			shuffle(indexes);
			cString[indexes[0]][i] = (pString[0][i] + pString[1][i]) / 2.0;
			cString[indexes[1]][i] = Math.max(0,1.5 * pString[0][i] - 0.5 * pString[1][i]);
			cString[indexes[2]][i] = Math.max(0,1.5 * pString[1][i] - 0.5 * pString[0][i]);
		}
		FloatingArrayGenotype[] children = new FloatingArrayGenotype[3];
		for (int i=0; i<3; i++) {
			children[i] = new FloatingArrayGenotype(cString[i]);
		}
		return children;
	}

	/**
	 * Performs whole arithmetic crossover on two real-coded genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @return	an array of two children genotypes.
	 */
	public FloatingArrayGenotype[] performWholeArithmeticCrossover(FloatingArrayGenotype[] parents) {
		double[][] pString = new double[2][];
		for (int i=0; i<2; i++) {
			pString[i] = parents[i].getArray();
		}
		double[][] cString = new double[2][length];
		for (int i=0; i<length; i++) {
			double a = ThreadLocalRandom.current().nextDouble();
			cString[0][i] = a * pString[0][i] + (1 - a) * pString[1][i];
			cString[1][i] = (1 - a) * pString[0][i] + a * pString[1][i];
		}
		FloatingArrayGenotype[] children = new FloatingArrayGenotype[2];
		for (int i=0; i<2; i++) {
			children[i] = new FloatingArrayGenotype(cString[i]);
		}
		return children;
	}

	/**
	 * Performs BLX-alpha crossover on two real-coded genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @return	a single child genotype.
	 */
	public FloatingArrayGenotype performBLXCrossover(FloatingArrayGenotype[] parents) {
		double alpha = 0.5;
		double[][] pString = new double[2][];
		for (int i=0; i<2; i++) {
			pString[i] = parents[i].getArray();
		}
		double[] childString = new double[length];
		for (int i=0; i<length; i++) {
			double width = pString[1][i] - pString[0][i];
			double b1 = pString[0][i] - width * alpha;
			double b2 = pString[1][i] + width * alpha;
			childString[i] = Math.max(0,ThreadLocalRandom.current().nextDouble() * (b2 - b1) + b1);
		}
		return new FloatingArrayGenotype(childString);
	}

	/**
	 * Performs heuristic crossover on two real-coded genotypes.
	 *
	 * @param	parent0		the first chromosome containing a parent genotype.
	 * @param	parent1		the second chromosome containing a parent genotype.
	 * @return	a single child genotype.
	 */
	private FloatingArrayGenotype performHeuristicCrossover(Chromosome<FloatingArrayGenotype> parent0, 
		Chromosome<FloatingArrayGenotype> parent1) {
		double[][] pString = new double[2][];
		if (comp.compare(parent0,parent1) <= 0) {
			pString[0] = parent0.getGenotype().getArray();
			pString[1] = parent1.getGenotype().getArray();
		}
		else {
			pString[1] = parent0.getGenotype().getArray();
			pString[0] = parent1.getGenotype().getArray();
		}
		double[] childString = new double[length];
		for (int i=0; i<length; i++) {
			double r = ThreadLocalRandom.current().nextDouble();
			childString[i] = Math.max(0,r * (pString[0][i] - pString[1][i]) + pString[0][i]);
		}
		return new FloatingArrayGenotype(childString);
	}

	/**
	 * Performs quadratic crossover on three real-coded genotypes.
	 *
	 * @param	parent0		the first chromosome containing a parent genotype.
	 * @param	parent1		the second chromosome containing a parent genotype.
	 * @param	parent2		the third chromosome containing a parent genotype.
	 * @return	a single child genotype.
	 */
	private FloatingArrayGenotype performQuadraticCrossover(Chromosome<FloatingArrayGenotype> parent0, 
		Chromosome<FloatingArrayGenotype> parent1, Chromosome<FloatingArrayGenotype> parent2) {
		ArrayList<Chromosome<FloatingArrayGenotype>> parents = new ArrayList<>(3);
		parents.add(parent0);
		parents.add(parent1);
		parents.add(parent2);
		Collections.sort(parents,comp);
		double[][] pString = new double[3][];
		double[] f = new double[3];
		for (int i=0; i<3; i++) {
			pString[i] = parents.get(i).getGenotype().getArray();
			f[i] = parents.get(i).getFitness();
		}
		double[] childString = new double[length];
		for (int i=0; i<length; i++) {
			double v01 = pString[0][i] - pString[1][i];
			double v02 = pString[0][i] - pString[2][i];
			double v12 = pString[1][i] - pString[2][i];
			boolean success = false;
			if (v01 != 0 && v02 != 0 && v12 != 0) {
				double a = f[2] / (v02 * v12) + f[0] / (v01 * v02) - f[1] / (v01 * v12);
				double b = (f[0] - f[1]) / v01 - a * (pString[0][i] + pString[1][i]);
				if (a < 0) {
					childString[i] = Math.max(0,-b / (2 * a));
					success = true;
				}
			}
			if (!success) {
				double r = ThreadLocalRandom.current().nextDouble();
				childString[i] = Math.max(0,r * (pString[0][i] - pString[2][i]) + pString[0][i]);
			}
		}
		return new FloatingArrayGenotype(childString);
	}

	/**
	 * Performs laplace crossover on two real-coded genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @return	an array of two children genotypes.
	 */
	public FloatingArrayGenotype[] performLaplaceCrossover(FloatingArrayGenotype[] parents) {
		double b = 1.0;
		double[][] pString = new double[2][];
		for (int i=0; i<2; i++) {
			pString[i] = parents[i].getArray();
		}
		double[][] cString = new double[2][length];
		for (int i=0; i<length; i++) {
			double diff = Math.abs(pString[0][i] - pString[1][i]);
			double u = ThreadLocalRandom.current().nextDouble();
			double beta = (u <= 0.5 ? b * Math.log(2 * u) : -b * Math.log(2 - 2 * u));
			for (int j=0; j<2; j++) {
				cString[j][i] = Math.max(0,pString[j][i] + beta * (pString[0][i] - pString[1][i]));
			}
		}
		FloatingArrayGenotype[] children = new FloatingArrayGenotype[2];
		for (int i=0; i<2; i++) {
			children[i] = new FloatingArrayGenotype(cString[i]);
		}
		return children;
	}

	/**
	 * Performs geometric crossover on two real-coded genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @return	an array of two children genotypes.
	 */
	public FloatingArrayGenotype[] performGeometricCrossover(FloatingArrayGenotype[] parents) {
		double[][] pString = new double[2][];
		for (int i=0; i<2; i++) {
			pString[i] = parents[i].getArray();
		}
		double[][] cString = new double[2][length];
		for (int i=0; i<length; i++) {
			double a = ThreadLocalRandom.current().nextDouble();
			cString[0][i] = Math.pow(pString[0][i],a) * Math.pow(pString[1][i],1 - a);
			cString[1][i] = Math.pow(pString[0][i],1 - a) * Math.pow(pString[1][i],a);
		}
		FloatingArrayGenotype[] children = new FloatingArrayGenotype[2];
		for (int i=0; i<2; i++) {
			children[i] = new FloatingArrayGenotype(cString[i]);
		}
		return children;
	}

	/**
	 * Performs simulated binary crossover on two real-coded genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @return	an array of two children genotypes.
	 */
	public FloatingArrayGenotype[] performSimulatedBinaryCrossover(FloatingArrayGenotype[] parents) {
		double nu = 1;
		double[][] pString = new double[2][];
		for (int i=0; i<2; i++) {
			pString[i] = parents[i].getArray();
		}
		double[][] cString = new double[2][length];
		for (int i=0; i<length; i++) {
			if (ThreadLocalRandom.current().nextBoolean()) {
				double u = ThreadLocalRandom.current().nextDouble();
				double beta = (u <= 0.5 ? Math.pow(2 * u,1.0 / (nu + 1.0)) : Math.pow(2 - 2 * u,-1.0 / (nu + 1.0)));
				cString[0][i] = Math.max(0,(pString[0][i] + pString[1][i] - beta * Math.abs(pString[0][i] - pString[1][i])) / 2.0);
				cString[1][i] = Math.max(0,(pString[0][i] + pString[1][i] + beta * Math.abs(pString[0][i] - pString[1][i])) / 2.0);
			}
			else {
				for (int j=0; j<2; j++) {
					cString[j][i] = pString[j][i];
				}
			}
		}
		FloatingArrayGenotype[] children = new FloatingArrayGenotype[2];
		for (int i=0; i<2; i++) {
			children[i] = new FloatingArrayGenotype(cString[i]);
		}
		return children;
	}

	/**
	 * Performs min/max crossover on two real-coded genotypes.
	 *
	 * @param	parents		the array of parent genotypes.
	 * @return	an array of two children genotypes.
	 */
	public FloatingArrayGenotype[] performMinMaxCrossover(FloatingArrayGenotype[] parents) {
		double nu = 1;
		double[][] pString = new double[2][];
		for (int i=0; i<2; i++) {
			pString[i] = parents[i].getArray();
		}
		double[][] cString = new double[2][length];
		for (int i=0; i<length; i++) {
			cString[0][i] = Math.min(pString[0][i],pString[1][i]);
			cString[1][i] = Math.max(pString[0][i],pString[1][i]);
		}
		FloatingArrayGenotype[] children = new FloatingArrayGenotype[2];
		for (int i=0; i<2; i++) {
			children[i] = new FloatingArrayGenotype(cString[i]);
		}
		return children;
	}

	/**
	 * Create a child by randomly altering gene values,
	 * by adding normally distributed values with a specified standard deviation,
	 * with a specified probability per gene.
	 *
	 * @param	parent	the genotype to mutate.
	 * @return	the mutated child.
	 */
	public FloatingArrayGenotype performValueMutation(FloatingArrayGenotype parent) {
		if (vmProb <= 0) {
			return parent;
		}
		double[] childString = parent.getArray();
		for (int i=0; i<length; i++) {
			if (ThreadLocalRandom.current().nextDouble() < vmProb) {
				childString[i] = Math.max(0,childString[i] + getNormal() * mStrength);
			}
		}
		return new FloatingArrayGenotype(childString);
	}

	/**
	 * Get a normally distributed random value with mean 0 and standard deviation 1.
	 *
	 * @return	the random value.
	 */
	private double getNormal() {
		double u1 = ThreadLocalRandom.current().nextDouble();
		double u2 = ThreadLocalRandom.current().nextDouble();
		return Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
	}

	/**
	 * Randomise the order of values in an array.
	 */
	private void shuffle(int[] array) {
		for (int i=0; i<array.length - 1; i++) {
			int r = ThreadLocalRandom.current().nextInt(array.length - i) + i;
			int temp = array[r];
			array[r] = array[i];
			array[i] = temp;
		}
	}

	// private boolean unequal(double a, double b, double c) {
	// 	return (a != b && b != c && c != a);
	// }

}