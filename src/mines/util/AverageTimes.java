package mines.util;

/**
 * Non-random distribution.
 * Always returns the mean.
 */
public final class AverageTimes implements TimeDistribution {

	/**
	 * Returns the mean.
	 *
	 * @param	mean
	 * @param	stdev
	 * @return	mean
	 */
	public double nextTime(final double mean, final double stdev) {
		if (mean < 0) {
			throw new IllegalArgumentException(String.format("Non-negative mean required: %f",mean));
		}
		return mean;
	}

}