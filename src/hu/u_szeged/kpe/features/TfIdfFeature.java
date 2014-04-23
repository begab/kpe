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
 * Calculates tf-idf score of a candidate phrase.
 */
public class TfIdfFeature extends Feature {

  private static final long serialVersionUID = -2391316562648124265L;

  public TfIdfFeature() {
    scale = Scale.NUMERIC;
    collectionToStoreDocVals = ArrayList.class;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    // for (Integer position : ngramForm.getValue().getPositions())
    updateFeatureVals(ngramForm.getValue().getPositions().size(), docToCheck);
  }

  protected double aggregation(List<Collection<Number>> docVals, String phrase, boolean train, List<int[]> length) {
    double globalVal = 0.0d;
    Integer[] phraseStats = dict.get(phrase);
    if (phraseStats != null)
      globalVal = (phraseStats[0] == null ? 0 : phraseStats[0]) + (phraseStats[1] == null ? 0 : phraseStats[1]) - (train ? 1 : 0);

    double[] values = new double[docVals.size()];
    for (int i = 0; i < values.length; ++i) {
      double occurrenceInDoc = ((ArrayList<Number>) docVals.get(i)).get(docVals.get(i).size() - 1).doubleValue();
      values[i] = (occurrenceInDoc / length.get(i)[0]) * Math.log((numDoc + 1) / (globalVal + 1));
    }
    return NLPUtils.mean(values);
  }
}
