package mines.sim;

import mines.util.IntQueue;
import java.util.*;
import java.io.*;

/**
 * Structure for simulation and routing parameters.
 */
public class MineParameters4 {

	/*
	 * Simulation parameters.
	 */
	protected int numTrucks;						//number of trucks.
	protected final int numShovels;					//number of shovels.
	protected final int numCrusherLocs;				//number of crusher locations.
	protected final int[] numCrushers;				//number of crushers at each location.
	protected final int numRoads;					//number of roads.
	protected final double[] emptyTimesMean;		//average emptying times for each crusher location.
	protected final double[] emptyTimesSD;			//standard deviations of emptying times for each crusher location.
	protected final double[] fillTimesMean;			//average filling times for each shovel.
	protected final double[] fillTimesSD;			//standard deviations of filling times for each shovel.
	protected final double[][] roadTravelTimesMean;	//average travelling times on each road in both directions.
	protected final double[][] roadTravelTimesSD;	//standard deviations of travelling time on each road in both directions.
	protected final double fullSlowdown;			//travel time increase for travelling full.
	protected final boolean[] isOneWay;				//whether each road is one-lane.

	/*
	 * Route specifications.
	 */
	protected int numRoutes;			//number of routes.
	protected int[][] routeRoads;		//list of roads comprising each route.
	protected int[] routeLengths;		//number of roads in each route.
	protected int[] routeShovels;		//the shovel at the end of each route.
	protected int[] routeCrushers;		//the crusher at the start of each route.
	protected int[][] routeDirections;	//list of directions travelled on each road in each route.

