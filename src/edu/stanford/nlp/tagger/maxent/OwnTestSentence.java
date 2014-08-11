package edu.stanford.nlp.tagger.maxent;

import hu.u_szeged.nlp.pos.HungarianMorphology;

import java.util.Set;

import edu.stanford.nlp.pipeline.SzTECoreNLP;

public class OwnTestSentence extends TestSentence {

  public OwnTestSentence(MaxentTagger maxentTagger) {
    super(maxentTagger);
  }

  protected String[] stringTagsAt(int pos) {
    String[] arr1;
    if ((pos < leftWindow()) || (pos >= size + leftWindow())) {
      arr1 = new String[1];
      arr1[0] = naTag;
      return arr1;
    }

    if (originalTags != null && originalTags.get(pos - leftWindow()) != null) {
      arr1 = new String[1];
      arr1[0] = originalTags.get(pos - leftWindow());
      return arr1;
    }

    String word = sent.get(pos - leftWindow());
    if (maxentTagger.dict.isUnknown(word)) {
      if (SzTECoreNLP.lang.equals("en")) {
        Set<String> open = maxentTagger.tags.getOpenTags();
        arr1 = open.toArray(new String[open.size()]);
      } else {
        arr1 = HungarianMorphology.getPossibleTags(word, maxentTagger.tags.getOpenTags());
      }
    } else {
      arr1 = maxentTagger.dict.getTags(word);
    }
    if (SzTECoreNLP.lang.equals("en")) {
      arr1 = maxentTagger.tags.deterministicallyExpandTags(arr1);
    }
    return arr1;
  }
}
