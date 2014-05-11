package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGram.SequenceType;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.main.KPEFilter;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.utils.NLPUtils;
import hu.u_szeged.utils.WikiQuery;
import hu.u_szeged.utils.WikiQuery.QueryType;

import java.util.AbstractSequentialList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.NormalizerAnnotator.NormalizerAnnotation;
import edu.stanford.nlp.pipeline.StopWordAnnotator.StopWordAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * This class sets the feature which indicates whether a Wikipedia article could be assigned to an NGram
 */
public class WikiFeature extends Feature {
  private static final long serialVersionUID = 1L;
  /** */
  private static Map<String, Set<String>> categoryCache;

  // TODO Q: Should the usage of categoryCache be limited somehow (e.g. by constraining it not to become extremely big) when there are lots of
  // documents?? A: Probably, we shall return to this question after the first OutOfMemoryException happened.

  public WikiFeature() {
    scale = Scale.BINARY;
    collectionToStoreDocVals = HashSet.class;
  }

  public void setFeatureField(KPEFilter kf) {
    if (categoryCache == null) {
      categoryCache = new HashMap<>();
    }
  }

  /**
   * This pattern is used to remove prefixes of category links and optional suffix parts in parenthesis
   */
  private static final Pattern p = Pattern.compile("(?i)(^(category:|portal:)|\\s\\([^()]+\\))");

  @SuppressWarnings("unchecked")
  private Set<String> getNormalizedWikiCategories(String articleName) {
    Set<String> normalizedCategories = categoryCache.get(articleName.toLowerCase());
    if (normalizedCategories == null) {
      normalizedCategories = new HashSet<>();

      List<Object> categories = ((List<Object>) WikiQuery.performQuery(articleName.toLowerCase(), QueryType.CATEGORY));
      // += 2 is used as every 2nd (even) Object is a count, while the odd indices stand for category names
      for (int i = 0; i < categories.size() / 2; i += 2) {
        String category = (String) categories.get(i);
        AbstractSequentialList<String> tokens = new LinkedList<String>();
        Matcher m = p.matcher(category);
        category = m.replaceAll("");
        for (CoreLabel cl : new NGram(category)) {
          if (!cl.word().matches(".*\\d.*") && cl.tag().startsWith("NN") && !cl.get(StopWordAnnotation.class)) {
            tokens.add(cl.get(NormalizerAnnotation.class));
          }
        }
        Collections.sort(tokens);
        String joinedVersion = NLPUtils.join(tokens);
        if (joinedVersion.length() > 0) {
          normalizedCategories.add(joinedVersion);
        }
      }
      categoryCache.put(articleName.toLowerCase(), normalizedCategories);
    }
    return normalizedCategories;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    String wikiForm = ngramForm.getKey().getSequenceAsString(SequenceType.WIKI_FROM).toLowerCase();
    for (String category : getNormalizedWikiCategories(wikiForm)) {
      updateFeatureVals(this.getClass().getName() + "_" + category, 1.0d, docToCheck);
    }
  }
}
