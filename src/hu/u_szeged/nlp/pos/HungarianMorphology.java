package hu.u_szeged.nlp.pos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.CoreLabel;

public class HungarianMorphology {

  public static String[] getPossibleTags(String word, Set<String> possibleTags) {
    Set<MorAna> morAnas = null;
    Set<String> res = null;
    String reduced = null;

    morAnas = HunLemMor.getMorphologicalAnalyses(word);
    res = new HashSet<String>();

    for (MorAna morAna : morAnas) {
      reduced = MagyarlancResourceHolder.getMSDReducer().reduce(morAna.getMsd());
      if (possibleTags.contains(reduced)) {
        res.add(reduced);
      }
    }

    if (res.size() == 0) {
      res.add("X");
    }

    return res.toArray(new String[res.size()]);
  }

  public static List<CoreLabel> recoverTags(List<CoreLabel> sentence) {
    Set<MorAna> set = null;
    for (CoreLabel tw : sentence) {
      set = HunLemMor.getMorphologicalAnalyses(tw.word());
      int max = -1;
      MorAna argmax = null;

      for (MorAna morAna : set) {

        int freq = MagyarlancResourceHolder.getFrequencies().containsKey(morAna.getMsd()) ? MagyarlancResourceHolder.getFrequencies().get(morAna.getMsd()) : 0;

        if (!morAna.getMsd().equals(null)) {
          if (MagyarlancResourceHolder.getMSDReducer().reduce(morAna.getMsd()).equals(tw.tag()) && (max < freq)) {
            argmax = morAna;
            max = freq;
          }
        }
      }

      if (argmax != null) {
        tw.setLemma(argmax.getLemma());
        tw.setTag(argmax.getMsd());
      } else {
        tw.setLemma(tw.word());
      }
    }
    return sentence;
  }
}
