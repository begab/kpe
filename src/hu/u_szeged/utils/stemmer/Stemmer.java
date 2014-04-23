package hu.u_szeged.utils.stemmer;

import java.io.Serializable;

public abstract class Stemmer implements Serializable {

  private static final long serialVersionUID = -2966966100130854943L;

  /**
   * Iterated stemming of the given word.
   */
  public abstract String stem(String str);

  /**
   * Stems everything in the given string.
   */
  public String stemString(String str) {
    str = str.toLowerCase();
    StringBuffer result = new StringBuffer();
    int start = -1;
    for (int j = 0; j < str.length(); j++) {
      char c = str.charAt(j);
      if (Character.isLetterOrDigit(c)) {
        if (start == -1) {
          start = j;
        }
      } else if (c == '\'') {
        if (start == -1) {
          result.append(c);
        }
      } else {
        if (start != -1) {
          result.append(stem(str.substring(start, j)));
          start = -1;
        }
        result.append(c);
      }
    }
    if (start != -1) {
      result.append(stem(str.substring(start, str.length())));
    }
    return result.toString();
  }
}