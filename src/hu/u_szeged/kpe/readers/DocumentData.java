package hu.u_szeged.kpe.readers;

import hu.u_szeged.kpe.aspirants.NGram;
import hu.u_szeged.utils.NLPUtils;
import hu.u_szeged.utils.stemmer.PorterStemmer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

public class DocumentData implements Comparable<DocumentData>, Serializable {

  private static final long serialVersionUID = -8144005167022088407L;

  public static final Set<String> ngramAnnotations = new HashSet<String>(Arrays.asList(new String[] { "PTBTokenizer", "WordsToSentences", "POSTagger",
      "Morpha", "Normalizer", "StopWord" }));

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
  /**
   * Mapping between the formatted strings of the document and their actual formatting
   */
  protected Map<NGram, Set<String>> formattedStrings;
  /** Set of topic labels for the document */
  private Collection<String> topicContainer;
  private DocumentType documentType;

  public static enum DocumentType {
    Contest, Isi, Scientific
  }

  /** Stemmer to normalize tokens of the topic definitions */
  private static PorterStemmer stemmer = new PorterStemmer();

  public DocumentData(String keyph, String fileName, List<String> topics, Class<?> docType) {
    this(keyph, fileName, docType);
    topicContainer = topics;
  }

  public DocumentData(Map<NGram, Integer> keyph, String fileName, int numInFile, Class<?> docType) {
    documentId = totalDocuments++;
    etalonKeyphrases = keyph;
    file = fileName;
    lineNumInFile = numInFile;
    documentType = DocumentType.valueOf(docType.getSimpleName().replace("Reader", ""));
  }

  public DocumentData(String keyph, String fileName, Class<?> docType) {
    documentId = totalDocuments++;
    etalonKeyphrases = transformKeyphrases(keyph);
    file = fileName;
    documentType = DocumentType.valueOf(docType.getSimpleName().replace("Reader", ""));
  }

  public DocumentData(String keyph, String fileName, int numInFile, Class<?> docType) {
    this(keyph, fileName, docType);
    lineNumInFile = numInFile;
  }

  public DocumentData(String keyph, String fileName, String topics, Class<?> docType) {
    this(keyph, fileName, docType);
    determineTopicSet(topics);
  }

  public DocumentData(String keyph, String fileName, String topics, int numInFile, Class<?> docType) {
    this(keyph, fileName, docType);
    lineNumInFile = numInFile;
    determineTopicSet(topics);
  }

  public Collection<String> getTopicSet() {
    return topicContainer;
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

  private void determineTopicSet(String docTopics) {
    String[] topics = docTopics.split("[,;]");
    topicContainer = new TreeSet<String>();
    for (String topic : topics) {
      String[] topicTokens = stemmer.stemString(topic).split(" ");
      Arrays.sort(topicTokens);
      String normalizedTopic = NLPUtils.join(topicTokens);
      if (normalizedTopic.length() > 0)
        topicContainer.add(NLPUtils.join(topicTokens));
    }
  }

  public DocumentType getDocumentType() {
    return documentType;
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
      if (tok.equalsIgnoreCase("c++"))
        newTok = tok;
      else if (tok.startsWith(".net"))
        newTok = tok;
      else
        newTok = tok.replaceAll("^\\p{Punct}|\\p{Punct}$", "");
      if (newTok.length() < tok.length()) {
        System.err.println("Etalon phrase " + tok + " transformed into " + newTok);
      }
      Annotation annotatedContent = new Annotation(newTok);
      KpeReader.sentenceAnalyzer.annotate(annotatedContent, ngramAnnotations, true);
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
    if (documentType == DocumentType.Contest || documentType == DocumentType.Isi || documentType == DocumentType.Scientific) {
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

  public boolean hasUnwantedLastParagraph() {
    return isScientific() && !hasOneSection();
  }

  public boolean isScientific() {
    return documentType == DocumentType.Contest || documentType == DocumentType.Isi || documentType == DocumentType.Scientific;
  }

  public boolean hasOneSection() {
    return documentType == DocumentType.Isi;
  }

  public TreeMap<Integer, List<CoreMap>> getSections(KpeReader reader, boolean serialize) {
    Set<String> annotationsToRepeat = new HashSet<String>();
    // annotationsToRepeat.add("StopWord");
    return getSections(annotationsToRepeat, reader, serialize);
  }

  public TreeMap<Integer, List<CoreMap>> getSections(Set<String> refreshAnnotations, KpeReader reader, boolean serialize) {
    TreeMap<Integer, List<CoreMap>> sectionsWithSentences = new TreeMap<Integer, List<CoreMap>>();
    List<Annotation> sections = tagAndParse(refreshAnnotations, reader, serialize);
    Iterator<Annotation> sectionIter = sections.iterator();
    while (sectionIter.hasNext()) {
      Annotation sectionAnn = sectionIter.next();
      List<CoreMap> sentencesOfSection = sectionAnn.get(SentencesAnnotation.class);
      sectionsWithSentences.put(sectionsWithSentences.size(), sentencesOfSection);
    }
    return sectionsWithSentences;
  }

  @SuppressWarnings("unchecked")
  private List<Annotation> tagAndParse(Set<String> refreshAnnotations, KpeReader reader, boolean serialize) {
    int numberInDoc = getLineNumInFile();
    File f = new File(file);
    String grammarFile = f.getParent() + "/grammar/" + (numberInDoc > 0 ? numberInDoc : "") + f.getName() + ".gr";
    if (serialize && new File(grammarFile).exists()) {
      try {
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(grammarFile)));
        List<Annotation> listOfAnnotation = (List<Annotation>) in.readObject();
        in.close();
        if (refreshAnnotations.size() > 0) {
          for (Annotation ann : listOfAnnotation) {
            KpeReader.sentenceAnalyzer.annotate(ann, refreshAnnotations, false);
          }
          NLPUtils.serialize(listOfAnnotation, grammarFile);
        }
        return listOfAnnotation;
      } catch (Exception e) {
        System.err.println("Error with the serialized grammar file " + grammarFile + "\n" + e);
      }
    }
    // text = text.replaceAll("(.)\\1{4,}", "$1");
    // List<String> sectionsOfText = determineSections(text);

    List<String> paragraphs = determineSections(reader.getText(file, lineNumInFile));
    List<Annotation> documentSections = new ArrayList<Annotation>(paragraphs.size());
    try {
      for (String section : paragraphs) {
        // System.err.println(section);
        // just some ugly hack to get over such expressions as inequalities that would affect the
        // tokenizer to make dull things
        if (isScientific()) {
          int originalLength = section.length();
          section = section.replaceAll("<([\\S&&[^>]]+) +", "< $1 "); // replaceAll("([<>])(\\S+)", "$1 $2");
          if (originalLength - section.length() < 0)
            System.err.println("Type-1 scientific document heuristic was applied for " + file);
          // get rid of hyphens as well that might get into the text unintentionally
          originalLength = section.length();
          section = section.replaceAll("([a-z0-9])-\\s+([a-z0-9])", "$1$2");
          if (originalLength - section.length() > 0)
            System.err.println("Type-2 scientific document heuristic was applied for " + file);
        }

        System.err.println(file + " is to be analysed...");
        Annotation sectionAnn = new Annotation(section);
        KpeReader.sentenceAnalyzer.annotate(sectionAnn);
        documentSections.add(sectionAnn);
      }
      if (serialize && file != null) {
        NLPUtils.serialize(documentSections, grammarFile);
      }
    } catch (Exception e) {
      System.err.println("Error occured during the annotation of file " + file + " of line " + lineNumInFile);
      e.printStackTrace();
    }
    return documentSections;
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
