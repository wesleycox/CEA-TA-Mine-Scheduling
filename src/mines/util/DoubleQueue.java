package mines.util;

import java.util.*;

/**
 * An array-based queue for primitive doubles.
 */
public class DoubleQueue {

	private double[] array;	//the queue.
	private int size;		//the number of elements.
	private int front;		//the index of the first element.

	/**
	 * Create an empty queue.
	 */
	public DoubleQueue() {
		array = new double[11];
		size = 0;
		front = 0;
	}

	/**
	 * Create a new queue with the provided initial elements.
	 *
	 * @param	initial	an array of initial elements.
	 */
	public DoubleQueue(double[] initial) {
		int initialLength = 11;
		while (initialLength < initial.length) {
			initialLength *= 2;
		}
		array = Arrays.copyOf(initial,initialLength);
		front = 0;
		size = initial.length;
	}

	/**
	 * Remove and return the first element in the queue.
	 * 
	 * @return	the front of the queue.
	 * @throws	NoSuchElementException if the queue is empty.
	 */
	public double poll() {
		if (size == 0) {
			throw new NoSuchElementException("Queue is empty");
		}
		double out = array[front];
		size--;
		front = (front + 1) % array.length;
		return out;
	}

	/**
	 * Remove and return the last element in the queue.
	 * 
	 * @return	the end of the queue.
	 * @throws	NoSuchElementException if the queue is empty.
	 */
	public double pollLast() {
		if (size == 0) {
			throw new NoSuchElementException("Queue is empty");
		}
		size--;
		return array[(front + size) % array.length];
	}

	/**
	 * Get whether the list is empty.
	 *
	 * @return	true if the size is 0,
	 *			false otherwise.
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Return but don't remove the first element in the queue.
	 * 
	 * @return	the front of the queue.
	 * @throws	NoSuchElementException if the queue is empty.
	 */
	public double peek() {
		if (size == 0) {
			throw new NoSuchElementException("Queue is empty");
		}
		return array[front];
	}

	/**
	 * Append an element to the end of the queue.
	 *
	 * @param	e	the element.
	 */
	public void add(double e) {
		ensureSize(size + 1);
		int end = (front + size) % array.length;
		array[end] = e;
		size++;
	}

	/**
	 * Place an element to the front of the queue.
	 *
	 * @param	e	the element.
	 */
	public void addFront(double e) {
		ensureSize(size + 1);
		front = (front + array.length - 1) % array.length;
		array[front] = e;
		size++;
	}

	/**
	 * Ensure the underlying array has enough size.
	 *
	 * @param	s	the minimum required size.
	 */
	private void ensureSize(int s) {
		if (s > array.length) {
			int newlength = Math.max(array.length * 2,1);
			while (newlength < s) {
				newlength *= 2;
			}
			double[] newarray = new double[newlength];
			int tail = Math.min(size,array.length - front);
			System.arraycopy(array,front,newarray,0,tail);
			if (tail < size) {
				System.arraycopy(array,0,newarray,tail,size - tail);
			}
			array = newarray;
			front = 0;
		}
	}

	/**
	 * Empty the queue.
	 */
	public void clear() {
		size = 0;
		front = 0;
	}

	/**
	 * Append all elements of a queue to the end of this one.
	 *
	 * @param	c	another DoubleQueue.
	 */
	public void addAll(DoubleQueue c) {
		ensureSize(this.size + c.size);
		int available = (this.front + this.size) % this.array.length;
		int rem = this.array.length - available;
		int first = Math.min(c.size,c.array.length - c.front);
		int second = c.size - first;
		int s1 = Math.min(rem,first);
		System.arraycopy(c.array,c.front,this.array,available,s1);
		if (s1 < first) {
			System.arraycopy(c.array,c.front + s1,this.array,0,first - s1);
			System.arraycopy(c.array,0,this.array,first - s1,second);
		}
		else {
			int s2 = Math.min(rem - first,second);
			System.arraycopy(c.array,0,this.array,available + first,s2);
			if (s2 < second) {
				System.arraycopy(c.array,s2,this.array,0,second - s2);
			}
		}
		this.size += c.size;
	}

	/**
	 * Get the number of elements in the queue.
	 *
	 * @return	the size.
	 */
	public int size() {
		return size;
	}

	/**
	 * Get an element from the queue,
	 * without removing it.
	 *
	 * @param	ind	the index of the element.
	 * @return	the element.
	 * @throws	IndexOutOfBoundException if the index is negative,
	 *			or is greater than the size - 1.
	 */
	public double get(int ind) {
		if (ind < 0 || ind >= size) {
			throw new IndexOutOfBoundsException(String.format("Index out of range: %d",ind));
		}
		int pos = (front + ind) % array.length;
		return array[pos];
	}

	/**
	 * Override an element in the queue.
	 *
	 * @param	ind	the index of the element.
	 * @param	e	the new value.
	 * @throws	IndexOutOfBoundException if the index is negative,
	 *			or is greater than the size - 1.
	 */
	public void set(int ind, double e) {
		if (ind < 0 || ind >= size) {
			throw new IndexOutOfBoundsException(String.format("Index out of range: %d",ind));
		}
		array[(front + ind) % array.length] = e;
	}

}