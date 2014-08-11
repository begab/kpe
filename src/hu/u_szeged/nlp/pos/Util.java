package hu.u_szeged.nlp.pos;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.stanford.nlp.io.IOUtils;

public class Util {

  /**
   * adott szo csak irasjeleket tartalmaz-e
   */
  public static boolean isPunctation(String spelling) {
    for (int i = 0; i < spelling.length(); ++i) {
      if (Character.isLetterOrDigit(spelling.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * 16 15-18 minden szam < 32
   */
  public static boolean isDate(String spelling) {
    for (String s : spelling.split("-")) {
      if (Integer.parseInt(s) > 31) {
        return false;
      }
    }
    return true;
  }

  static Map<String, Set<MorAna>> readCorpus(String file) {
    BufferedReader reader = null;
    String line = null;
    Set<MorAna> morAnas = null;
    String[] splitted = null;

    Map<String, Set<MorAna>> corpus = null;
    corpus = new TreeMap<String, Set<MorAna>>();

    try {
      reader = new BufferedReader(new InputStreamReader(IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(file), "UTF-8"));
      while ((line = reader.readLine()) != null) {
        morAnas = new TreeSet<MorAna>();
        splitted = line.split("\t");
        for (int i = 1; i < splitted.length - 1; i++) {
          morAnas.add(new MorAna(splitted[i], splitted[i + 1]));
          i++;
        }
        corpus.put(splitted[0], morAnas);
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return corpus;
  }

  static Map<String, Integer> readFrequencies(String file) {
    BufferedReader reader = null;
    String line = null;
    String[] splitted = null;

    Map<String, Integer> frequencies = null;
    frequencies = new TreeMap<String, Integer>();

    try {
      reader = new BufferedReader(new InputStreamReader(IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(file), "UTF-8"));
      while ((line = reader.readLine()) != null) {
        splitted = line.split("\t");
        frequencies.put(splitted[0], Integer.parseInt(splitted[1]));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return frequencies;
  }

  public static Set<String> loadPunctations() {
    Set<String> punctations = null;
    punctations = new HashSet<String>();

    String[] puncts = { "!", ",", "-", ".", ":", ";", "?", "–" };

    for (String punct : puncts) {
      punctations.add(punct);
    }

    return punctations;
  }

  public static Set<String> loadMorPhonDir() {
    Set<String> morPhonDir = null;
    morPhonDir = new HashSet<String>();
    String[] morPhons = new String[] { "talány", "némber", "sün", "fal", "holló", "felhő", "kalap", "hely", "köd" };

    for (String morPhon : morPhons) {
      morPhonDir.add(morPhon);
    }

    return morPhonDir;
  }

  public static Map<String, String> readCorrDic(String file) {
    BufferedReader reader = null;
    String line = null;
    String[] splitted = null;

    Map<String, String> dictionary = null;
    dictionary = new TreeMap<String, String>();

    try {
      reader = new BufferedReader(new InputStreamReader(IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(file), "UTF-8"));

      while ((line = reader.readLine()) != null) {
        splitted = line.split("\t");
        dictionary.put(splitted[0], splitted[1]);
      }

    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return dictionary;
  }
}
