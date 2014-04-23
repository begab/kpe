package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.utils.NLPUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.stanford.nlp.util.CoreMap;

/**
 * Determines the number of occurrences of a candidate phrase as true keyphrase on the training set.
 */
public class KeyFreqFeature extends Feature {

  private static final long serialVersionUID = 2994697250601490879L;

  public KeyFreqFeature() {
    scale = Scale.NUMERIC;
    collectionToStoreDocVals = HashSet.class;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    if (train && docToCheck != documentToExamine)
      updateFeatureVals(docs[docToCheck].getKeyphrases().containsKey(ngramForm.getKey()) ? 1.0d : 0.0d, docToCheck);
  }

  protected double aggregation(List<Collection<Number>> docVals, String phrase, boolean train, List<int[]> length) {
    Integer keyphraseCounter = 0;
    Integer[] globalPhraseStats = dict.get(phrase);
    if (globalPhraseStats != null && globalPhraseStats[1] != null) {
      keyphraseCounter = globalPhraseStats[1];
    }
    double[] vals = new double[docVals.size()];
    for (int c = 0; c < docVals.size(); ++c) {
      Set<Number> val = (HashSet<Number>) docVals.get(c);
      vals[c] = keyphraseCounter - (val.contains(1.0d) ? 1.0d : 0.0d);
    }
    return NLPUtils.mean(vals);
  }
}
