package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.aspirants.NGram;
import hu.u_szeged.kpe.aspirants.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.utils.NLPUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.util.CoreMap;

/**
 * The class determines the ratio of the number of reference containing sentences and the total number of sentences both containing a particular
 * orthographic form of an NGram references.
 */
public class HasReferenceFeature extends Feature {

  private static final long serialVersionUID = -2156406089444467312L;
  private List<Integer> sentencesNum;

  public HasReferenceFeature() {
    scale = Scale.NUMERIC;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    if (sentencesNum == null) {
      sentencesNum = new ArrayList<Integer>(docs.length);
      for (int i = 0; i < docs.length; ++i)
        sentencesNum.add(0);
    }
    sentencesNum.set(docToCheck, sentencesNum.get(docToCheck) + ngramForm.getValue().getSentencePositions().size());
    updateFeatureVals(ngramForm.getValue().numWithReferences(), docToCheck);
  }

  protected double aggregation(List<Collection<Number>> docVals, String phrase, boolean train, List<int[]> length) {
    double[] perDocFeatureVals = new double[docVals.size()];
    for (int doc = 0; doc < docVals.size(); ++doc) {
      List<Number> docNums = (ArrayList<Number>) docVals.get(doc);
      if (sentencesNum.get(doc) > 0)
        perDocFeatureVals[doc] = docNums.get(docNums.size() - 1).doubleValue() / sentencesNum.get(doc);
    }
    sentencesNum = null;
    return NLPUtils.mean(perDocFeatureVals);
  }
}
