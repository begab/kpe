package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.util.CoreMap;

/**
 * Determines the number of tokens a candidate phrase consists of.
 */
public class LengthFeature extends Feature {

  private static final long serialVersionUID = -6828991006669939839L;

  public LengthFeature() {
    scale = Scale.NUMERIC;
    dummyValue = -1;
    collectionToStoreDocVals = LinkedList.class;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    updateFeatureVals(ngramForm.getKey().size(), docToCheck);
    updateFeatureVals(ngramForm.getValue().getPositions().size(), docToCheck);
  }

  public Map<String, Double> aggregateVals(boolean train, String token, List<int[]> length, double[] dedicatedFeatures) {
    Map<Integer, Integer> lengthFreqs = new HashMap<Integer, Integer>();
    int sumOccurrence = 0, sumTotalLength = 0, minLength = Integer.MAX_VALUE, maxLength = Integer.MIN_VALUE;
    for (List<Collection<Number>> entry : featureVals.values()) {
      int documents = entry.size();
      doc: for (int i = 0; i < documents; ++i) {
        Iterator<Number> lengthOccurrencesIterator = entry.get(i).iterator();
        while (lengthOccurrencesIterator.hasNext()) {
          int tokenLength = lengthOccurrencesIterator.next().intValue();
          if (tokenLength == 0) {
            break doc;
          }
          if (tokenLength > maxLength) {
            maxLength = tokenLength;
          }
          if (tokenLength < minLength) {
            minLength = tokenLength;
          }
          int freq = lengthOccurrencesIterator.next().intValue();
          Integer prevVal = lengthFreqs.get(tokenLength);
          lengthFreqs.put(tokenLength, (prevVal == null ? 0 : prevVal) + freq);
          sumOccurrence += freq;
          sumTotalLength += freq * tokenLength;
        }
      }
    }
    double mean = sumTotalLength / (double) sumOccurrence;
    double stdev = 0.0;
    for (Entry<Integer, Integer> occurrences : lengthFreqs.entrySet()) {
      stdev += occurrences.getValue() * (occurrences.getKey() - mean) * (occurrences.getKey() - mean);
    }
    stdev = Math.sqrt(stdev / sumOccurrence);
    Map<String, Double> aggregatedVals = new HashMap<String, Double>();
    aggregatedVals.put("TokenLengthMean", mean);
    aggregatedVals.put("TokenLengthMinLength", (double) minLength);
    aggregatedVals.put("TokenLengthMaxLength", (double) maxLength);
    aggregatedVals.put("TokenLengthSTDev", stdev);

    // reset it so the next time a set of documents are to be keyphrased this flag can start to count from the
    // beginning
    documentToExamine = -1;
    featureVals = new HashMap<String, List<Collection<Number>>>();
    return aggregatedVals;
  }

  // public LengthFeature() {
  // scale = Scale.NUMERIC;
  // collectionToStoreDocVals = TreeSet.class;
  // }
  //
  // public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int
  // docToCheck,
  // List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
  // updateFeatureVals(ngramForm.getKey().size(), docToCheck);
  // }
}
