package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.utils.NLPUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.util.CoreMap;

/**
 * Computes the standard deviation based on the positions of the candidate phrase within the document.
 */
public class STDevFeature extends Feature {
  private static final long serialVersionUID = 8928290175581042743L;

  public STDevFeature() {
    scale = Scale.NUMERIC;
    collectionToStoreDocVals = LinkedList.class;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    for (Integer position : ngramForm.getValue().getPositions())
      updateFeatureVals(position, docToCheck);
  }

  protected double aggregation(List<Collection<Number>> docVals, String phrase, boolean train, List<int[]> length) {
    double[] perDocFeatureVals = new double[docVals.size()];
    for (int doc = 0; doc < docVals.size(); ++doc) {
      LinkedList<Number> positions = (LinkedList<Number>) docVals.get(doc);
      double[] relativePositions = new double[positions.size()];
      int i = 0;
      for (Number position : positions) {
        relativePositions[i++] = position.doubleValue() / length.get(doc)[0];
      }

      double mean = NLPUtils.mean(relativePositions), summ = 0.0;
      for (int j = 0; j < relativePositions.length; j++)
        summ += (relativePositions[j] - mean) * (relativePositions[j] - mean);
      perDocFeatureVals[doc] = Math.sqrt(summ / relativePositions.length);
    }
    return NLPUtils.mean(perDocFeatureVals);
  }

}
