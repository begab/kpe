package hu.u_szeged.kpe.readers;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.utils.NLPUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.MweDictAnnotator.MWEAnnotation;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class DocumentData implements Comparable<DocumentData>, Serializable {

  private static final long serialVersionUID = -8144005167022088407L;

  /** The total number of DocumentData objects initialized */
  private static int totalDocuments;
  /** Identifier of the document */
  private int documentId;
  /** Location of the document */
  private String file;
  /** Stores ordinal number of the document within the file */
  private int lineNumInFile;
  /** Keyphrases of the document */
  private Map<NGram, Integer> etalonKeyphrases;
  /** Acronyms of the document */
  private Map<String, Integer> acronyms;
  /** Mapping between the formatted strings of the document and their actual formatting */
  protected Map<NGram, Set<String>> formattedStrings;
  /** This can be useful to define reader specific behavior */
  private String documentType;

  public DocumentData(String keyph, String fileName, Class<?> docType) {
    documentId = totalDocuments++;
    etalonKeyphrases = transformKeyphrases(keyph);
    file = fileName;
    documentType = docType.getSimpleName().replace("Reader", "");
  }

  public int getDocId() {
    return documentId;
  }

  public void setDocId(int id) {
    documentId = id;
  }

  public Map<NGram, Set<String>> getFormattedStrings() {
    return formattedStrings;
  }

  public Map<NGram, Integer> getKeyphrases() {
    return etalonKeyphrases;
  }

  public void setKeyphrases(String keyph) {
    etalonKeyphrases = transformKeyphrases(keyph);
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public Map<String, Integer> getAcronyms() {
    return acronyms;
  }

  public void setAcronyms(Map<String, Integer> acr) {
    acronyms = acr;
  }

  public boolean isFormatted(NGram phraseBuffer) {
    return formattedStrings != null && formattedStrings.containsKey(phraseBuffer);
  }

  // public boolean isFormatted(CoreLabel ew) {
  // return formattedStringParts != null && formattedStringParts.contains(ew);
  // }

  /**
   * Gets all the phrases in the given string and puts them into a map with its occurrences.
   */
  public Map<NGram, Integer> transformKeyphrases(String keyphrases) {
    HashMap<NGram, Integer> hash = new HashMap<NGram, Integer>();
    if (keyphrases == null || keyphrases.length() == 0)
      return hash;
    for (String tok : keyphrases.split("(\r?\n)+")) {
      tok = tok.trim();
      if (tok.length() == 0)
        continue;

      String newTok;
      if (tok.equalsIgnoreCase("c++")) {
        newTok = tok;
      } else if (tok.startsWith(".net")) {
        newTok = tok;
      } else {
        newTok = tok.replaceAll("^\\p{Punct}|\\p{Punct}$", "");
      }

      if (newTok.length() < tok.length()) {
        System.err.println("Etalon phrase " + tok + " transformed into " + newTok);
      }
      Annotation annotatedContent = new Annotation(newTok);
      KpeReader.sentenceAnalyzer.annotate(annotatedContent);
      NGram id = new NGram(annotatedContent.get(TokensAnnotation.class));
      Integer value = hash.get(id);
      hash.put(id, value == null ? 1 : ++value);
    }
    return hash;
  }

  public int getLineNumInFile() {
    return lineNumInFile;
  }

  public String toString() {
    return documentId + "\t" + file;
  }

  public int compareTo(DocumentData dd) {
    int fileComparison = file.compareTo(dd.getFile());
    return fileComparison == 0 ? (lineNumInFile < dd.getLineNumInFile() ? -1 : 1) : fileComparison;
  }

  public boolean equals(Object o) {
    if (!(o instanceof DocumentData)) {
      return false;
    }
    return file.equals(((DocumentData) o).getFile()) && ((DocumentData) o).getLineNumInFile() == lineNumInFile;
  }

  public int hashCode() {
    return (file + "_" + lineNumInFile).hashCode();
  }

  public boolean containsReference(CoreMap sentence) {
    List<CoreLabel> sentenceTokens = sentence.get(TokensAnnotation.class);
    if (documentType.matches("(?i)semeval|scientific")) {
      nobracket: for (int i = 0; i < sentenceTokens.size(); ++i) {
        if (sentenceTokens.get(i).word().equals("-LRB-")) {
          while (++i < sentenceTokens.size()) {
            CoreLabel nextToken = sentenceTokens.get(i);
            if (!nextToken.word().matches("(,|\\d+|[A-Z&a-z]+\\d{2}|-RRB-)")) {
              continue nobracket;
            } else if (nextToken.word().equals("-RRB-")) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  public boolean isScientific() {
    return documentType.matches("(?i)semeval|scientific");
  }

  public TreeMap<Integer, List<CoreMap>> getSections(KpeReader reader, boolean serialize) {
    TreeMap<Integer, List<CoreMap>> sectionsWithSentences = new TreeMap<Integer, List<CoreMap>>();
    List<Annotation> sections = tagAndParse(reader, serialize);
    Iterator<Annotation> sectionIter = sections.iterator();
    while (sectionIter.hasNext()) {
      Annotation sectionAnn = sectionIter.next();
      List<CoreMap> sentencesOfSection = sectionAnn.get(SentencesAnnotation.class);
      sectionsWithSentences.put(sectionsWithSentences.size(), sentencesOfSection);
    }
    return sectionsWithSentences;
  }

  /**
   * Checks for the presence of some critical annotations. In the case some of those entered among the parameters is missing, the texts needs to be
   * re-annotated.
   * 
   * @param a
   *          annotation
   * @param r
   *          reader with the desired annotations
   * @return
   */
  private boolean needsReannotation(Annotation a, KpeReader r) {
    List<CoreMap> sentences = a.get(SentencesAnnotation.class);
    List<CoreLabel> tokens = a.get(TokensAnnotation.class);
    if (tokens == null || sentences == null || tokens.size() == 0 || sentences.size() == 0) {
      return true;
    }
    Set<Class<?>> sentenceAnnotations = sentences.get(0).keySet();
    Set<Class<?>> tokenAnnotations = tokens.get(0).keySet();
    if ((r.getIsMweOn() && !tokenAnnotations.contains(MWEAnnotation.class)) || (r.getIsNeOn() && !tokenAnnotations.contains(NamedEntityTagAnnotation.class))) {
      return true;
    }
    if (r.getIsSyntaxOn() && !sentenceAnnotations.contains(TreeAnnotation.class)) {
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private List<Annotation> tagAndParse(KpeReader reader, boolean serialize) {
    int numberInDoc = getLineNumInFile();
    File f = new File(file);
    String grammarFile = f.getParent() + "/grammar/" + (numberInDoc > 0 ? numberInDoc : "") + f.getName() + ".gr";
    if (serialize && new File(grammarFile).exists()) {
      try {
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(grammarFile)));
        List<Annotation> documentSections = (List<Annotation>) in.readObject();
        in.close();
        if (documentSections.size() == 0 || needsReannotation(documentSections.get(0), reader)) {
          analyzeSections(documentSections, grammarFile, serialize);
        }
        return documentSections;
      } catch (Exception e) {
        System.err.println("Error with the serialized grammar file " + grammarFile + "\n" + e);
      }
    }
    // text = text.replaceAll("(.)\\1{4,}", "$1");
    // List<String> sectionsOfText = determineSections(text);

    List<String> paragraphs = determineSections(reader.getText(file, lineNumInFile));
    List<Annotation> documentSections = new ArrayList<Annotation>(paragraphs.size());
    try {
      System.err.println(file + " is to be analysed...");
      for (String section : paragraphs) {
        // just some ugly hack to get over such expressions as inequalities that would affect the tokenizer to make dull things
        if (isScientific()) {
          int originalLength = section.length();
          section = section.replaceAll("<([\\S&&[^>]]+) +", "< $1 "); // replaceAll("([<>])(\\S+)", "$1 $2");
          if (originalLength - section.length() < 0) {
            System.err.println("Type-1 scientific document heuristic was applied for " + file);
          }
          // get rid of hyphens as well that might get into the text unintentionally
          originalLength = section.length();
          section = section.replaceAll("([a-z0-9])-\\s+([a-z0-9])", "$1$2");
          if (originalLength - section.length() > 0) {
            System.err.println("Type-2 scientific document heuristic was applied for " + file);
          }
        }
        documentSections.add(new Annotation(section));
      }
      analyzeSections(documentSections, grammarFile, serialize);
    } catch (Exception e) {
      System.err.println("Error occured during the annotation of file " + file + " of line " + lineNumInFile);
      e.printStackTrace();
    }
    return documentSections;
  }

  private void analyzeSections(List<Annotation> documentSections, String grammarFile, boolean serialize) {
    for (Annotation ann : documentSections) {
      KpeReader.sentenceAnalyzer.annotate(ann);
    }
    if (serialize && file != null) {
      NLPUtils.serialize(documentSections, grammarFile);
    }
  }

  /**
   * @param text
   * @return the List of sections of the document
   */
  private List<String> determineSections(String text) {
    // In the simplest case the whole text is handled as one section.
    List<String> sections = new ArrayList<String>(1);
    sections.add(text);
    return sections;
  }

}
