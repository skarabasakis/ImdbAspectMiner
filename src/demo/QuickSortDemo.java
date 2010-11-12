// The MIT License
//
// Copyright (c) 2010 Stelios Karabasakis
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//
package demo;

import java.util.logging.Level;
import util.AppLogger;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class QuickSortDemo {
	
	public static void main(String[] args)
	{
		int[] array = { 10, 5, 2, 4, 7, 8, 5, 9, 7, 5, 2 };
		int[] mirror = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

		System.out.println(" Array: " + makeString(array) + "\nMirror: " + makeString(mirror));

		QuickSortDemo.quicksort(array, mirror, 1, array.length - 1, true);
		
		System.out.println("\n Array: " + makeString(array) + "\nMirror: " + makeString(mirror));

		QuickSortDemo.stabilize(array, mirror, array.length - 1);
		
		System.out.println("\n Array: " + makeString(array) + "\nMirror: " + makeString(mirror));

	}
	
	public static String makeString(int[] array)
	{
		StringBuilder str = new StringBuilder();
		for (int x : array) {
			str.append(x + "\t");
		}
		
		return str.toString();
	}

	public static void quicksort(int[] array, int[] mirror, int index_low, int index_high, boolean descending)
	{
		int length = index_high - index_low + 1;
		if (length <= 1) {
			// Do nothing
		}
		/*
		 * else if (length == 2) { if (array[index_low] > array[index_high]) { swap(array,
		 * index_low, index_high); swap(mirror, index_low, index_high); } }
		 */
		else {
			int[] original_array = array.clone();
			int[] original_mirror = mirror.clone();
			
			// TODO remove
			for (int i = index_low ; i <= index_high ; i++) {
				array[i] = mirror[i] = 0;
			}

			int index_pivot = (index_low + index_high) / 2;
			
			int pos_low = index_low, pos_high = index_high;
			for (int i = index_low ; i <= index_high ; i++) {
				if (i != index_pivot) {
					// Compare original array value to pivot, and place value on the left or the
					// right side of the partition accordingly
					if (descending ? original_array[i] > original_array[index_pivot]
						: original_array[i] < original_array[index_pivot]) {
						array[pos_low] = original_array[i];
						mirror[pos_low] = original_mirror[i];
						pos_low++;
					}
					else {
						array[pos_high] = original_array[i];
						mirror[pos_high] = original_mirror[i];
						pos_high--;
					}
				}
			}
			// Finally, place pivot value in the last free spot of the partition
			if (pos_low != pos_high) {
				AppLogger.error.log(Level.SEVERE, "Something went wrong in sort method");
				throw new RuntimeException("Something went wrong in sort method");
			}
			array[pos_low] = original_array[index_pivot];
			mirror[pos_low] = original_mirror[index_pivot];

			quicksort(array, mirror, index_low, pos_low - 1, descending);
			quicksort(array, mirror, pos_high + 1, index_high, descending);
		}
	}
	
	public static void stabilize(int[] values, int[] classes, int n_of_classes)
	{
		// For each class in the classes array
		for (int lower_i = 2 ; lower_i <= n_of_classes ; lower_i++) {
			
			// Find if it marks the lower bound of a subarray of consecutive classes that have the
			// same value, and find the class that marks the upper bound of that section
			int upper_i;
			for (upper_i = lower_i + 1 ; upper_i <= n_of_classes ; upper_i++) {
				if (values[upper_i] != values[lower_i]) {
					break;
				}
			}
			
			if (upper_i != lower_i) {
				// Look within the subarray bound by [lower_i, upper_i) and find the class
				// that is closer to the reference class that directly precedes the subarray
				int reference = lower_i - 1;
				int closer_i = lower_i;
				int class_distance = Math.abs(classes[reference] - classes[closer_i]);
				
				for (int candidate_closer_i = lower_i + 1 ; candidate_closer_i < upper_i ; candidate_closer_i++) {
					if (Math.abs(classes[reference] - classes[candidate_closer_i]) < class_distance) {
						closer_i = candidate_closer_i;
						class_distance = Math.abs(classes[reference] - classes[closer_i]);
					}
				}
				
				// Swap positions between classes at positions lower_i and closer_i
				int class_temp = classes[lower_i];
				classes[lower_i] = classes[closer_i];
				classes[closer_i] = class_temp;
			}
		}
	}
}
