package hu.u_szeged.nlp.pos.converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MSDReducer {

  private Map<String, String> cache = null;

  private final Pattern NOUN_PATTERN_1 = Pattern.compile("N.-..---s3");
  private final Pattern NOUN_PATTERN_2 = Pattern.compile("N.-..---..s");

  private final Pattern ADJECTIVE_PATTERN_1 = Pattern.compile("A..-..-.--s3");
  private final Pattern ADJECTIVE_PATTERN_2 = Pattern.compile("A..-..-.--..s");

  private final Pattern NUMERAL_PATTERN_1 = Pattern.compile("M.-...-.--s3");
  private final Pattern NUMERAL_PATTERN_2 = Pattern.compile("M.-...-.--..s");

  private final Pattern OPEN_PATTERN_1 = Pattern.compile("O..-..---s3");
  private final Pattern OPEN_PATTERN_2 = Pattern.compile("O..-..---..s");

  private final Pattern VERB_PATTERN_1 = Pattern.compile("V[^a]cp[12]p---y");
  private final Pattern VERB_PATTERN_2 = Pattern.compile("V[^a]ip1s---y");
  private final Pattern VERB_PATTERN_3 = Pattern.compile("V[^a]cp3p---y");
  private final Pattern VERB_PATTERN_4 = Pattern.compile("V[^a]is1[sp]---y");

  public MSDReducer() {
    this.setCache(new HashMap<String, String>());
  }

  /**
   * reduce noun
   * 
   * @param msd
   */
  private String reduceN(String msd) {
    StringBuffer result = null;
    result = new StringBuffer("N");

    // dative/genitive
    // superessive/essive

    if (msd.length() > 4 && (msd.charAt(4) == 'd' || msd.charAt(4) == 'g' || msd.charAt(4) == 'p')) {
      result.append(msd.charAt(4));
    }

    // N.-..---s3
    if (NOUN_PATTERN_1.matcher(msd).find()) {
      result.append("s");
    }

    // N.-..---..s
    if (NOUN_PATTERN_2.matcher(msd).find()) {
      result.append("z");
    }

    return result.toString();
  }

  private String reduceO(String msd) {
    StringBuffer result = null;
    result = new StringBuffer("O");

    // dative/genitive
    // superessive/essive
    if (msd.length() > 5 && (msd.charAt(5) == 'd' || msd.charAt(5) == 'g' || msd.charAt(5) == 'p')) {
      result.append(msd.charAt(5));
    }
    // O..-..---s3
    if (OPEN_PATTERN_1.matcher(msd).find()) {
      result.append("s");
    }

    // O..-..---..s
    if (OPEN_PATTERN_2.matcher(msd).find()) {
      result.append("z");
    }

    return result.toString();
  }

  /**
   * reduce verb
   */
  private String reduceV(String msd) {
    String result = null;

    // Va
    if (msd.startsWith("Va")) {
      result = "Va";
    }

    // festett�l
    // Vsis[123][sp]---[yn]
    // else if (msd.startsWith("Vsis") && !msd.equals("Vsis3s---n")) {
    // if (msd.endsWith("---y"))
    // // 1
    // result = "Vsy";
    // else
    // result = "Vs";
    // }

    // olvasn�nk
    // V[^a]cp1p---y
    else if (VERB_PATTERN_1.matcher(msd).find()) {
      result = "Vcp";
    }

    // eszek eszem
    // V[^a]ip1s---y
    else if (VERB_PATTERN_2.matcher(msd).find()) {
      result = "Vip";
    }

    // festetn�k
    // V[^a]cp3p---y
    // private final Pattern VERB_PATTERN_3 = Pattern.compile("V[^a]cp3p---y");
    else if (VERB_PATTERN_3.matcher(msd).find()) {
      // if (msd.charAt(1) == 's')
      // result = "Vs3p";
      // else
      result = "V3p";
    }

    // festettem

    // V s is[123][sp]---[yn]

    // V[^a]is 1 [sp]---y

    else if (VERB_PATTERN_4.matcher(msd).find()) {
      // if (msd.charAt(1) == 's')
      // //2
      // result = "Vs1y";
      // else
      result = "Vy";
    }

    // V-m felszlito mod
    else if (msd.length() > 2 && msd.charAt(2) == 'm') {
      result = "Vm";
    }

    // V--p jelen ido egybeeshet multtal pl.: �rt
    else if (msd.length() > 3 && msd.charAt(3) == 'p') {
      result = "Vp";
    }

    else {
      result = "V";
    }

    return result;
  }

  /**
   * reduce adjective
   * 
   * @param msd
   * @return
   */
  private String reduceA(String msd) {
    StringBuffer result = null;
    result = new StringBuffer("A");

    // dative/genitive
    // superessive/essive
    if (msd.length() > 5 && (msd.charAt(5) == 'd' || msd.charAt(5) == 'g' || msd.charAt(5) == 'p')) {
      result.append(msd.charAt(5));
    }

    // A..-..-.--s3
    if (ADJECTIVE_PATTERN_1.matcher(msd).find()) {
      result.append("s");
    }

    // A..-..-.--..s
    if (ADJECTIVE_PATTERN_2.matcher(msd).find()) {
      result.append("z");
    }
    return result.toString();
  }

  /**
   * reduce pronoun
   * 
   * @param msd
   * @return
   */
  private String reduceP(String msd) {
    StringBuffer result = null;
    result = new StringBuffer("P");

    // Pq Pr Pp
    if (msd.length() > 1 && (msd.charAt(1) == 'q' || msd.charAt(1) == 'r' || msd.charAt(1) == 'p')) {
      if (msd.charAt(1) == 'p')
        result.append('e');
      else
        result.append(msd.charAt(1));
    }

    // dative/genitive
    // superessive/essive
    if (msd.length() > 5 && (msd.charAt(5) == 'd' || msd.charAt(5) == 'g' || msd.charAt(5) == 'p')) {
      result.append(msd.charAt(5));
    }

    return result.toString();
  }

  /**
   * reduce adverb
   * 
   * @param msd
   * @return
   */
  private String reduceR(String msd) {
    StringBuffer result = null;
    result = new StringBuffer("R");

    // Rq Rr Rp Rl
    if (msd.length() > 1 && (msd.charAt(1) == 'q' || msd.charAt(1) == 'r' || msd.charAt(1) == 'p' || msd.charAt(1) == 'l')) {
      result.append(msd.charAt(1));
    }

    return result.toString();
  }

  private String reduceM(String msd) {
    StringBuffer result = null;
    result = new StringBuffer("M");

    // fractal
    if (msd.length() > 1 && msd.charAt(1) == 'f') {
      result.append(msd.charAt(1));
    }

    // dative/genitive
    // superessive/essive
    if (msd.length() > 4 && (msd.charAt(4) == 'd' || msd.charAt(4) == 'g' || msd.charAt(4) == 'p')) {
      result.append(msd.charAt(4));
    }

    // M.-...-.--s3
    if (NUMERAL_PATTERN_1.matcher(msd).find()) {
      result.append("s");
    }

    // M.-...-.--..s
    if (NUMERAL_PATTERN_2.matcher(msd).find()) {
      result.append("z");
    }
    return result.toString();
  }

  public String reduce(String msd) {

    if (this.getCache().containsKey(msd)) {
      return this.getCache().get(msd);
    }

    String reduced = null;

    if (msd.length() == 1)
      return msd;

    switch (msd.charAt(0)) {

    case 'N':
      reduced = reduceN(msd);
      break;

    case 'V':
      reduced = reduceV(msd);
      break;

    case 'A':
      reduced = reduceA(msd);
      break;

    case 'P':
      reduced = reduceP(msd);
      break;

    case 'R':
      reduced = reduceR(msd);
      break;

    case 'M':
      reduced = reduceM(msd);
      break;

    case 'O':
      reduced = reduceO(msd);
      break;

    case 'C':
      reduced = msd;
      break;

    // T, S, I, X, Y, Z
    default:
      reduced = String.valueOf(msd.charAt(0));
    }

    this.getCache().put(msd, reduced);

    return reduced;
  }

  public void setCache(Map<String, String> cache) {
    this.cache = cache;
  }

  public Map<String, String> getCache() {
    return cache;
  }

  public void printCache() {
    for (Map.Entry<String, String> entry : this.getCache().entrySet()) {
      System.out.println(entry.getKey() + "\t" + entry.getValue());
    }
  }

  public static void main(String args[]) throws IOException {

    MSDReducer reducer = null;
    reducer = new MSDReducer();

    System.out.println(reducer.reduce("Pp3-sp"));
    System.out.println(reducer.reduce("Ccsw"));
    System.out.println(reducer.reduce("Pq3-ses--------3"));
    System.out.println(reducer.reduce("Px3-sn----------s"));
    // reducer.printCache();

  }
}
