package hu.u_szeged.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NLPUtils {

  private static final String DEFAULT_CHARSET = "UTF-8";
  private static Charset charset = Charset.forName(DEFAULT_CHARSET);
  public static double log2 = Math.log(2);
  public static double SMALL = 1e-6;

  /**
   * Computes the mean for an array of Numbers.
   * 
   * @param vector
   *          the array
   * @return the mean
   */
  public static/* @pure@ */double mean(Number[] vector) {

    double sum = 0;

    if (vector.length == 0) {
      return 0;
    }
    for (int i = 0; i < vector.length; i++) {
      sum += vector[i].doubleValue();
    }
    return sum / (double) vector.length;
  }

  public static/* @pure@ */double mean(Collection<Number> vector) {
    return mean(vector.toArray(new Number[vector.size()]));
  }

  public static/* @pure@ */double mean(double[] vector) {

    double sum = 0;

    if (vector.length == 0) {
      return 0;
    }
    for (int i = 0; i < vector.length; i++) {
      sum += vector[i];
    }
    return sum / (double) vector.length;
  }

  public static/* @pure@ */double[] meanAndVariance(Collection<Number> vector) {
    return meanAndVariance(vector.toArray(new Number[vector.size()]), true);
  }

  public static/* @pure@ */double[] meanAndVariance(Collection<Number> vector, boolean biased) {
    return meanAndVariance(vector.toArray(new Number[vector.size()]), biased);
  }

  public static/* @pure@ */double[] meanAndVariance(Number[] vector, boolean biased) {

    double sum = 0.0d, ssum = 0.0d;

    if (vector.length == 0) {
      return new double[] { 0.0d, 0.0d };
    }
    for (int i = 0; i < vector.length; i++) {
      sum += vector[i].doubleValue();
      ssum += vector[i].doubleValue() * vector[i].doubleValue();
    }
    double mean = sum / (double) vector.length;
    double variance = (ssum / (double) vector.length) - (mean * mean);
    if (!biased) {
      variance *= vector.length / (double) (vector.length - 1);
    }
    return new double[] { mean, variance };
  }

  /**
   * Returns the kth-smallest value in the array.
   * 
   * @param array
   *          the array of integers
   * @param k
   *          the value of k
   * @return the kth-smallest value
   */
  public static double kthSmallestValue(int[] array, int k) {

    int[] index = new int[array.length];

    for (int i = 0; i < index.length; i++) {
      index[i] = i;
    }

    return array[index[select(array, index, 0, array.length - 1, k)]];
  }

  /**
   * Returns the kth-smallest value in the array
   * 
   * @param array
   *          the array of double
   * @param k
   *          the value of k
   * @return the kth-smallest value
   */
  public static double kthSmallestValue(double[] array, int k) {

    int[] index = new int[array.length];

    for (int i = 0; i < index.length; i++) {
      index[i] = i;
    }

    return array[index[select(array, index, 0, array.length - 1, k)]];
  }

  /**
   * Returns index of maximum element in a given array of doubles. First maximum is returned.
   * 
   * @param doubles
   *          the array of doubles
   * @return the index of the maximum element
   */
  public static/* @pure@ */int maxIndex(double[] doubles) {

    double maximum = 0;
    int maxIndex = 0;

    for (int i = 0; i < doubles.length; i++) {
      if ((i == 0) || (doubles[i] > maximum)) {
        maxIndex = i;
        maximum = doubles[i];
      }
    }

    return maxIndex;
  }

  /**
   * Returns index of maximum element in a given array of integers. First maximum is returned.
   * 
   * @param ints
   *          the array of integers
   * @return the index of the maximum element
   */
  public static/* @pure@ */int maxIndex(int[] ints) {

    int maximum = 0;
    int maxIndex = 0;

    for (int i = 0; i < ints.length; i++) {
      if ((i == 0) || (ints[i] > maximum)) {
        maxIndex = i;
        maximum = ints[i];
      }
    }

    return maxIndex;
  }

  /**
   * Returns index of minimum element in a given array of integers. First minimum is returned.
   * 
   * @param ints
   *          the array of integers
   * @return the index of the minimum element
   */
  public static/* @pure@ */int minIndex(int[] ints) {

    int minimum = 0;
    int minIndex = 0;

    for (int i = 0; i < ints.length; i++) {
      if ((i == 0) || (ints[i] < minimum)) {
        minIndex = i;
        minimum = ints[i];
      }
    }

    return minIndex;
  }

  /**
   * Returns index of minimum element in a given array of doubles. First minimum is returned.
   * 
   * @param doubles
   *          the array of doubles
   * @return the index of the minimum element
   */
  public static/* @pure@ */int minIndex(double[] doubles) {

    double minimum = 0;
    int minIndex = 0;

    for (int i = 0; i < doubles.length; i++) {
      if ((i == 0) || (doubles[i] < minimum)) {
        minIndex = i;
        minimum = doubles[i];
      }
    }

    return minIndex;
  }

  private static int select(/* @non_null@ */double[] array, /* @non_null@ */int[] index, int left, int right, int k) {
    if (left == right) {
      return left;
    } else {
      int middle = partition(array, index, left, right);
      if ((middle - left + 1) >= k) {
        return select(array, index, left, middle, k);
      } else {
        return select(array, index, middle + 1, right, k - (middle - left + 1));
      }
    }
  }

  private static int select(/* @non_null@ */int[] array, /* @non_null@ */int[] index, int left, int right, int k) {
    if (left == right) {
      return left;
    } else {
      int middle = partition(array, index, left, right);
      if ((middle - left + 1) >= k) {
        return select(array, index, left, middle, k);
      } else {
        return select(array, index, middle + 1, right, k - (middle - left + 1));
      }
    }
  }

  public <T extends Comparable<? super T>> void sort(T[] arr) {

    // do quicksort
    Arrays.sort(arr);

    Collection<T> list = new ArrayList<T>();
    int i;
    for (i = 0; i < arr.length - 1; i++) {
      if (arr[i].compareTo(arr[i + 1]) != 0) { // if not duplicate, add to the list
        list.add(arr[i]);
      }
    }
    list.add(arr[i]); // add last element
    // btw how do You know that last is not duplicate

  }

  private static <T extends Comparable<? super T>> int partition(T[] array, int[] index, int l, int r) {
    T pivot = array[index[(l + r) / 2]];
    int help;
    while (l < r) {
      while (array[index[l]].compareTo(pivot) < 0 && l < r) {
        l++;
      }
      while (array[index[r]].compareTo(pivot) > 0 && l < r) {
        r--;
      }
      if (l < r) {
        help = index[l];
        index[l] = index[r];
        index[r] = help;
        l++;
        r--;
      }
    }
    if (l == r && array[index[r]].compareTo(pivot) > 0) {
      r--;
    }
    return r;
  }

  private static int partition(double[] array, int[] index, int l, int r) {
    double pivot = array[index[(l + r) / 2]];
    int help;
    while (l < r) {
      while ((array[index[l]] < pivot) && (l < r)) {
        l++;
      }
      while ((array[index[r]] > pivot) && (l < r)) {
        r--;
      }
      if (l < r) {
        help = index[l];
        index[l] = index[r];
        index[r] = help;
        l++;
        r--;
      }
    }
    if ((l == r) && (array[index[r]] > pivot)) {
      r--;
    }
    return r;
  }

  private static int partition(int[] array, int[] index, int l, int r) {
    double pivot = array[index[(l + r) / 2]];
    int help;
    while (l < r) {
      while ((array[index[l]] < pivot) && (l < r)) {
        l++;
      }
      while ((array[index[r]] > pivot) && (l < r)) {
        r--;
      }
      if (l < r) {
        help = index[l];
        index[l] = index[r];
        index[r] = help;
        l++;
        r--;
      }
    }
    if ((l == r) && (array[index[r]] > pivot)) {
      r--;
    }
    return r;
  }

  public static/* @pure@ */double xlogx(double c) {
    if (c == 0) {
      return 0.0;
    }
    return c * NLPUtils.log2((double) c);
  }

  public static/* @pure@ */double log2(double a) {
    return Math.log(a) / log2;
  }

  public static <T extends Comparable<? super T>> int[] stableSort(T[] array) {
    int[] index = new int[array.length];
    int[] newIndex = new int[array.length];
    int[] helpIndex;
    int numEqual;

    array = (T[]) array.clone();
    for (int i = 0; i < index.length; i++) {
      index[i] = i;
    }
    quickSort(array, index, 0, array.length - 1);
    // Make sort stable
    int i = 0;
    while (i < index.length) {
      numEqual = 1;
      for (int j = i + 1; ((j < index.length) && array[index[i]].equals(array[index[j]])); j++)
        numEqual++;
      if (numEqual > 1) {
        helpIndex = new int[numEqual];
        for (int j = 0; j < numEqual; j++)
          helpIndex[j] = i + j;
        quickSort(index, helpIndex, 0, numEqual - 1);
        for (int j = 0; j < numEqual; j++)
          newIndex[i + j] = index[helpIndex[j]];
        i += numEqual;
      } else {
        newIndex[i] = index[i];
        i++;
      }
    }
    return newIndex;
  }

  public static/* @pure@ */int[] stableSort(double[] array) {
    int[] index = new int[array.length];
    int[] newIndex = new int[array.length];
    int[] helpIndex;
    int numEqual;

    array = (double[]) array.clone();
    for (int i = 0; i < index.length; i++) {
      index[i] = i;
      if (Double.isNaN(array[i])) {
        array[i] = Double.MAX_VALUE;
      }
    }
    quickSort(array, index, 0, array.length - 1);
    // Make sort stable
    int i = 0;
    while (i < index.length) {
      numEqual = 1;
      for (int j = i + 1; ((j < index.length) && eq(array[index[i]], array[index[j]])); j++)
        numEqual++;
      if (numEqual > 1) {
        helpIndex = new int[numEqual];
        for (int j = 0; j < numEqual; j++)
          helpIndex[j] = i + j;
        quickSort(index, helpIndex, 0, numEqual - 1);
        for (int j = 0; j < numEqual; j++)
          newIndex[i + j] = index[helpIndex[j]];
        i += numEqual;
      } else {
        newIndex[i] = index[i];
        i++;
      }
    }
    return newIndex;
  }

  private static <T extends Comparable<? super T>> void quickSort(T[] array, int[] index, int left, int right) {
    if (left < right) {
      int middle = partition(array, index, left, right);
      quickSort(array, index, left, middle);
      quickSort(array, index, middle + 1, right);
    }
  }

  private static void quickSort(/* @non_null@ */double[] array, /* @non_null@ */int[] index, int left, int right) {
    if (left < right) {
      int middle = partition(array, index, left, right);
      quickSort(array, index, left, middle);
      quickSort(array, index, middle + 1, right);
    }
  }

  private static void quickSort(/* @non_null@ */int[] array, /* @non_null@ */int[] index, int left, int right) {
    if (left < right) {
      int middle = partition(array, index, left, right);
      quickSort(array, index, left, middle);
      quickSort(array, index, middle + 1, right);
    }
  }

  public static/* @pure@ */boolean grOrEq(double a, double b) {
    return (b - a < SMALL);
  }

  public static/* @pure@ */boolean eq(double a, double b) {
    return (a - b < SMALL) && (b - a < SMALL);
  }

  public static/* @pure@ */double variance(double[] vector) {
    double sum = 0, sumSquared = 0;

    if (vector.length <= 1) {
      return 0;
    }
    for (int i = 0; i < vector.length; i++) {
      sum += vector[i];
      sumSquared += (vector[i] * vector[i]);
    }
    double result = (sumSquared - (sum * sum / (double) vector.length)) / (double) (vector.length - 1);

    // We don't like negative variance
    if (result < 0) {
      return 0;
    } else {
      return result;
    }
  }

  public static List<List<String>> readAsList(String file, String delimiter, Charset characterSet) {
    charset = characterSet;
    return readAsList(file, delimiter);
  }

  public static List<List<String>> readAsList(String file, String delimiter) {
    List<List<String>> list = new LinkedList<List<String>>();
    String line;
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
      while ((line = br.readLine()) != null)
        list.add(Arrays.asList(line.split(delimiter)));
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return list;
  }

  public static void readDocToCollection(String file, Collection<String> collection, Charset chSet) {
    charset = chSet;
    readDocToCollection(file, collection);
  }

  public static void readDocToCollection(String file, Collection<String> collection) {
    try {
      readDocToCollection(new FileInputStream(new File(file)), collection, charset);
    } catch (FileNotFoundException e) {
      System.err.println("Error during reading input stream " + file);
    }
  }

  public static void readDocToCollection(File file, Collection<String> collection, Charset chSet) {
    charset = chSet;
    readDocToCollection(file, collection);
  }

  public static void readDocToCollection(File file, Collection<String> collection) {
    try {
      readDocToCollection(new FileInputStream(file), collection, charset);
    } catch (FileNotFoundException e) {
      System.err.println("Error during reading input stream " + file);
      e.printStackTrace();
    }
  }

  public static void readDocToCollection(InputStream is, Collection<String> collection) {
    String line;
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(is, charset));
      while ((line = br.readLine()) != null)
        collection.add(line);
      br.close();
    } catch (NullPointerException ne) {
      System.err.println("Collection to add elements into not initialized...");
    } catch (IOException e) {
      System.err.println("Error during reading input stream " + is);
    }
  }

  public static void readDocToCollection(InputStream is, Collection<String> collection, Charset chSet) {
    charset = chSet;
    readDocToCollection(is, collection);
  }

  public static Map<String, Integer> readDocToMap(String file, Charset chSet) {
    charset = chSet;
    return readDocToMap(file, chSet);
  }

  public static Map<String, Integer> readDocToMap(String file) {
    Map<String, Integer> map = new HashMap<String, Integer>();
    String line;
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
      while ((line = br.readLine()) != null) {
        String[] split = line.split("\t");
        if (split.length == 2)
          map.put(split[0], Integer.parseInt(split[1]));
      }
      br.close();
    } catch (Exception e) {
      System.err.println("Error during reading file " + file);
    }
    return map;
  }

  public static void serialize(Object obj, String file) {
    try {
      int lastDash = Math.max(file.lastIndexOf('\\'), file.lastIndexOf('/'));
      if (lastDash != -1)
        new File(file.substring(0, lastDash)).mkdirs();
      ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
      out.writeObject(obj);
      out.flush();
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Joins an array of strings to a single string.
   */
  public static String join(String[] str) {
    return join(str, ' ');
  }

  public static String join(String[] str, char joiner) {
    if (str.length == 0)
      return "";
    StringBuffer result = new StringBuffer(str[0]);
    for (int i = 1; i < str.length; i++)
      result.append(joiner + str[i]);
    return result.toString();
  }

  public static String join(AbstractSequentialList<String> str) {
    return join(str, ' ');
  }

  public static String join(AbstractSequentialList<String> str, char joiner) {
    return join(str.toArray(new String[str.size()]), joiner);
  }
}
