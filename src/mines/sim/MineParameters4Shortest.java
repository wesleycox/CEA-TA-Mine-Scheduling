package mines.sim;

import mines.util.IntList;
import java.util.*;

/**
 * Structure for simulation and routing parameters.
 * Overrides route creation to use shortest-path routing to each shovel.
 */
public class MineParameters4Shortest extends MineParameters4 {

	public MineParameters4Shortest(String input) {
		super(input);
	}

	private static final double INFINITY = 1e9;

	private static class Node implements Comparable<Node> {

		int index;
		double weight;

		public Node(int i, double w) {
			this.index = i;
			this.weight = w;
		}

		public int compareTo(Node other) {
			return Double.compare(this.weight,other.weight);
		}
	}

	@Override
	protected void establishRoutes(int numNodes, ArrayList<ArrayList<int[]>> adjList) {
		ArrayList<int[]> routes = new ArrayList<>();
		ArrayList<int[]> directions = new ArrayList<>();
		for (int i=0; i<numCrusherLocs; i++) {
			dijkstras(i,routes,directions,numNodes,adjList);
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
	 * Uses Dijkstras shortest path algorithm to create routes.
	 *
	 * @param	cid			the crusher index at the start of the route.
	 * @param	routes		the lists of roads for the complete routes found so far.
	 * @param	directions	the lists of directions for the complete routes founds so far.
	 * @param	numNodes	the number of nodes in the road network, not including shovels and crushers.
	 * @param	adjList		the list of adjacency lists.
	 * @see	establishRoutes
	 */
	private void dijkstras(int cid, ArrayList<int[]> routes, ArrayList<int[]> directions, int numNodes, ArrayList<ArrayList<int[]>> adjList) {
		double[] dist = new double[numShovels + numNodes];
		int[] prevNode = new int[numShovels + numNodes];
		int[] prevRoad = new int[numShovels + numNodes];
		int[] prevDir = new int[numShovels + numNodes];
		boolean[] visited = new boolean[numShovels + numNodes];
		for (int i=0; i<numShovels + numNodes; i++) {
			dist[i] = INFINITY;
			prevRoad[i] = -1;
			prevDir[i] = -1;
			prevNode[i] = -1;
		}
		PriorityQueue<Node> q = new PriorityQueue<>();
		for (int[] adj : adjList.get(cid)) {
			int index = adj[0];
			int road = adj[1];
			int dir = adj[2];
			double roadTime = roadTravelTimesMean[road][dir] * (isOneWay[road] ? 2 : 1);
			dist[index - numCrusherLocs] = roadTime;
			prevRoad[index - numCrusherLocs] = road;
			prevDir[index - numCrusherLocs] = dir;
			q.add(new Node(index - numCrusherLocs,roadTime));
		}
		while (!q.isEmpty()) {
			Node out = q.poll();
			if (!visited[out.index]) {
				visited[out.index] = true;
				if (out.index >= numShovels) {
					for (int[] adj : adjList.get(out.index + numCrusherLocs)) {
						int index = adj[0];
						if (index >= numCrusherLocs && !visited[index - numCrusherLocs]) {
							int road = adj[1];
							int dir = adj[2];
							double roadTime = roadTravelTimesMean[road][dir] * (isOneWay[road] ? 2 : 1);
							double alt = dist[out.index] + roadTime;
							if (alt < dist[index - numCrusherLocs]) {
								dist[index - numCrusherLocs] = alt;
								prevRoad[index - numCrusherLocs] = road;
								prevDir[index - numCrusherLocs] = dir;
								prevNode[index - numCrusherLocs] = out.index;
								q.add(new Node(index - numCrusherLocs,alt));
							} 
						}
					}
				}
			}
		}
		for (int i=0; i<numShovels; i++) {
			if (dist[i] < INFINITY) {
				IntList route = new IntList();
				IntList direction = new IntList();
				int current = i;
				while (current >= 0) {
					int road = prevRoad[current];
					int dir = prevDir[current];
					route.add(road);
					direction.add(dir);
					current = prevNode[current];
				}
				int length = route.size();
				int[] ra = new int[length + 2];
				int[] da = new int[length];
				for (int j=0; j<length; j++) {
					ra[j] = route.get(length - 1 - j);
					da[j] = direction.get(length - 1 - j);
				}
				ra[length] = cid;
				ra[length + 1] = i;
				routes.add(ra);
				directions.add(da);
			}
		}
	}

}