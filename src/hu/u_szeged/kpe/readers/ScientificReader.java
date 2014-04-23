package hu.u_szeged.kpe.readers;

import hu.u_szeged.utils.NLPUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class ScientificReader extends KpeReader {

  protected static final String DEFAULT_EXTENSION = ".txt";
  // really ugly pattern for detecting numeric expressions
  protected static final Pattern NUM_PATTERN = Pattern.compile("(([-+±]|(\\+/-))?\\d+([.,]\\d+)?%?)+");

  protected void setDetails() {
    fileType = DEFAULT_EXTENSION;
  }

  public List<DocumentData> getContent(String directory, String file) {
    String filePathChunk = file.substring(0, file.indexOf(fileType));
    StringBuffer keywords = new StringBuffer();

    File keyphraseFile = new File(filePathChunk.replace("Content", "KeyPhrase") + ".key");
    if (keyphraseFile.exists()) {
      List<String> lines = new LinkedList<>();
      NLPUtils.readDocToCollection(keyphraseFile, lines);
      for (String st : lines)
        keywords.append(st + "\r\n");
    }
    List<DocumentData> toReturn = new ArrayList<DocumentData>(1);
    toReturn.add(new DocumentData(keywords.toString(), file, this.getClass()));
    return toReturn;
  }

  // public TreeMap<Integer, List<CoreMap>> sectionMapping(DocumentData doc) {
  // List<CoreMap> grammar = tagAndParse(doc).get(SentencesAnnotation.class);
  // int actualSection = 0;
  // TreeMap<Integer, List<CoreMap>> sectionSentences = new TreeMap<Integer, List<CoreMap>>();
  // sectionSentences.put(actualSection, new LinkedList<CoreMap>());
  // boolean refSeen = false;
  // for (int s = 0; s < grammar.size(); ++s){
  // CoreMap sentence = grammar.get(s);
  // List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
  // String firstToken = tokens.get(0).word();
  // StringBuffer firstThreeTokens = new StringBuffer(firstToken);
  // for (int i = 1; i < 3 && i < tokens.size(); ++i){
  // firstThreeTokens.append(' ' + tokens.get(i).word());
  // }
  //
  // if (!refSeen && firstToken.matches("\\d+")){
  // int sect = Integer.parseInt(firstToken);
  // if (sect > actualSection && sect - actualSection < 3 && sect != actualSection){
  // // in case a section header were missed
  // boolean ok = true;
  // for (int i = 1; i < 6 && s + i < grammar.size(); ++i){
  // String firstExtendedWordISentenceAway = grammar.get(s +
  // i).get(TokensAnnotation.class).get(0).word();
  // if (firstExtendedWordISentenceAway.matches("\\d+")
  // && Integer.parseInt(firstExtendedWordISentenceAway) - sect == 1){
  // ok = false;
  // break;
  // }
  // }
  // if (ok)
  // sectionSentences.put((actualSection = sect), new LinkedList<CoreMap>());
  // }
  // }else if (firstThreeTokens.toString().matches("(?i)(\\d+ (\\. )?)?references?")){
  // refSeen = true;
  // sectionSentences.put(++actualSection, new LinkedList<CoreMap>());
  // }
  // sectionSentences.get(actualSection).add(sentence);
  // }
  // return sectionSentences;
  // }

  @Override
  protected boolean mightBeSectionHeader(String line) {
    return line.matches("[1-9]\\d?(\\.\\d+\\.?)*\\.?\\s.+");
  }

  public boolean hasUnwantedLastParagraph() {
    return true;
  }

  @Override
  public String getText(String file, int numberWithinFile) {
    StringBuffer article = new StringBuffer();
    try {
      boolean firstAbstractSeen = false;
      String line;
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), getEncoding()));
      while ((line = br.readLine()) != null) {
        if (!firstAbstractSeen) {
          int originalLength = line.length();
          line = line.replaceAll(" A(bstract|BSTRACT) ", "\r\nAbstract\r\n");
          firstAbstractSeen = line.length() == originalLength + 2;
        }
        article.append(line + "\r\n");
      }
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return article.toString();
  }
}
