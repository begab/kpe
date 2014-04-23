package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.pipeline.MweDictAnnotator.MWEAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * This class makes use of a list, crawled from Wikipedia containing MWEs and identifies NGrams that can be mapped to any element of it.
 */
public class MweFeature extends Feature {

  /**
   * 
   */
  private static final long serialVersionUID = 3440662396687971296L;

  public MweFeature() {
    scale = Scale.BINARY;
    collectionToStoreDocVals = HashSet.class;
  }

  @Override
  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    boolean isMWE = false, isCompoundMwe = false, containsMwe = false;
    List<Entry<Character, Integer>> types = new ArrayList<Entry<Character, Integer>>();
    List<String> markUps = new ArrayList<String>();
    List<int[]> spans = new ArrayList<int[]>();
    for (int i = 0; i < ngramForm.getKey().size(); ++i) {
      String mweAnnotation = ngramForm.getKey().get(i).get(MWEAnnotation.class);
      String[] mweAnnotationParts = mweAnnotation.split("@");
      int removed = 0;
      for (int p = 0; p < mweAnnotationParts.length; ++p) {
        char type = mweAnnotationParts[p].charAt(0);
        String markUp = mweAnnotationParts[p].replaceAll("[BIE]-", "");
        if (i == 0 && type != 'E' && type != 'O') {
          types.add(new SimpleEntry<Character, Integer>(type, i));
          markUps.add(markUp);
        } else if (i > 0) {
          if (type == 'E') {
            Entry<Character, Integer> t = types.remove(p - removed);
            markUps.remove(p - removed);
            removed++;

            boolean acceptablePOSsequence = true;
            for (int token = t.getValue(); token <= i; ++token) {
              String tag = ngramForm.getKey().get(token).get(PartOfSpeechAnnotation.class);
              if ((token == t.getValue() || token == i) && !tag.matches("(?i)nn.{0,2}|jj.?")) {
                acceptablePOSsequence = false;
                break;
              } else if (!tag.matches("(?i)nn.{0,2}|jj.?|in|pos")) {
                acceptablePOSsequence = false;
                break;
              }
            }
            if (acceptablePOSsequence) {
              containsMwe = containsMwe || t.getKey() == 'B';
              boolean fullScope = t.getValue() == 0 && i == ngramForm.getKey().size() - 1;
              if (t.getKey() == 'B' && !fullScope)
                spans.add(new int[] { t.getValue(), i });
              isMWE = isMWE || (t.getKey() == 'B' && fullScope);
            }
          } else if (type == 'B') {
            types.add(new SimpleEntry<Character, Integer>(type, i));
            markUps.add(markUp);
          }
        }
      }
    }
    if (spans.size() > 1) {
      for (int[] span : spans) {
        for (int[] span2 : spans) {
          if (span[1] >= span2[0] && span[0] == 0 && span2[1] == ngramForm.getKey().size() - 1) {
            isCompoundMwe = true;
          }
        }
      }
    }
    updateFeatureVals(this.getClass().getName() + "_NGram", isMWE ? 1.0d : 0.0d, docToCheck);
    updateFeatureVals(this.getClass().getName() + "_compoundNGram", isCompoundMwe ? 1.0d : 0.0d, docToCheck);
    updateFeatureVals(this.getClass().getName() + "_Containment", containsMwe ? 1.0d : 0.0d, docToCheck);
  }
}
