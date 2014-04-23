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
 * Determines if a candidate phrase was present in any of the section headers of the document containing it.
 */
public class PresentInSectionHeaderFeature extends Feature {

  private static final long serialVersionUID = 3126702621060345499L;

  public PresentInSectionHeaderFeature() {
    scale = Scale.BINARY;
    collectionToStoreDocVals = HashSet.class;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    int presentInHeading = ngramForm.getValue().getSeenAsHeading();
    updateFeatureVals(presentInHeading, docToCheck);
  }
}
