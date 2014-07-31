package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.main.KPEFilter;
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
 * Determines the POS sequence for candidate phrases.
 */
public class PosFeature extends NominalFeature {
  private static final long serialVersionUID = 8124439968184109852L;
  private List<Integer> occurrences;

  public PosFeature() {
    scale = Scale.NUMERIC;
    dummyValue = -1;
    canBeRepresentedAsSequential = true;
  }

  public void setFeatureField(KPEFilter kf) {
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    if (documentToExamine == -1) {
      occurrences = new ArrayList<Integer>(docs.length);
      for (int i = 0; i < docs.length; ++i)
        occurrences.add(0);
    }
    int occurrence = ngramForm.getValue().getPositions().size();
    occurrences.set(docToCheck, occurrences.get(docToCheck) + occurrence);
    // String pos = ngramForm.getKey().getSequenceAsString(NGram.SequenceType.tag, '_');
    // updateFeatureVals(this.getClass().getName() + "_" + pos, 1.0d, docToCheck, HashSet.class);
    int ngramSize = ngramForm.getKey().size();
    StringBuffer sb = new StringBuffer();
    for (int position = 0; position < ngramSize; ++position) {
      CoreLabel cl = ngramForm.getKey().get(position);
      String type = ngramSize == 1 ? "S" : (position == 0 ? "B" : (position < ngramSize - 1 ? "I" : "E"));
      // String tag = cl.get(StopWordAnnotation.class) ? "MISC" : (SzTECoreNLP.lang == Language.HU ? cl.tag()
      // : cl.tag().substring(0,
      String tag = cl.get(StopWordAnnotation.class) ? "MISC" : cl.tag().substring(0, Math.min(2, cl.tag().length()));
      if (employBIESmarkup) {
        updateFeatureVals(this.getClass().getName() + "_" + type + "_" + tag, occurrence, docToCheck);
      } else {
        sb.append("_" + tag);
      }
    }
    if (!employBIESmarkup) {
      updateFeatureVals(this.getClass().getName() + sb.toString(), occurrence, docToCheck);
    }
  }

  protected double aggregation(List<Collection<Number>> docVals, String phrase, boolean train, List<int[]> length) {
    double[] perDocFeatureVals = new double[docVals.size()];
    for (int doc = 0; doc < docVals.size(); ++doc) {
      List<Number> docNums = (ArrayList<Number>) docVals.get(doc);
      if (docNums.size() > 0 && occurrences.get(doc) != 0) {
        perDocFeatureVals[doc] = docNums.get(docNums.size() - 1).doubleValue() / occurrences.get(doc);
      }
    }
    return NLPUtils.mean(perDocFeatureVals);
  }
}
