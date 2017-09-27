package mines.util;

import java.util.Arrays;

/**
 * An array-based list for primitive int.
 */
public class IntList {

	private int[] array;	//the list.
	private int size;		//the number of elements in the list.

	/**
	 * Create a new list.
	 */
	public IntList() {
		array = new int[11];
		size = 0;
	}

	/**
	 * Ensure the underlying array has enough size.
	 *
	 * @param	s	the minimum required size.
	 */
	private void ensureSize(int s) {
		if (s > array.length) {
			int newlength = array.length * 2;
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
	public void add(int e) {
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
	 * Get whether the list is empty.
	 *
	 * @return	true if the size is 0,
	 *			false otherwise.
	 */
	public boolean isEmpty() {
		return (size == 0);
	}

	/**
	 * Get an element from the list.
	 *
	 * @param	ind	the index of the element.
	 * @return	the element.
	 * @throws	IndexOutOfBoundException if the index is negative,
	 *			or is greater than the size - 1.
	 */
	public int get(int ind) {
		if (ind >= size || ind < 0) {
			throw new IndexOutOfBoundsException(String.format("Index out of range: %d",ind));
		}
		return array[ind];
	}
	
}