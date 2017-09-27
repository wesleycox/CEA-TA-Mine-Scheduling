package mines.sim;

import mines.util.*;
import java.util.*;

/**
 * Monte Carlo simulator for estimating road clear times on one-lane roads.
 */
public final class SharedTimeEstimator {

	private enum EventKind {
		ARRIVED_LEFT, CLEARED_LEFT, ARRIVED_RIGHT, CLEARED_RIGHT
	}

	private static class Event implements Comparable<Event> {

		double time;
		EventKind kind;

		public Event(double time, EventKind kind) {
			this.time = time;
			this.kind = kind;
		}

		public int compareTo(Event other) {
			return Double.compare(this.time,other.time);
		}
	}

	private static final double NOISE = 0.05;
	private static final int NUM_SAMPLES = 100000;

	private SharedTimeEstimator() {}

	// public static double[] estimateTravelTimes(double[] length, double[] flow) {
	// 	return estimateTravelTimes(length,flow,(new double[2]));
	// }

	/**
	 * Estimate the clear times for a one-lane road in both directions,
	 * using Monte Carlo simulation.
	 *
	 * @param	length		an array of the average travel times in both directions.
	 * @param	flow		an array of the average truck flow in both directions.
	 * @param	lengthSD	an array of the travel times standard deviations in both directions.
	 * @return	an array of clear times in both directions.
	 */
	public static double[] estimateTravelTimes(double[] length, double[] flow, double[] lengthSD) {
		if (flow[0] == 0 || flow[1] == 0) {
			return Arrays.copyOf(length,2);
		}
		DoubleList[] samples = new DoubleList[2];
		for (int i=0; i<2; i++) {
			samples[i] = new DoubleList();
		}
		PriorityQueue<Event> events = new PriorityQueue<>();
		for (int i=0; i<2; i++) {
			double last = 0;
			for (int j=0; j<NUM_SAMPLES; j++) {
				double mult = 1 + Math.random() * 2 * NOISE - NOISE;
				last += mult / flow[i];
				events.add(new Event(last,(i == 0 ? EventKind.ARRIVED_LEFT : EventKind.ARRIVED_RIGHT)));
			}
		}
		int stage = (events.peek().kind == EventKind.ARRIVED_LEFT ? 0 : 2);
		int onRoad = 0;
		DoubleQueue[] waiting = new DoubleQueue[2];
		for (int i=0; i<2; i++) {
			waiting[i] = new DoubleQueue();
		}
		for (int i=0; i<NUM_SAMPLES; i++) {
			Event next = events.poll();
			switch (next.kind) {
				case ARRIVED_LEFT: {
					switch (stage) {
						case 0: {
							double time = randomTime(length[0],lengthSD[0]);
							events.add(new Event(next.time + /*length[0]*/time,EventKind.CLEARED_LEFT));
							onRoad++;
							samples[0].add(/*length[0]*/time);
							break;
						}
						case 2: {
							if (onRoad == 0) {
								double time = randomTime(length[0],lengthSD[0]);
								stage = 0;
								events.add(new Event(next.time + /*length[0]*/time,EventKind.CLEARED_LEFT));
								onRoad++;
								samples[0].add(/*length[0]*/time);
								break;
							}
							stage = 3;
						}
						case 1:
						case 3: {
							waiting[0].add(next.time);
							break;
						}
					}
					break;
				}
				case ARRIVED_RIGHT: {
					switch (stage) {
						case 2: {
							double time = randomTime(length[1],lengthSD[1]);
							events.add(new Event(next.time + /*length[1]*/time,EventKind.CLEARED_RIGHT));
							onRoad++;
							samples[1].add(/*length[1]*/time);
							break;
						}
						case 0: {
							if (onRoad == 0) {
								double time = randomTime(length[1],lengthSD[1]);
								stage = 2;
								events.add(new Event(next.time + /*length[1]*/time,EventKind.CLEARED_RIGHT));
								onRoad++;
								samples[1].add(/*length[1]*/time);
								break;
							}
							stage = 1;
						}
						case 1:
						case 3: {
							waiting[1].add(next.time);
							break;
						}
					}
					break;
				}
				case CLEARED_LEFT: {
					onRoad--;
					if (onRoad == 0 && stage == 1) {
						stage = (waiting[0].isEmpty() ? 2 : 3);
						while (!waiting[1].isEmpty()) {
							double time = randomTime(length[1],lengthSD[1]);
							double joined = waiting[1].poll();
							events.add(new Event(next.time + /*length[1]*/time,EventKind.CLEARED_RIGHT));
							onRoad++;
							samples[1].add(next.time + /*length[1]*/time - joined);
						}
					}
					break;
				}
				case CLEARED_RIGHT: {
					onRoad--;
					if (onRoad == 0 && stage == 3) {
						stage = (waiting[1].isEmpty() ? 0 : 1);
						while (!waiting[0].isEmpty()) {
							double time = randomTime(length[0],lengthSD[0]);
							double joined = waiting[0].poll();
							events.add(new Event(next.time + /*length[0]*/time,EventKind.CLEARED_LEFT));
							onRoad++;
							samples[0].add(next.time + /*length[0]*/time - joined);
						}
					}
					break;
				}
			}
		}
		double[] stats0 = getStats(samples[0]);
		double[] stats1 = getStats(samples[1]);
		// return new double[]{getAverage(samples[0]),getAverage(samples[1])};
		return new double[]{stats0[0],stats1[0],stats0[1],stats1[1]};
	}

	/**
	 * Get a random value from a uniform distribution.
	 *
	 * @param	m	the mean.
	 * @param	sd	the standard deviation.
	 * @return	a random value.
	 */
	private static double randomTime(double m, double sd) {
		return m + Math.sqrt(3) * sd * (2 * Math.random() - 1);
	}

	/**
	 * Get the average value of a list.
	 *
	 * @param	list	a list of doubles.
	 * @return	the average.
	 */
	private static double getAverage(DoubleList list) {
		int length = list.size();
		double average = 0;
		for (int i=0; i<length; i++) {
			average += list.get(i) / length;
		}
		return average;
	}

	/**
	 * Get the mean and standard deviation of a list.
	 *
	 * @param	list	a list of doubles.
	 * @return	an array containing the mean and standard deviation,
	 *			[m,sd].
	 */
	private static double[] getStats(DoubleList list) {
		double average = getAverage(list);
		int length = list.size();
		double var = 0;
		for (int i=0; i<length; i++) {
			var += Math.pow(list.get(i) - average,2) / length;
		}
		return new double[]{average,Math.sqrt(var)};
	}

}