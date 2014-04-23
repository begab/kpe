package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.aspirants.NGram;
import hu.u_szeged.kpe.aspirants.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import edu.stanford.nlp.util.CoreMap;

/**
 * Determines the relative first token occurrence of the candidate phrase in its document.
 */
public class FirstIndexFeature extends Feature {

  private static final long serialVersionUID = 7457551102285695438L;

  /**
   * the dummy value should be 1.0 here, since dummy values are applied when the phrase in question is not present in the document at all, indicating
   * the worst relative first occurrence in this case of 1.0d
   */

  public FirstIndexFeature() {
    scale = Scale.NUMERIC;
    dummyValue = 1.0d;
    collectionToStoreDocVals = TreeSet.class;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    double value = ngramForm.getValue().getPositions().get(0) / (double) length[0];
    updateFeatureVals(value, docToCheck);
  }
}
