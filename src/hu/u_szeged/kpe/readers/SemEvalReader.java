package hu.u_szeged.kpe.readers;

import hu.u_szeged.utils.NLPUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class SemEvalReader extends ScientificReader {

  /** Files having the value of DEFAULT_EXTENSION as their suffices will be processed only. */
  private static final String DEFAULT_EXTENSION = ".txt.final";
  private Map<String, String> readerKeyphrases = null;
  private Map<String, String> authorKeyphrases = null;
  /** This set keeps track of the directories, the keyphrases of which were tried to be read into memory */
  private Set<String> registeredDirs;

  protected void setDetails() {
    fileType = DEFAULT_EXTENSION;
    registeredDirs = new HashSet<>();
  }

  public List<DocumentData> getContent(String dir, String file) {
    if (registeredDirs.add(dir)) {
      readerKeyphrases = etalonKeyphrases("reader", dir);
      authorKeyphrases = etalonKeyphrases("author", dir);
      if (goldAnnotation) {
        // readerKeyphrases.putAll(authorKeyphrases);
        for (Entry<String, String> readers : readerKeyphrases.entrySet()) {
          String authors = authorKeyphrases.get(readers.getKey());
          if (authors.length() > 0) {
            readers.setValue(readers.getValue() + "\r\n" + authors);
          }
        }
      }
    }
    String fileId = file.replaceAll(".*[\\\\/](([CHIJ]-)?\\d+)" + fileType, "$1");
    List<DocumentData> toReturn = new ArrayList<DocumentData>(1);
    toReturn.add(new ContestDocumentData(readerKeyphrases.get(fileId), authorKeyphrases.get(fileId), file, this.getClass()));
    return toReturn;
  }

  public String getText(String file, int numberWithinFile) {
    List<String> lines = new LinkedList<>();
    NLPUtils.readDocToCollection(file, lines);
    List<String> reducedLines = new ArrayList<String>(lines.size());
    List<Integer> lengths = new ArrayList<Integer>(lines.size());
    List<Integer> tokens = new ArrayList<Integer>(lines.size());
    List<Number> charPerToken = new ArrayList<Number>(lines.size());

    int sum = 0, tokenSum = 0, i = 0, lineNum = 0;
    for (; i < lines.size(); ++i) {
      if (lines.get(i).trim().length() > 0) {
        reducedLines.add(lines.get(i++));
        break;
      }
    }
    // Map<String, Integer> multipleLines = new HashMap<String, Integer>();
    // Map<String, List<String>> differentNumericLineForms = new HashMap<String, List<String>>();
    try {
      while (!lines.get(i++).matches("(?i)\\s*abstract\\s*")) {
        continue;
      }
    } catch (IndexOutOfBoundsException e) {
      i = 1;
      System.err.println("No abstract for file " + file);
    }
    boolean referencesSeen = false;
    for (; i < lines.size(); ++i) {
      String line = lines.get(i);
      if (!referencesSeen && line.matches("(?i)(\\d+\\.?\\s+)?references?") && i + 1 < lines.size() && lines.get(i + 1).matches("\\[1\\].*"))
        referencesSeen = true;
      if (!isUnnecessaryLine(line)) {
        lineNum++;
        reducedLines.add(line);
        int tokenNumber = line.split(" +").length;
        if (!referencesSeen) {
          // String modifiedLine = line.replaceAll("\\s*\\d+\\s*", "_NUM_");
          // if (!line.endsWith(".") && !line.toLowerCase().startsWith("theorem")){
          // if (!modifiedLine.equals(line)){
          // List<String> differentFormsOfLine = differentNumericLineForms.get(modifiedLine);
          // differentFormsOfLine = differentFormsOfLine == null ? new ArrayList<String>() :
          // differentFormsOfLine;
          // differentFormsOfLine.add(line);
          // differentNumericLineForms.put(modifiedLine, differentFormsOfLine);
          // }
          // Integer val = multipleLines.get(modifiedLine);
          // multipleLines.put(modifiedLine, val == null ? 1 : ++val);
          // }
          sum += line.length();
          lengths.add(line.length());
          tokenSum += tokenNumber;
          tokens.add(tokenNumber);
          charPerToken.add((double) (line.length() - tokenNumber) / tokenNumber);
        }
      }
    }

    Set<String> toSkip = new HashSet<String>();
    // for (Entry<String, Integer> multipleLine : multipleLines.entrySet()){
    // int timesOfOccurrence = multipleLine.getValue();
    // if (timesOfOccurrence > 4){
    // List<String> formsOfLine = differentNumericLineForms.get(multipleLine.getKey());
    // System.err.println(timesOfOccurrence + "\t" + multipleLine);
    // while (--timesOfOccurrence >= 0){
    // String actualForm = formsOfLine == null ? multipleLine.getKey() :
    // formsOfLine.get(timesOfOccurrence);
    // toSkip.add(actualForm);
    // sum -= actualForm.length();
    // int tokenNumber = actualForm.trim().split(" +").length;
    // tokenSum -= tokenNumber;
    // lengths.remove((Integer) actualForm.length());
    // tokens.remove((Integer) tokenNumber);
    // charPerToken.remove((double) (actualForm.length() - tokenNumber) / tokenNumber);
    // }
    // }
    // }
    double avgCharPerToken = NLPUtils.mean(charPerToken), devCharPerToken = 0.0;
    double devChars = 1.0, avgChars = (double) sum / lengths.size();
    double devTokens = 0.0, avgTokens = (double) tokenSum / tokens.size();

    for (int index = 0; index < lengths.size(); ++index) {
      devChars += Math.pow(avgChars - lengths.get(index), 2);
      devTokens += Math.pow(avgTokens - tokens.get(index), 2);
      devCharPerToken += Math.pow(avgCharPerToken - charPerToken.get(index).doubleValue(), 2);
    }

    String[] article = processText(reducedLines, avgChars, Math.sqrt(devChars / lineNum), avgTokens, Math.sqrt(devTokens / tokens.size()), avgCharPerToken,
        Math.sqrt(devCharPerToken / charPerToken.size()), toSkip);

    // List<String> sentences = new ArrayList<String>();
    // sentences.add(article[0]);
    // String allKeyphrases = "";
    /*
     * Set<String> phrases = new TreeSet<String>(); for (Entry<String, String> kphs : authorKeyphrases.entrySet()){ if (kphs.getKey().charAt(0) !=
     * fileId.charAt(0)) continue; String[] kphArray = kphs.getValue().split("\r*\n"); for (String kph : kphArray)
     * phrases.add(kph.toLowerCase().trim()); } for (String phrase : phrases)
     */
    // for (String phrase : authorKeyphrases.values())
    // if (phrase.replaceAll("\\s+", " ").length() != 1)
    // allKeyphrases += phrase + "\r\n";
    return article[0];
  }

  protected String[] processText(List<String> reducedLines, double avgCh, double devCh, double avgToken, double devToken, double avgTokenChar,
      double devTokenChar, Set<String> toSkip) {
    boolean sectionTitle = false, newLine = true, referencesSeen = false, anySectionSeen = false;
    StringBuffer sb = new StringBuffer();
    while (sb.length() == 0) {
      // TODO turn on mostPossibleTitle function, when felt necessary
      sb = new StringBuffer(/** mostPossibleTitle */
      (reducedLines.get(0)));
    }

    StringBuffer references = new StringBuffer();
    StringBuffer titleAndAbstract = new StringBuffer(sb.toString() + "\r\n");
    for (int i = 1; i < reducedLines.size(); ++i) {
      String line = reducedLines.get(i);
      if (toSkip.contains(line) || line.length() == 0)
        continue;
      // int tokensInLine = line.split("\\s+").length;
      // double avgTokenLength = (double) (line.length() - tokensInLine) / tokensInLine;
      if (referencesSeen) {
        sb.append((line.matches("\\[?\\d+\\]?.*") ? "\r\n" : " ") + line);
        references.append((line.matches("\\[?\\d+\\]?.*") ? "\r\n" : " ") + line);
      } else if (line.trim().matches("[1-9]\\d?(\\.\\d+\\.?)*\\.?\\s+[A-Z][\\s:\\-A-Za-z\"]+")
          || (line.matches("\\s*([1-9]\\d?\\.?\\s+)?R(eferences?|EFERENCES?)"))) {
        // && reducedLines.get(i + 1).matches("\\s*\\[1\\].*")
        sb.append("\r\n" + line);
        sectionTitle = true;
        anySectionSeen = true;
        newLine = true;
        if (line.matches("(?i)\\s*(\\d+\\.?\\s+)?references?"))
          referencesSeen = true;
      } else if (line.matches(".*\\S[.?:]$")) {
        sb.append(line + "\r\n");
        // } else if (avgTokenLength > avgTokenChar - devTokenChar && tokensInLine > avgToken - devToken &&
        // line.length() > avgCh - devCh) {
      } else if (sectionTitle && Character.isUpperCase(line.charAt(0))) {
        sb.append(" " + line);
        sectionTitle = false;
      } else {
        sb.append((newLine ? "\r\n" : "") + line + " ");
        sectionTitle = false;
        newLine = false;
      }
      if (!anySectionSeen)
        titleAndAbstract.append(line + " ");
    }
    return new String[] { sb.toString(), references.toString(), titleAndAbstract.toString() };
  }

  protected boolean isUnnecessaryLine(String line) {
    String unnecessityIndicators = "(){}[]";
    Map<Character, List<Integer>> indicatorPositions = new HashMap<Character, List<Integer>>();
    // initialize
    for (int i = 0; i < unnecessityIndicators.length(); ++i)
      indicatorPositions.put(unnecessityIndicators.charAt(i), new LinkedList<Integer>());
    int numericTokens = 0, fromPrevToken = 0, parentheses = 0;
    line = line.replaceAll(" +", " ");
    String lineParts[] = line.split(" +");
    for (String linePart : lineParts) {
      numericTokens = NUM_PATTERN.matcher(linePart).matches() ? ++numericTokens : numericTokens;
      for (int c = 0; c < linePart.length(); ++c) {
        int indicatorPos;
        if ((indicatorPos = unnecessityIndicators.indexOf(linePart.charAt(c))) != -1) {
          char charAtC = linePart.charAt(c);
          int actualPos = fromPrevToken + c;
          if (charAtC == '[' && actualPos < line.length() - 1 && !Character.isDigit(line.charAt(actualPos + 1))) {
            indicatorPositions.get(unnecessityIndicators.charAt(indicatorPos)).add(actualPos);
            parentheses++;
          } else if (charAtC == ']' && actualPos > 0 && !Character.isDigit(line.charAt(actualPos - 1))) {
            indicatorPositions.get(unnecessityIndicators.charAt(indicatorPos)).add(actualPos);
            parentheses++;
          } else if (charAtC != ']' && charAtC != '[') {
            indicatorPositions.get(unnecessityIndicators.charAt(indicatorPos)).add(actualPos);
            parentheses++;
          }
        }
      }
      fromPrevToken += linePart.length() + 1;
    }
    return numericTokens > 0.5 * lineParts.length || (parentheses > 1 && isBadlyParenthesized(indicatorPositions));
  }

  private boolean isBadlyParenthesized(Map<Character, List<Integer>> indicatorPositions) {
    String openerChars = "{([";
    Set<Integer> openers = new TreeSet<Integer>();
    Set<Integer> closers = new TreeSet<Integer>();
    for (Entry<Character, List<Integer>> entry : indicatorPositions.entrySet()) {
      if (openerChars.indexOf(entry.getKey()) != -1) {
        openers.addAll(entry.getValue());
      } else {
        closers.addAll(entry.getValue());
      }
    }
    ArrayList<Integer> openerIndices = new ArrayList<Integer>(openers);
    ArrayList<Integer> closerIndices = new ArrayList<Integer>(closers);
    if (openerIndices.size() == 0 || closerIndices.size() == 0)
      return true;
    int visitedClosers = 0;
    for (; visitedClosers < closerIndices.size(); visitedClosers++) {
      if (closerIndices.get(visitedClosers) > openerIndices.get(0)) {
        if (visitedClosers > 1 || openerIndices.size() - closerIndices.size() + visitedClosers > 1) {
          return true;
        } else {
          break;
        }
      }
    }
    int oi = 0;
    for (; visitedClosers < closerIndices.size() && oi < openerIndices.size(); visitedClosers++) {
      // return true i.e.likely to be some kind of equation
      if (openerIndices.get(oi) > closerIndices.get(visitedClosers)
          || (++oi < openerIndices.size() && closerIndices.get(visitedClosers) > openerIndices.get(oi)))
        return true;
    }
    return false;
  }

  private Map<String, String> etalonKeyphrases(String type, String dir) {
    Map<String, String> map = new HashMap<String, String>();
    List<List<String>> answers = NLPUtils.readAsList(dir + "/" + type + ".final", " : ", m_encoding);
    for (List<String> line : answers) {
      StringBuffer keyphrases = new StringBuffer();
      if (line.size() > 1) {
        String[] phrases = line.get(1).split(",");
        for (String s : phrases) {
          String[] parts = s.split("\\+");
          for (String p : parts)
            keyphrases.append(p + "\r\n");
        }
      }
      map.put(line.get(0), keyphrases.toString());
    }
    return map;
  }

}
