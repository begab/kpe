package hu.u_szeged.nlp.pos;

import hu.u_szeged.nlp.pos.guesser.HyphenicGuesser;
import hu.u_szeged.nlp.pos.guesser.NumberGuesser;

import java.util.HashSet;
import java.util.Set;

public class HunLemMor {
  /**
   * addott szó lehetséges morfológiai elemzéseinek megahatározása
   */

  public static Set<MorAna> getMorphologicalAnalyses(String word) {
    Set<MorAna> morAnas = new HashSet<MorAna>();

    // irasjelek
    if (Util.isPunctation(word)) {
      // a legfontosabb irasjelek lemmaja maga az irasjel, POS kodja szinten
      // maga az irasjel lesz
      // . , ; : ! ? - —
      if (MagyarlancResourceHolder.getPunctations().contains(word)) {
        morAnas.add(new MorAna(word, word));
      }
      // egyeb irasjelek lemmaja maga az irasjel, POS kódja 'K' lesz
      else {
        morAnas.add(new MorAna(word, "K"));
      }
      return morAnas;
    }

    Set<MorAna> potentialResultSet = MagyarlancResourceHolder.getCorpus().get(word);
    // ha benne van a corpus.lex-ben
    if (potentialResultSet != null) {
      return potentialResultSet;
    }

    potentialResultSet = MagyarlancResourceHolder.getCorpus().get(word.toLowerCase());
    // ha benne van a corpus.lex-ben kisbetűvel
    if (potentialResultSet != null) {
      return potentialResultSet;
    }

    // szám
    morAnas = NumberGuesser.guess(word);

    if (morAnas.size() > 0) {
      return morAnas;
    }

    // római szám
    morAnas.addAll(NumberGuesser.guessRomanNumber(word));

    // rfsa
    for (String kr : MagyarlancResourceHolder.getRFSA().analyse(word)) {
      // String copyOfKr = kr;
      // if (standardized) {
      // // igekotok levalasztasa
      // kr = kr.substring(kr.indexOf("$"));
      //
      // if (kr.contains("(") && kr.indexOf("(") < kr.indexOf("/"))
      // kr = kr.substring(kr.indexOf("$") + 1, kr.indexOf("("));
      // else
      // kr = kr.substring(kr.indexOf("$") + 1, kr.indexOf("/"));
      // }
      // System.err.println(copyOfKr + "\t" + kr);
      morAnas.addAll(MagyarlancResourceHolder.getKRToMSD().getMSD(kr));
    }

    // (kötöjeles)összetett szó
    if (morAnas.size() == 0) {
      // kötöjeles
      if (word.contains("-") && word.indexOf("-") > 1) {
        for (String morphCode : HyphenicWord.analyseHyphenicCompoundWord(word)) {
          morAnas.addAll(MagyarlancResourceHolder.getKRToMSD().getMSD(morphCode));
        }
      } else {
        // összetett szó
        for (String morphCode : CompoundWord.analyseCompoundWord(word.toLowerCase())) {
          morAnas.addAll(MagyarlancResourceHolder.getKRToMSD().getMSD(morphCode));
        }
      }
    }

    // guess (Bush-nak, Bush-kormányhoz)
    if (morAnas.size() == 0) {
      int index = word.lastIndexOf("-") > 1 ? word.lastIndexOf("-") : 0;

      if (index > 0) {
        String root = null;
        String suffix = null;

        root = word.substring(0, index);
        suffix = word.substring(index + 1);
        morAnas.addAll(HyphenicGuesser.guess(root, suffix));
      }
    }

    // népes szavak
    if (morAnas.size() == 0) {

      if (MagyarlancResourceHolder.getCorrDic().containsKey(word) && !word.equals(MagyarlancResourceHolder.getCorrDic().get(word))) {
        morAnas.addAll(getMorphologicalAnalyses(MagyarlancResourceHolder.getCorrDic().get(word)));
      }

      else if (MagyarlancResourceHolder.getCorrDic().containsKey(word.toLowerCase())
          && !word.equals(MagyarlancResourceHolder.getCorrDic().get(word.toLowerCase()))) {
        morAnas.addAll(getMorphologicalAnalyses(MagyarlancResourceHolder.getCorrDic().get(word.toLowerCase())));
      }

      // System.out.println(word);

      return morAnas;
    } else {
      return morAnas;
    }

  }

}
