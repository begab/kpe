package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.aspirants.NGram;
import hu.u_szeged.kpe.aspirants.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.util.CoreMap;

/**
 * Computes whether the candidate phrase was present in the abstract part of its document.
 */
public class PresentInAbstractFeature extends Feature {

  private static final long serialVersionUID = -3849480915661213949L;

  public PresentInAbstractFeature() {
    scale = Scale.BINARY;
    collectionToStoreDocVals = HashSet.class;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    int presentInAbstract = ngramForm.getValue().isPresentInAbstract() ? 1 : 0;
    updateFeatureVals(presentInAbstract, docToCheck);
  }

}
