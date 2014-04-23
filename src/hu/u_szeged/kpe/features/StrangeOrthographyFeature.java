package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.aspirants.NGram;
import hu.u_szeged.kpe.aspirants.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

public class StrangeOrthographyFeature extends Feature {

  private static final long serialVersionUID = 2374136292284809751L;
  private static final Pattern charRunPattern = Pattern.compile("(?i)([a-z])\\1{2,}");

  public StrangeOrthographyFeature() {
    scale = Scale.BINARY;
    dummyValue = -1;
    canBeRepresentedAsSequential = true;
    collectionToStoreDocVals = HashSet.class;
  }

  @Override
  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    int ngramSize = ngramForm.getKey().size();
    StringBuffer concatenatedCharrunPattern = new StringBuffer();
    StringBuffer concatenatedStrangeCapitalizationPattern = new StringBuffer();
    StringBuffer concatenatedNePattern = new StringBuffer();
    for (int position = 0; position < ngramSize; ++position) {
      CoreLabel cl = ngramForm.getKey().get(position);
      String originalToken = cl.word();
      boolean hasUpperCase = false;
      Set<Integer> charRuns = new HashSet<Integer>();
      Matcher match = charRunPattern.matcher(originalToken);
      while (match.find()) {
        charRuns.add(match.end() - match.start());
      }

      for (int i = 1; i < originalToken.length(); ++i) {
        if (Character.isUpperCase(originalToken.charAt(i))) {
          hasUpperCase = true;
          break;
        }
      }
      if (employBIESmarkup) {
        String type = ngramSize == 1 ? "S" : (position == 0 ? "B" : (position < ngramSize - 1 ? "I" : "E"));
        updateFeatureVals(type + "_STRANGE_CAPITALIZATION", hasUpperCase ? 1.0d : 0.0d, docToCheck);
        updateFeatureVals(type + "_NE_" + cl.getString(NamedEntityTagAnnotation.class), 1.0d, docToCheck);
        for (Integer charRun : charRuns) {
          if (charRun > 3)
            updateFeatureVals(type + "_CHARRUN_" + charRun, 1.0d, docToCheck);
        }
      } else {
        concatenatedStrangeCapitalizationPattern.append(hasUpperCase ? "1" : "0");
        concatenatedNePattern.append("_" + cl.getString(NamedEntityTagAnnotation.class));
        for (Integer charRun : charRuns) {
          concatenatedCharrunPattern.append("_");
          if (charRun > 3)
            concatenatedCharrunPattern.append(Integer.toString(charRun) + "|");
        }
      }
    }
    if (!employBIESmarkup) {
      updateFeatureVals("CHARRUN_PATTERN" + concatenatedCharrunPattern, 1.0d, docToCheck);
      updateFeatureVals("NE_PATTERN" + concatenatedNePattern, 1.0d, docToCheck);
      updateFeatureVals("STRANGE_CAPITALIZATION_PATTERN_" + concatenatedStrangeCapitalizationPattern, 1.0d, docToCheck);
    }
  }
}
