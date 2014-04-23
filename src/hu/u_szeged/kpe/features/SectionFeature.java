package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.main.KPEFilter;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.utils.NLPUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.util.CoreMap;

/**
 * Calculates sf-isf (section frequency-inverted section frequency) in the form of sf(term, document)*isf(term).
 */
public class SectionFeature extends Feature {

  private static final long serialVersionUID = 7606431493967247562L;
  private int numSections;

  public SectionFeature() {
    scale = Scale.NUMERIC;
    collectionToStoreDocVals = HashSet.class;
  }

  public void setFeatureField(KPEFilter kf) {
    numSections = kf.getNumSections();
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    for (Integer sectionPresent : ngramForm.getValue().getSectionIds())
      updateFeatureVals(sectionPresent, docToCheck);
  }

  protected double aggregation(List<Collection<Number>> docVals, String phrase, boolean train, List<int[]> length) {
    double globalVal = 0.0d;
    double[] globalVals = new double[docVals.size()];
    Integer[] phraseStats = dict.get(phrase);
    if (phraseStats != null)
      globalVal += phraseStats[2];

    for (int doc = 0; train && doc < docVals.size(); ++doc) {
      globalVals[doc] = globalVal - docVals.get(doc).size();
      if (globalVals[doc] < 0) {
        System.err.println("Apparently, we have some problem.");
      }
    }

    double[] values = new double[globalVals.length];
    for (int i = 0; i < globalVals.length; ++i)
      values[i] = ((double) docVals.get(i).size() / length.get(i)[1]) * Math.log((numSections + 1) / (globalVals[i] + 1));
    return NLPUtils.mean(values);
  }
}
