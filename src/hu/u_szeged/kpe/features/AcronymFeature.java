package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.aspirants.NGram;
import hu.u_szeged.kpe.aspirants.NGram.SequenceType;
import hu.u_szeged.kpe.aspirants.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.utils.NLPUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import edu.stanford.nlp.util.CoreMap;

/**
 * Decides whether a given keyphrase aspirant is an extended for of an acronym present in its document.
 */
public class AcronymFeature extends Feature {
  private static final long serialVersionUID = -499383355487365213L;
  private Set<String> abbreviations;

  public AcronymFeature() {
    scale = Scale.BINARY;
    collectionToStoreDocVals = HashSet.class;
  }

  /**
   * Method that checks whether a long term (of more than one token) could be an extended form of of a possible acronym.<br />
   * Initials of the individual tokens of the longer form must be consistent with the abbreviation in the sense that they must not be such characters
   * that are not a successive character of the substring of the abbreviation that is not covered.<br />
   * E.g. for pairs
   * <ul>
   * <li>("UN", "United Nations") the result would be 1.0d</li>
   * <li>("UN", "United Kingdom") 0.0d</li>
   * <li>("UNO", "United Organization") gives the result of 1.0 as well</li>
   * </ul>
   * 
   * @param acronym
   *          abbreviation or acronym to check
   * @param longTerm
   *          possible extension of the shorter form of the other parameter
   * @return double value indicating when returning 1.0 that the longer term could be an extended form of the shorter form in question
   */
  public static double checkForAcronymity(String acronym, String longTerm) {
    acronym = acronym.toLowerCase().replaceAll("\\p{Punct}", "");
    longTerm = NLPUtils.join(longTerm.split("-")).toLowerCase();
    longTerm = longTerm.replaceAll("\\s+and\\s+", " ");
    if (longTerm.split(" ").length < 2 || longTerm.startsWith(acronym))
      return 0.0;
    ArrayList<Integer> tokenBorders = new ArrayList<Integer>();
    Stack<Integer> lastlyInvolved = new Stack<Integer>();
    lastlyInvolved.push(0);
    int whitespace = longTerm.indexOf(" ") + 1;
    tokenBorders.add(0);
    tokenBorders.add(whitespace);
    while ((whitespace = longTerm.indexOf(" ", whitespace)) != -1)
      tokenBorders.add(++whitespace);
    int[] matchingChars = new int[longTerm.length()];
    boolean[] notForInvolving = new boolean[longTerm.length()];
    for (int c = 0; c < matchingChars.length; ++c) {
      if (c > 0)
        matchingChars[c] = matchingChars[c - 1];
      if (tokenBorders.contains(c)) {
        if (longTerm.charAt(c) != acronym.charAt(matchingChars[c])) {
          int wronglyInvolvedIndex;
          if ((wronglyInvolvedIndex = lastlyInvolved.pop()) == 0)
            return 0.0;
          notForInvolving[wronglyInvolvedIndex] = true;
          for (int index = wronglyInvolvedIndex; index <= c; ++index) {
            matchingChars[index]--;
          }
          c = wronglyInvolvedIndex;
        } else {
          matchingChars[c]++;
          lastlyInvolved.push(c);
        }
      } else {
        if (longTerm.charAt(c) == acronym.charAt(matchingChars[c]) && !notForInvolving[c]) {
          matchingChars[c]++;
          lastlyInvolved.push(c);
        }
      }

      if (matchingChars[c] == acronym.length())
        if (c < tokenBorders.get(tokenBorders.size() - 1)) {
          int wronglyInvolvedIndex = lastlyInvolved.pop();
          notForInvolving[wronglyInvolvedIndex] = true;
          for (int index = wronglyInvolvedIndex; index <= c; ++index) {
            matchingChars[index]--;
          }
          c = wronglyInvolvedIndex;
        } else
          return 1.0;
    }
    return 0.0;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {

    if (abbreviations == null) {
      abbreviations = new HashSet<String>();
      for (DocumentData doc : docs)
        abbreviations.addAll(doc.getAcronyms().keySet());
    }

    String original = ngramForm.getKey().getSequenceAsString(SequenceType.original).replaceAll("-?s$", "");
    int matchingAcronyms = 0;

    for (String acronym : abbreviations) {
      if (checkForAcronymity(acronym, original) == 1.0) {
        matchingAcronyms++;
        break;
      }
    }
    updateFeatureVals(matchingAcronyms, docToCheck);
  }

  protected double aggregation(List<Collection<Number>> docVals, String phrase, boolean train, List<int[]> length) {
    abbreviations = null;
    return super.aggregation(docVals, phrase, train, length);
  }
}
