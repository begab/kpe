package hu.u_szeged.nlp.pos;

import java.util.Collection;
import java.util.LinkedHashSet;

public class HyphenicWord {

  public static LinkedHashSet<String> analyseHyphenicCompoundWord(String hyphenicCompoundWord) {
    LinkedHashSet<String> analises = null;
    analises = new LinkedHashSet<String>();

    if (!hyphenicCompoundWord.contains("-")) {
      return analises;
    }

    String firstPart = null;
    String secondPart = null;

    int hyphenPosition = 0;

    hyphenPosition = hyphenicCompoundWord.indexOf('-');
    firstPart = hyphenicCompoundWord.substring(0, hyphenPosition);
    secondPart = hyphenicCompoundWord.substring(hyphenPosition + 1, hyphenicCompoundWord.length());

    // a kötőjel előtti és a kötőjel utáni résznek is van elemzése (pl.:
    // adat-kezelőt)
    if (CompoundWord.isBisectable(firstPart + secondPart)) {
      analises = CompoundWord.getCompatibleAnalises(firstPart, secondPart, true);
    }

    // a kötőjel előtti résznek is van elemzése, a kötőjel utáni rész két részre
    // bontható
    else if (MagyarlancResourceHolder.getRFSA().analyse(firstPart).size() > 0 && CompoundWord.isBisectable(secondPart)) {
      Collection<String> firstPartAnalises = null;
      firstPartAnalises = MagyarlancResourceHolder.getRFSA().analyse(firstPart);

      String firstPartOfSecondSection = null;
      String secondPartOfSecondSection = null;
      LinkedHashSet<String> secondSectionAnalises = null;
      int bisectIndex = 0;
      bisectIndex = CompoundWord.bisectIndex(secondPart);
      firstPartOfSecondSection = secondPart.substring(0, bisectIndex);
      secondPartOfSecondSection = secondPart.substring(bisectIndex, secondPart.length());

      secondSectionAnalises = CompoundWord.getCompatibleAnalises(firstPartOfSecondSection, secondPartOfSecondSection);

      for (String firstAnalyse : firstPartAnalises) {
        for (String secondAnalyse : secondSectionAnalises) {
          if (CompoundWord.isCompatibleAnalyises(KRUtils.getRoot(firstAnalyse), KRUtils.getRoot(secondAnalyse))) {
            if (analises == null) {
              analises = new LinkedHashSet<String>();
            }
            analises.add(KRUtils.getRoot(secondAnalyse).replace("$", "$" + firstPart + "-"));
          }
        }
      }
    }

    else if (CompoundWord.isBisectable(firstPart) && MagyarlancResourceHolder.getRFSA().analyse(secondPart).size() > 0) {
      Collection<String> secondPartAnalises = null;
      secondPartAnalises = MagyarlancResourceHolder.getRFSA().analyse(secondPart);

      String firstSectionOfFirstPart = null;
      String secondSectionOfFirstPart = null;
      LinkedHashSet<String> firstPartAnalises = null;
      int bisectIndex = 0;

      bisectIndex = CompoundWord.bisectIndex(firstPart);
      firstSectionOfFirstPart = firstPart.substring(0, bisectIndex);
      secondSectionOfFirstPart = firstPart.substring(bisectIndex, firstPart.length());

      firstPartAnalises = CompoundWord.getCompatibleAnalises(firstSectionOfFirstPart, secondSectionOfFirstPart);

      for (String firstAnalyse : firstPartAnalises) {
        for (String secondAnalyse : secondPartAnalises) {
          if (CompoundWord.isCompatibleAnalyises(KRUtils.getRoot(firstAnalyse), KRUtils.getRoot(secondAnalyse))) {
            if (analises == null) {
              analises = new LinkedHashSet<String>();
            }
            analises.add(KRUtils.getRoot(secondAnalyse).replace("$", "$" + firstPart + "-"));
          }
        }
      }
    }

    return analises;
  }

  public static void main(String[] args) {

  }
}
