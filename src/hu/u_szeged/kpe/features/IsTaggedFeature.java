package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.aspirants.NGram;
import hu.u_szeged.kpe.aspirants.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.ling.CoreAnnotations.XmlContextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

/**
 * Indicates if the candidate phrase was formatted somehow (e.g. with HTML tags or LaTex formatter).
 */
public class IsTaggedFeature extends Feature {

  private static final long serialVersionUID = -2085560624319886333L;

  public IsTaggedFeature() {
    scale = Scale.BINARY;
    collectionToStoreDocVals = HashSet.class;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {

    int ngramSize = ngramForm.getKey().size();
    Map<String, Integer> taggingLengths = new HashMap<String, Integer>();
    for (int position = 0; position < ngramSize; ++position) {
      CoreLabel cl = ngramForm.getKey().get(position);
      String type = ngramSize == 1 ? "S" : (position == 0 ? "B" : (position < ngramSize - 1 ? "I" : "E"));
      for (String tag : new HashSet<String>(cl.get(XmlContextAnnotation.class))) {
        updateFeatureVals(this.getClass().getName() + "_" + type + "_" + tag.toLowerCase(), 1.0d, docToCheck);
        Integer prevVal = taggingLengths.get(tag);
        taggingLengths.put(tag, prevVal == null ? 1 : ++prevVal);
      }
    }
    for (Entry<String, Integer> taggingLength : taggingLengths.entrySet()) {
      if (taggingLength.getValue() == ngramSize)
        updateFeatureVals(this.getClass().getName() + "_" + taggingLength.getKey().toLowerCase(), 1.0d, docToCheck);
    }
  }
}
