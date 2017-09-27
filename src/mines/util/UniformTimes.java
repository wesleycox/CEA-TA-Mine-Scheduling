package mines.util;

import java.util.concurrent.*;

/**
 * Uniform distribution.
 */
public class UniformTimes implements TimeDistribution {

	/**
	 * Generate a uniform random value with the given mean and standard deviation.
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
		double min = mean - Math.sqrt(3) * stdev;
		return Math.max(0,mean + Math.sqrt(3) * stdev * (2 * ThreadLocalRandom.current().nextDouble() - 1));
	}

}