package mines.sol;

import mines.sim.MineParameters4;

/**
 * Solutions create controllers.
 * All simulation parameters are stored,
 * so the parameters necessary for a particular controller can be used.
 */
public abstract class Solution4 {

	protected final int numTrucks;
	protected final int numShovels;
	protected final int numCrusherLocs;
	protected final int[] numCrushers;
	protected final int numRoads;
	protected final double[] emptyTimesMean;
	protected final double[] emptyTimesSD;
	protected final double[] fillTimesMean;
	protected final double[] fillTimesSD;
	protected final double[][] roadTravelTimesMean;
	protected final double[][] roadTravelTimesSD;
	protected final double fullSlowdown;
	protected final boolean[] isOneWay;

	protected final int numRoutes;
	protected final int[][] routeRoads;
	protected final int[][] routeDirections;
	protected final int[] routeLengths;
	protected final int[] routeShovels;
	protected final int[] routeCrushers;

	/**
	 * Solution constructor.
	 *
	 * @param	params	the simulation parameters.
	 */
	public Solution4(MineParameters4 params) {
		numTrucks = params.getNumTrucks();
		numShovels = params.getNumShovels();
		numCrusherLocs = params.getNumCrusherLocs();
		numCrushers = params.getNumCrushers();
		numRoads = params.getNumRoads();
		emptyTimesMean = params.getMeanEmptyTimes();
		emptyTimesSD = params.getEmptyTimesSD();
		fillTimesMean = params.getMeanFillTimes();
		fillTimesSD = params.getFillTimesSD();
		roadTravelTimesMean = params.getMeanTravelTimes();
		roadTravelTimesSD = params.getTravelTimesSD();
		fullSlowdown = params.getFullSlowdown();
		isOneWay = params.getIsOneWay();

		numRoutes = params.getNumRoutes();
		routeRoads = params.getRouteRoads();
		routeDirections = params.getRouteDirections();
		routeLengths = params.getRouteLengths();
		routeShovels = params.getRouteShovels();
		routeCrushers = params.getRouteCrushers();
	}

	/**
	 * Creates a controller.
	 *
	 * @return	a controller.
	 */
	public abstract Controller4 getController();

	/**
	 * Get the solution name.
	 *
	 * @return	the solution name.
	 */
	public abstract String getSolutionName();
}