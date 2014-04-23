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
 * Checks if the candidate phrase was present in the references part of its document.
 */
public class PresentInReferencesFeature extends Feature {

  private static final long serialVersionUID = 5443765973643060417L;

  public PresentInReferencesFeature() {
    scale = Scale.BINARY;
    collectionToStoreDocVals = HashSet.class;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    int presentInReferences = ngramForm.getValue().isPresentInReferences() ? 1 : 0;
    updateFeatureVals(presentInReferences, docToCheck);
  }

}
