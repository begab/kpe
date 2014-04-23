package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

/**
 * This feature assigns the character 2/3-gram suffixes for each token of the occurrences of the NGrams belonging to a normalized form.
 */
public class SuffixFeature extends Feature {

  private static final long serialVersionUID = 4955319571139381492L;

  public SuffixFeature() {
    scale = Scale.BINARY;
    dummyValue = -1;
    canBeRepresentedAsSequential = true;
    collectionToStoreDocVals = HashSet.class;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    int ngramLength = ngramForm.getKey().size();
    StringBuffer[] concatenatedSuffices = new StringBuffer[2];

    for (int tokenNumber = 0; tokenNumber < ngramLength; ++tokenNumber) {
      CoreLabel cl = ngramForm.getKey().get(tokenNumber);
      String word = cl.word().toLowerCase();
      int lemmaLenght = word.length();
      for (int i = 0; !employBIESmarkup && i < concatenatedSuffices.length; ++i) {
        // if this is the first token we need to instantiate array elements
        if (tokenNumber == 0) {
          concatenatedSuffices[i] = new StringBuffer();
        }
        concatenatedSuffices[i].append("_" + word.charAt(lemmaLenght - 1));
      }
      String type = ngramLength == 1 ? "S" : (tokenNumber == 0 ? "B" : (tokenNumber < ngramLength - 1 ? "I" : "E"));

      StringBuffer sb = new StringBuffer();
      sb.append(word.charAt(lemmaLenght - 1));
      for (int i = 2; i <= lemmaLenght && i < concatenatedSuffices.length + 2; ++i) {
        char charToAdd = word.charAt(lemmaLenght - i);
        if (!Character.isLetter(charToAdd))
          break;
        if (employBIESmarkup) {
          sb.append(charToAdd);
          updateFeatureVals(this.getClass().getName() + "_" + type + "_" + sb, 1.0d, docToCheck);
        } else {
          for (int s = 0; s < concatenatedSuffices.length; ++s) {
            if (i - 2 <= s) {
              concatenatedSuffices[s].append(charToAdd);
            }
          }
        }
      }
    }
    if (!employBIESmarkup) {
      for (StringBuffer suffix : concatenatedSuffices) {
        updateFeatureVals(this.getClass().getName() + suffix.toString(), 1.0d, docToCheck);
      }
    }
  }
}