	/**
	 * Initialise the parameters from an input file,
	 * and determine the available routes.
	 *
	 * Input files are of the format:
	 *	First line:
	 *		T NT FS
	 *		where NT is the number of trucks,
	 *		and FS is the full slowdown penalty.
	 *	Second line:
	 *		C NCL
	 *		where NCL is the number of crusher locations.
	 *	NCL lines of:
	 *		NC EM ESD
	 *		where for the nth line, 
	 *		NC is the number of crushers at the nth location,
	 *		EM is mean emptying time for all crushers at that location,
	 *		and ESD is the standard deviation.
	 *	One line of:
	 *		S NS
	 *		where NS is the number of shovels.
	 *	NS lines of:
	 *		FM FSG
	 *		where for the nth line,
	 *		FM is the mean filling time for the nth shovel,
	 *		and FSD is the standard deviation.
	 *	One line of:
	 *		R NR N NN
	 *		where NR is the number of roads,
	 *		and NN is the number of nodes in the road network,
	 *		not including shovels and crushers.
	 *	NR lines of:
	 *		n1 i1 n2 i2 TM TSD rt
	 *		where n1 and n2 are nodes types,
	 *		i.e. c for crusher, s for shovel, n for node,
	 *		i1 and i2 are indexes for the respective node type,
	 *		TM is the mean travel time along the road,
	 *		TSD is the standard deviation,
	 *		and rt is the road type,
	 *		i.e. t for two-lane, o for one-lane.
	 * Any deviation from this format will result in error.
	 *
	 * @param	input	the input filename
	 * @throws	IllegalArgumentException 	if the input file is incorrectly formatted,
	 *										or could not be opened.
	 */
	public MineParameters4(String input) {
		try {
			Scanner in = new Scanner(new File(input));
			String[] line = in.nextLine().split(" ");
			if (line.length == 3 && line[0].equals("T")) {
				numTrucks = Integer.parseInt(line[1]);
				fullSlowdown = Double.parseDouble(line[2]);
			}
			else {
				throw new IllegalArgumentException("The input file format is invalid");
			}
			line = in.nextLine().split(" ");
			if (line.length == 2 && line[0].equals("C")) {
				numCrusherLocs = Integer.parseInt(line[1]);
			}
			else {
				throw new IllegalArgumentException("The input file format is invalid");
			}
			numCrushers = new int[numCrusherLocs];
			emptyTimesMean = new double[numCrusherLocs];
			emptyTimesSD = new double[numCrusherLocs];
			for (int i=0; i<numCrusherLocs; i++) {
				line = in.nextLine().split(" ");
				if (line.length == 3) {
					numCrushers[i] = Integer.parseInt(line[0]);
					emptyTimesMean[i] = Double.parseDouble(line[1]);
					emptyTimesSD[i] = Double.parseDouble(line[2]);
				}
				else {
					throw new IllegalArgumentException("The input file format is invalid");
				}
			}
			line = in.nextLine().split(" ");
			if (line.length == 2 && line[0].equals("S")) {
				numShovels = Integer.parseInt(line[1]);
			}
			else {
				throw new IllegalArgumentException("The input file format is invalid");
			}
			fillTimesMean = new double[numShovels];
			fillTimesSD = new double[numShovels];
			for (int i=0; i<numShovels; i++) {
				line = in.nextLine().split(" ");
				if (line.length == 2) {
					fillTimesMean[i] = Double.parseDouble(line[0]);
					fillTimesSD[i] = Double.parseDouble(line[1]);
				}
				else {
					throw new IllegalArgumentException("The input file format is invalid");
				}
			}
			line = in.nextLine().split(" ");
			int numNodes;
			if (line.length == 4 && line[0].equals("R") && line[2].equals("N")) {
				numRoads = Integer.parseInt(line[1]);
				numNodes = Integer.parseInt(line[3]);
			}
			else {
				throw new IllegalArgumentException("The input file format is invalid");
			}
			roadTravelTimesMean = new double[numRoads][2];
			roadTravelTimesSD = new double[numRoads][2];
			isOneWay = new boolean[numRoads];
			ArrayList<ArrayList<int[]>> adjList = new ArrayList<>(numCrusherLocs + numShovels + numNodes);
			for (int i=0; i<numCrusherLocs + numShovels + numNodes; i++) {
				adjList.add(new ArrayList<>());
			}
			for (int i=0; i<numRoads; i++) {
				line = in.nextLine().split(" ");
				if (line.length == 9) {
					int[] ends = new int[2];
					for (int j=0; j<2; j++) {
						int index = Integer.parseInt(line[2 * j + 1]);
						if (index < 0) {
							throw new IllegalArgumentException("The input file format is invalid");
						}
						switch (line[2 * j]) {
							case "c": {
								if (index > numCrusherLocs) {
									throw new IllegalArgumentException("The input file format is invalid");
								}
								ends[j] = index;
								break;
							}
							case "s": {
								if (index > numShovels) {
									throw new IllegalArgumentException("The input file format is invalid");
								}
								ends[j] = index + numCrusherLocs;
								break;
							}
							case "n": {
								if (index > numNodes) {
									throw new IllegalArgumentException("The input file format is invalid");
								}
								ends[j] = index + numCrusherLocs + numShovels;
								break;
							}
							default: {
								throw new IllegalArgumentException("The input file format is invalid");
							}
						}
					}
					for (int j=0; j<2; j++) {
						roadTravelTimesMean[i][j] = Double.parseDouble(line[4 + 2 * j]);
						roadTravelTimesSD[i][j] = Double.parseDouble(line[5 + 2 * j]);
					}
					switch (line[8]) {
						case "o": {
							isOneWay[i] = true;
							break;
						}
						case "t": {
							isOneWay[i] = false;
							break;
						}
						default: {
							throw new IllegalArgumentException("The input file format is invalid");
						}
					}
					for (int j=0; j<2; j++) {
						adjList.get(ends[j]).add(new int[]{ends[1 - j],i,j});
					}
				}
			}
			this.establishRoutes(numNodes,adjList);
			// for (int i=0; i<numRoutes; i++) {
			// 	System.out.printf("%s %s %d %d %d\n",Arrays.toString(routeRoads[i]),Arrays.toString(routeDirections[i]),routeLengths[i],
			// 		routeCrushers[i],routeShovels[i]);
			// }
		}
		catch (FileNotFoundException fnfe) {
			throw new IllegalArgumentException(String.format("The input file could not be found: %s",input));
		}
		catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("The input file format is invalid");
		}
		catch (IndexOutOfBoundsException ioobe) {
			throw new IllegalArgumentException("The input file format is invalid");
		}
		catch (NoSuchElementException nsee) {
			throw new IllegalArgumentException("The input file format is invalid");
		}
	}

	/**
	 * Get the number of trucks.
	 *
	 * @return	the number of trucks.
	 */
	public int getNumTrucks() {
		return numTrucks;
	}

	/**
	 * Set the number of trucks.
	 *
	 * @param	numTrucks	the new number of trucks.
	 */
	public void setNumTrucks(int numTrucks) {
		this.numTrucks = numTrucks;
	}

	/**
	 * Get the number of shovels.
	 *
	 * @return	the number of shovels.
	 */
	public int getNumShovels() {
		return numShovels;
	}

	/**
	 * Get the number of crusher locations.
	 *
	 * @return	the number of crushers.
	 */
	public int getNumCrusherLocs() {
		return numCrusherLocs;
	}

	/**
	 * Get the number of crushers at each location.
	 *
	 * @return	an array of the number of crushers at each location.
	 */
	public int[] getNumCrushers() {
		return Arrays.copyOf(numCrushers,numCrusherLocs);
	}

	/**
	 * Get the number of roads.
	 *
	 * @return	the number of roads.
	 */
	public int getNumRoads() {
		return numRoads;
	}

	/**
	 * Get the mean emptying time of all crusher locations.
	 *
	 * @return	an array of mean emptying times.
	 */
	public double[] getMeanEmptyTimes() {
		return Arrays.copyOf(emptyTimesMean,numCrusherLocs);
	}

	/**
	 * Get the emptying time standard deviation at all crusher locations.
	 *
	 * @return	an array of emptying time SDs.
	 */
	public double[] getEmptyTimesSD() {
		return Arrays.copyOf(emptyTimesSD,numCrusherLocs);
	}

	/**
	 * Get the mean filling time of all shovels.
	 *
	 * @return	an array of mean filling times.
	 */
	public double[] getMeanFillTimes() {
		return Arrays.copyOf(fillTimesMean,numShovels);
	}

	/**
	 * Get the filling time standard deviation of all shovels.
	 *
	 * @return	an array of filling time SDs.
	 */
	public double[] getFillTimesSD() {
		return Arrays.copyOf(fillTimesSD,numShovels);
	}

	/**
	 * Get the mean travelling time of all roads.
	 *
	 * @return	a 2D array of mean travelling times.
	 */
	public double[][] getMeanTravelTimes() {
		double[][] out = new double[numRoads][];
		for (int i=0; i<numRoads; i++) {
			out[i] = Arrays.copyOf(roadTravelTimesMean[i],2);
		}
		return out;
	}

	/**
	 * Get the travelling time standard deviation of all roads.
	 *
	 * @return	a 2D array of travelling time SDs.
	 */
	public double[][] getTravelTimesSD() {
		double[][] out = new double[numRoads][];
		for (int i=0; i<numRoads; i++) {
			out[i] = Arrays.copyOf(roadTravelTimesSD[i],2);
		}
		return out;
	}

	/**
	 * Get the slowdown penalty for full trucks.
	 *
	 * @return	the full slowdown penalty.
	 */
	public double getFullSlowdown() {
		return fullSlowdown;
	}

	/**
	 * Get whether each road is one-lane.
	 *
	 * @return	a boolean array,
	 *			where true indicates a one-lane road.
	 */
	public boolean[] getIsOneWay() {
		return Arrays.copyOf(isOneWay,numRoads);
	}

	/**
	 * Get the number of routes.
	 *
	 * @return	the number of routes.
	 */
	public int getNumRoutes() {
		return numRoutes;
	}

	/**
	 * Get the roads comprising each route.
	 *
	 * @return	a 2D array listing the roads in each route.
	 */
	public int[][] getRouteRoads() {
		int[][] out = new int[numRoutes][];
		for (int i=0; i<numRoutes; i++) {
			out[i] = Arrays.copyOf(routeRoads[i],routeLengths[i]);
		}
		return out;
	}

	/**
	 * Get the number of roads in each route.
	 *
	 * @return	an array of route lengths.
	 */
	public int[] getRouteLengths() {
		return Arrays.copyOf(routeLengths,numRoutes);
	}

	/**
	 * Get the crusher supplied by each route.
	 *
	 * @return	an array of crusher indexes.
	 */
	public int[] getRouteCrushers() {
		return Arrays.copyOf(routeCrushers,numRoutes);
	}

	/**
	 * Get the shovel supplied by each route.
	 *
	 * @return	an array of shovel indexes.
	 */
	public int[] getRouteShovels() {
		return Arrays.copyOf(routeShovels,numRoutes);
	}

	/**
	 * Get the direction travelled along each road of each route.
	 *
	 * @return	a 2D array listing the directions of the roads in each route.
	 */
	public int[][] getRouteDirections() {
		int[][] out = new int[numRoutes][];
		for (int i=0; i<numRoutes; i++) {
			out[i] = Arrays.copyOf(routeDirections[i],routeLengths[i]);
		}
		return out;
	}

	/**
	 * Determine the routes available.
	 *
	 * Note that the simulator makes no checks about route correctness,
	 * and assumes the route information created by this method is accurate.
	 *
	 * Each created route is comprised of a list of road-direction pairs.
	 *
	 * @param	numNodes	the number of nodes
	 * @param	adjList		a list of adjacency lists,
	 *						where the first NC lists correspond to the crushers,
	 *						the next NS lists correspond to the shovels,
	 *						and the remaining lists correspond to the nodes.
	 *						each adjacency lists contains tuples,
	 *						where the first value is an index (using the scheme just described),
	 *						the second is a road index,
	 *						and the third is direction of the road from the first to the second (0 or 1).
	 */
	protected void establishRoutes(int numNodes, ArrayList<ArrayList<int[]>> adjList) {
		ArrayList<int[]> routes = new ArrayList<>();
		ArrayList<int[]> directions = new ArrayList<>();
		boolean[] seen = new boolean[numCrusherLocs + numShovels + numNodes];
		IntQueue currentRoute = new IntQueue();
		IntQueue currentDirections = new IntQueue();
		for (int i=0; i<numCrusherLocs; i++) {
			dfs(i,i,seen,currentRoute,currentDirections,routes,directions,adjList);
		}
		numRoutes = routes.size();
		routeRoads = new int[numRoutes][];
		routeShovels = new int[numRoutes];
		routeCrushers = new int[numRoutes];
		routeLengths = new int[numRoutes];
		routeDirections = new int[numRoutes][];
		for (int i=0; i<numRoutes; i++) {
			int[] route = routes.get(i);
			int[] dir = directions.get(i);
			routeLengths[i] = route.length - 2;
			routeRoads[i] = new int[routeLengths[i]];
			routeDirections[i] = new int[routeLengths[i]];
			for (int j=0; j<routeLengths[i]; j++) {
				routeRoads[i][j] = route[j];
				routeDirections[i][j] = dir[j];
			}
			routeCrushers[i] = route[routeLengths[i]];
			routeShovels[i] = route[routeLengths[i] + 1];
		}
	}

	/**
	 * A recursive DFS to create routes.
	 *
	 * @param	node				the current node to examine.
	 * @param	cid					the crusher index at the start of the route.
	 * @param	seen				the visited array for the stack.
	 * @param	currentRoute		the list of roads currently on the stack.
	 * @param	currentDirections	the list of directions for the roads currently on the stack.
	 * @param	routes				the lists of roads for the complete routes found so far.
	 * @param	directions			the lists of directions for the complete routes founds so far.
	 * @param	adjList				the list of adjacency lists.
	 * @see	establishRoutes
	 */
	private void dfs(int node, int cid, boolean[] seen, IntQueue currentRoute, IntQueue currentDirections, ArrayList<int[]> routes, 
		ArrayList<int[]> directions, ArrayList<ArrayList<int[]>> adjList) {
		if (node < numCrusherLocs) {
			if (node != cid) {
				return;
			}
		}
		else if (node - numCrusherLocs < numShovels) {
			int length = currentRoute.size();
			int[] route = new int[length + 2];
			int[] dir = new int[length];
			for (int i=0; i<length; i++) {
				route[i] = currentRoute.get(i);
				dir[i] = currentDirections.get(i);
			}
			route[length] = cid;
			route[length + 1] = node - numCrusherLocs;
			routes.add(route);
			directions.add(dir);
			return;
		}
		seen[node] = true;
		for (int[] adj : adjList.get(node)) {
			if (!seen[adj[0]]) {
				currentRoute.add(adj[1]);
				currentDirections.add(adj[2]);
				dfs(adj[0],cid,seen,currentRoute,currentDirections,routes,directions,adjList);
				currentRoute.pollLast();
				currentDirections.pollLast();
			}
		}
		seen[node] = false;
	}

}