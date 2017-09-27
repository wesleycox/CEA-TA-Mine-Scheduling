import mines.sim.*;
import mines.util.*;
import mines.system.*;
import mines.sol.*;
import mines.sol.greedy.*;
import mines.sol.lp.*;
import mines.ea.op.gene.*;
import mines.sol.ea.cont.combined.linked.coevolution.*;
import mines.sol.ea.cont.dispatch.fixed.*;
import mines.sol.ea.cont.lights.*;

public class Main {

	public static void main(String[] args) {
		if (args.length < 4) {
			throw new IllegalArgumentException(String.format("\nusage: ... Main filename numSamples runtime solIndex...\n" +
				"\tfilename the input file name\n" +
				"\tnumSamples the integer number of to run simulations per solution\n" +
				"\truntime the real-valued shift length per simulation\n" +
				"\tsolIndex a solution index between 0 and 27 (inclusive)\n"));
		}
		try {
			Main main = new Main();
			String file = args[0];
			int numSamples = Integer.parseInt(args[1]);
			double runtime = Double.parseDouble(args[2]);
			int[] solIndexes = new int[args.length - 3];
			for (int i=3; i<args.length; i++) {
				solIndexes[i - 3] = Integer.parseInt(args[i]);
			}
			main.run(file,solIndexes,numSamples,runtime);
		}
		catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(String.format("\nusage: ... Main filename numSamples runtime solIndex...\n" +
				"\tfilename the input file name\n" +
				"\tnumSamples the integer number of to run simulations per solution\n" +
				"\truntime the real-valued shift length per simulation\n" +
				"\tsolIndex a solution index between 0 and 27 (inclusive)\n"));
		}
	}

	public void run(String file, int[] solIndexes, int numSamples, double runtime) {
		MineParameters4 params = new MineParameters4Shortest(file);
		TimeDistribution tgen = new NormalTimes();
		MineSimulator4 sim = new MineSimulator4(params,tgen);
		Debugger.setDebug(true);
		for (int solIndex : solIndexes) {
			Solution4 sol;
			System.out.printf("Preparing solution index %d...\n",solIndex);
			switch (solIndex) {
				case 0:
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
				case 7:
				case 8:
				case 9:
				case 10:
				case 11:
				case 12:
				case 13: {
					HeuristicKind[] hKinds = new HeuristicKind[]{HeuristicKind.MTRT,HeuristicKind.MTCT,HeuristicKind.MTST,
						HeuristicKind.MTTWT1,HeuristicKind.MTTWT2,HeuristicKind.MTSWT,HeuristicKind.MSWT};
					HeuristicKind hKindUse = hKinds[solIndex % 7];
					boolean allGreedy = (solIndex <= 6);
					sol = new SingleCrusherGreedySolution(params,tgen,20,hKindUse,runtime,allGreedy);
					break;
				}
				case 14:
				case 15: {
					boolean allGreedy = (solIndex <= 14);
					sol = new DISPATCHSolution(params,true,false,runtime,allGreedy);
					break;
				}
				case 16:
				case 17:
				case 18:
				case 19:
				case 20:
				case 21: {
					int fitnessIndex = (solIndex - 16) % 3;
					double lookAheadFactor = (fitnessIndex == 2 ? 2.0 : 1.0);
					double xoProb = 0.99;
					CrossoverKind xoKind = CrossoverKind.SINGLE_POINT;
					double vmProb = 0.01;
					double insertProb = 0.01;
					double deleteProb = 0.01;
					boolean allowDuplicateOffspring = false;
					int popSize = 100;
					int numOffspring = 100;
					double elitism = 0;
					boolean allowSurvivors = true;
					int bucketSize = 20;
					int resampleRate = 1;
					int resampleSize = 1;
					int maxGen = 999;
					int conCutoff = 99;
					double improvement = 0.005;
					double updateInterval = 15;
					boolean allGreedy = (solIndex <= 16);
					sol = new SingleCrusherContinuousEAFLListDispatchWTimerSolution(params,runtime,allGreedy,tgen)
						.setFitnessParams(lookAheadFactor,fitnessIndex)
						.setXOParams(xoProb,xoKind)
						.setMutationParams(vmProb,insertProb,deleteProb)
						.setAllowDuplicateOffspring(allowDuplicateOffspring)
						.setStrategyParams(popSize,numOffspring,elitism,allowSurvivors)
						.setSamplingParams(bucketSize,resampleRate,resampleSize)
						.setTerminationParams(maxGen,conCutoff,improvement)
						.setUpdateInterval(updateInterval)
						.initialise();
					break;
				}
				case 22:
				case 23:
				case 24: {
					int fitnessIndex = (solIndex - 22) % 3;
					double lookAheadFactor = (fitnessIndex == 2 ? 2.0 : 1.0);
					double updateInterval = 15;
					boolean allowDuplicateOffspring = false;
					int popSize = 100;
					int numOffspring = 100;
					double elitism = 0;
					boolean allowSurvivors = true;
					int bucketSize = 20;
					int resampleRate = 1;
					int resampleSize = 1;
					int maxGen = 999;
					int conCutoff = 99;
					double improvement = 0.005;
					double xoProb = 1.0;
					CrossoverKind[] xoKinds = new CrossoverKind[]{CrossoverKind.BLX_A};
					double vmProb = 1.0;
					double mStrength = 0.05;
					sol = new ContinuousEALightsWFlowDispatchSolution(params,tgen)
						.setFitnessParams(lookAheadFactor,fitnessIndex)
						.setUpdateInterval(updateInterval)
						.setAllowDuplicateOffspring(allowDuplicateOffspring)
						.setStrategyParams(popSize,numOffspring,elitism,allowSurvivors)
						.setSamplingParams(bucketSize,resampleRate,resampleSize)
						.setTerminationParams(maxGen,conCutoff,improvement)
						.setXOParams(xoProb,xoKinds)
						.setMutationParams(vmProb,mStrength)
						.initialise();
					break;
				}
				case 25:
				case 26:
				case 27: {
					int fitnessIndex = (solIndex - 25) % 3;
					double lookAheadFactor = (fitnessIndex == 2 ? 2.0 : 1.0);
					int numSamplesF = 20;
					double xoProbDispatch = 0.99;
					CrossoverKind xoKindDispatch = CrossoverKind.SINGLE_POINT;
					double vmProbDispatch = 0.01;
					double insertProbDispatch = 0.01;
					double deleteProbDispatch = 0.01;
					double flipProbDispatch = 0.0;
					boolean allowDuplicateOffspringDispatch = false;
					double xoProbLights = 1.0;
					CrossoverKind[] xoKindsLights = new CrossoverKind[]{CrossoverKind.BLX_A};
					double vmProbLights = 1.0;
					double mStrengthLights = 0.05;
					boolean allowDuplicateOffspringLights = false;
					int numCollaborators = 1;
					int popSizeDispatch = 100;
					int popSizeLights = 100;
					int numOffspringDispatch = 100;
					int numOffspringLights = 100;
					boolean allowSurvivors = true;
					int maxGen = 999;
					int conCutoff = 100 / numCollaborators;
					double improvement = 0.005;
					double updateInterval = 15;
					sol = new SingleCrusherContinuousCEACombinedSolution(params,tgen)
						.setFitnessParams(numSamplesF,lookAheadFactor,fitnessIndex)
						.setDispatchXOParams(xoProbDispatch,xoKindDispatch)
						.setDispatchMutationParams(vmProbDispatch,insertProbDispatch,deleteProbDispatch,flipProbDispatch)
						.setDispatchAllowDuplicateOffspring(allowDuplicateOffspringDispatch)
						.setLightsXOParams(xoProbLights,xoKindsLights)
						.setLightsMutationParams(vmProbLights,mStrengthLights)
						.setLightsAllowDuplicateOffspring(allowDuplicateOffspringLights)
						.setCollaborationParams(numCollaborators)
						.setStrategyParams(popSizeDispatch,popSizeLights,numOffspringDispatch,numOffspringLights,allowSurvivors)
						.setTerminationParams(maxGen,conCutoff,improvement)
						.setUpdateInterval(updateInterval)
						.initialise();
					break;
				}
				default: {
					throw new IllegalArgumentException(String.format("Illegal solution index provided: %d",solIndex));
				}
			}
			System.out.printf("Preparing controller...\n");
			sim.loadController(sol.getController());
			double[] samples = new double[numSamples];
			double total = 0;
			Debugger.setDebug(false);
			System.out.printf("Beginning simulations...\n");
			for (int i=0; i<numSamples; i++) {
				sim.initialise();
				sim.simulate(runtime);
				int numEmpties = sim.getEmpties();
				samples[i] = numEmpties;
				total += samples[i];
				System.out.printf("Simulation %d complete with %d truckloads\n",i + 1,numEmpties);
			}
			System.out.printf("%d simulations complete...\n",numSamples);
			double average = total / numSamples;
			double stdev = 0;
			for (int i=0; i<numSamples; i++) {
				stdev += (samples[i] - average) * (samples[i] - average);
			}
			stdev = Math.sqrt(stdev / numSamples);
			System.out.printf("%s : mean-%f sd-%f\n\n",sol.getSolutionName(),average,stdev);
		}
	}
	
}