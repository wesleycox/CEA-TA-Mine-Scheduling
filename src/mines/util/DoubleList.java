package mines.util;

import java.util.Arrays;

/**
 * An array-based list for primitive doubles.
 */
public class DoubleList {

	private double[] array;	//the list.
	private int size;		//the number of elements in the list.

	/**
	 * Create a new list.
	 */
	public DoubleList() {
		array = new double[11];
		size = 0;
	}

	/**
	 * Create a new list with the same elements as the provided list.
	 *
	 * @param	initial	the DoubleList to copy.
	 */
	public DoubleList(DoubleList initial) {
		size = initial.size;
		int initialLength = 11;
		while (initialLength < size) {
			initialLength *= 2;
		}
		array = Arrays.copyOf(initial.array,initialLength);
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
			array = Arrays.copyOf(array,newlength);
		}
	}

	/**
	 * Append an element to the end of the list.
	 *
	 * @param	e	the element.
	 */
	public void add(double e) {
		ensureSize(size + 1);
		array[size] = e;
		size++;
	}

	/**
	 * Empty the list.
	 */
	public void clear() {
		size = 0;
	}

	/**
	 * Get the number of elements in the list.
	 *
	 * @return	the size.
	 */
	public int size() {
		return size;
	}

	/**
	 * Get an element from the list.
	 *
	 * @param	ind	the index of the element.
	 * @return	the element.
	 * @throws	IndexOutOfBoundException if the index is negative,
	 *			or is greater than the size - 1.
	 */
	public double get(int ind) {
		if (ind >= size || ind < 0) {
			throw new IndexOutOfBoundsException(String.format("Index out of range: %d",ind));
		}
		return array[ind];
	}

	/**
	 * Get whether the list is empty.
	 *
	 * @return	true if the size is 0,
	 *			false otherwise.
	 */
	public boolean isEmpty() {
		return (size == 0);
	}

	/**
	 * Append all elements of a list to the end of this one.
	 *
	 * @param	list	another DoubleList.
	 */
	public void addAll(DoubleList list) {
		ensureSize(this.size + list.size);
		System.arraycopy(list.array,0,this.array,this.size,list.size);
		this.size += list.size;
	}
}