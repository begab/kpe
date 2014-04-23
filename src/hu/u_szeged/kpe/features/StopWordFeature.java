package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.utils.NLPUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StopWordAnnotator.StopWordAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * Indicates how many stopwords the original phrase contained (before having been converted into its normalized form).
 */
public class StopWordFeature extends Feature {

  private static final long serialVersionUID = 8414326148202027597L;
  private List<Integer> occurrences;

  public StopWordFeature() {
    scale = Scale.NUMERIC;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    int numOfOccurrences = ngramForm.getValue().getPositions().size();
    if (occurrences == null) {
      occurrences = new ArrayList<Integer>(docs.length);
      for (int i = 0; i < docs.length; ++i)
        occurrences.add(0);
    }
    occurrences.set(docToCheck, occurrences.get(docToCheck) + numOfOccurrences);

    boolean hasStopWord = false;
    for (CoreLabel ew : ngramForm.getKey())
      hasStopWord = hasStopWord || ew.get(StopWordAnnotation.class);

    if (hasStopWord)
      updateFeatureVals(numOfOccurrences, docToCheck);
  }

  protected double aggregation(List<Collection<Number>> docVals, String phrase, boolean train, List<int[]> length) {
    double[] perDocVals = new double[docVals.size()];
    for (int doc = 0; doc < docVals.size(); ++doc) {
      ArrayList<Number> stopwordsForDoc = (ArrayList<Number>) docVals.get(doc);
      if (stopwordsForDoc.size() > 0 && occurrences.get(doc) > 0) {
        double numHavingStopword = ((Number) (stopwordsForDoc.get(stopwordsForDoc.size() - 1))).doubleValue();
        perDocVals[doc] = numHavingStopword / occurrences.get(doc);
      }
    }
    occurrences = null;
    return NLPUtils.mean(perDocVals);
  }
}
