package mines.util;

import java.util.concurrent.*;

/**
 * Normal distribution.
 * Uses the Box-Muller method.
 */
public class NormalTimes implements TimeDistribution {

	private double z;
	private boolean generate;

	public NormalTimes() {
		generate = false;
	}

	/**
	 * Generate a normally distributed random value with the given mean and standard deviation.
	 *
	 * @param	mean	the mean.
	 * @param	sd		the standard deviation.
	 * @return	a random value.
	 */
	public double nextTime(final double mean, final double stdev) {
		if (mean < 0) {
			throw new IllegalArgumentException(String.format("Non-negative mean required: %f",mean));
		}
		else if (stdev <= 0) {
			throw new IllegalArgumentException(String.format("Positive standard deviation required: %f",stdev));
		}
		generate = !generate;
		if (generate) {
			double u1 = ThreadLocalRandom.current().nextDouble();
			double u2 = ThreadLocalRandom.current().nextDouble();
			double z0 = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
			z = Math.sqrt(-2 * Math.log(u1)) * Math.sin(2 * Math.PI * u2);
			return Math.max(0,z0 * stdev + mean);
		}
		else {
			return Math.max(0,z * stdev + mean);
		}
	}

}