package hu.u_szeged.utils;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreLabel;

public class Stopword implements Serializable {

  public static final long serialVersionUID = 1L;
  public static Set<String> stopwords = null;

  public Stopword() {
    if (stopwords != null) {
      return;
    }

    stopwords = new HashSet<String>();
    NLPUtils.readDocToCollection(System.getProperty("user.dir") + "/resources/stopwords/stopwords_en.txt", stopwords, Charset.forName("UTF-8"));
  }

  /**
   * Returns true if the given string is a stop word.
   */
  public boolean isStopword(String str) {
    return stopwords.contains(str.toLowerCase());
  }

  public boolean isStopword(CoreLabel ew) {
    String lemma = ew.get(LemmaAnnotation.class);
    if (lemma == null)
      System.err.println("No lemma for token " + ew);
    else
      lemma = lemma.toLowerCase();
    return isStopword(ew.word().toLowerCase()) || (lemma != null && isStopword(lemma));
  }
}