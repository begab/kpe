package hu.u_szeged.kpe.aspirants;

import hu.u_szeged.kpe.readers.KpeReader;
import hu.u_szeged.utils.NLPUtils;
import hu.u_szeged.utils.stemmer.PorterStemmer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.WordSense;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.OriginalTextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.NormalizerAnnotator.NormalizerAnnotation;
import edu.stanford.nlp.pipeline.StopWordAnnotator.StopWordAnnotation;

/**
 * An extension of CoreLabel containing ArrayList, that stores words in a lexicographical ordering (based on CoreLabelComparator), after removing
 * stopword components from the representation.
 */

public class NGram extends ArrayList<CoreLabel> implements Cloneable {

  public static PorterStemmer ps = new PorterStemmer();
  private static final long serialVersionUID = 3797853353962652098l;
  private static final CoreLabelComparator coreLabelComparator = new CoreLabelComparator();
  private static WordNetDatabase wn_database;
  private String normalizedForm;

  public enum SequenceType {
    tag, stem, lemma, wikiForm, original
  }

  public NGram() {
    this(4);
  }

  public NGram(int n) {
    super(n);
    normalizedForm = "";
  }

  public NGram(String toAnnotate) {
    Annotation annotation = new Annotation(toAnnotate);
    KpeReader.sentenceAnalyzer.annotate(annotation);
    for (CoreLabel cl : annotation.get(TokensAnnotation.class)) {
      add(cl);
    }
    setNormalizedForm();
  }

  public NGram(String[] originalForms, String[] lemmas) {
    for (int i = 0; i < originalForms.length; ++i) {
      CoreLabel cl = new CoreLabel();
      cl.set(TextAnnotation.class, originalForms[i]);
      cl.set(OriginalTextAnnotation.class, originalForms[i]);
      cl.set(LemmaAnnotation.class, lemmas[i]);
      cl.set(PartOfSpeechAnnotation.class, "dummy");
      add(cl);
    }
    setNormalizedForm();
  }

  public NGram(CoreLabel ew) {
    this();
    add(ew);
    setNormalizedForm();
  }

  public NGram(Collection<CoreLabel> words) {
    super(words);
    setNormalizedForm();
  }

  public boolean add(CoreLabel element) {
    boolean returnValue = super.add(element);
    setNormalizedForm();
    return returnValue;
  }

  public void add(int index, CoreLabel element) {
    super.add(index, element);
    setNormalizedForm();
  }

  public boolean equals(Object o) {
    if (o instanceof CoreLabel) {
      return equals(new NGram((CoreLabel) o));
      // return new NGram((CoreLabel) o).normalizedForm.equals(this.normalizedForm);
    } else if (o instanceof NGram) {
      // return ((NGram) o).normalizedForm.equals(this.normalizedForm);
      NGram n = (NGram) o;
      if (n.size() != size())
        return false;
      for (int i = 0; i < size(); ++i) {
        CoreLabel clThis = get(i);
        CoreLabel other = n.get(i);
        int comparison = coreLabelComparator.compareForNGramEquality(clThis, other);
        if (comparison != 0)
          return false;
      }
      return true;
    } else
      return false;
  }

  public int compareTo(NGram ngram) {
    return this.normalizedForm.compareTo(ngram.normalizedForm);
  }

  public int hashCode() {
    return normalizedForm.hashCode();
  }

  public Object clone() {
    NGram clone = new NGram();
    for (CoreLabel ew : this)
      clone.add(new CoreLabel(ew));
    clone.normalizedForm = normalizedForm;
    return clone;
  }

  public static boolean initWordNet(String wordNetDir) {
    if (wordNetDir != null && !wordNetDir.matches("(?i)[\\p{Punct}\\s]*false[\\p{Punct}\\s]*")) {
      if (new File(wordNetDir).isDirectory()) {
        System.setProperty("wordnet.database.dir", wordNetDir);
        wn_database = WordNetDatabase.getFileInstance();
        return true;
      } else {
        System.err.println("The WordNet dictionary directory provided does not exist.");
        System.err.println("Either disable the usage of WordNet or set its correct location in the config file.");
        System.exit(1);
      }
    }
    return false;
  }

  private static String getTransformedForm(CoreLabel cl) {
    String pos = cl.getString(PartOfSpeechAnnotation.class).toLowerCase();
    Set<SynsetType> synsetTypes = new HashSet<SynsetType>(Arrays.asList(SynsetType.ALL_TYPES));
    if (pos.startsWith("nn")) {
      synsetTypes = new HashSet<SynsetType>(Arrays.asList(new SynsetType[] { SynsetType.NOUN }));
    } else if (pos.startsWith("vb")) {
      synsetTypes = new HashSet<SynsetType>(Arrays.asList(new SynsetType[] { SynsetType.VERB }));
    } else if (pos.startsWith("jj")) {
      synsetTypes = new HashSet<SynsetType>(Arrays.asList(new SynsetType[] { SynsetType.ADJECTIVE, SynsetType.ADJECTIVE_SATELLITE }));
    }

    Map<String, Integer> aggrFreqs = new HashMap<String, Integer>();
    String lemma = cl.get(LemmaAnnotation.class);
    Synset[] synsets = wn_database.getSynsets(lemma);
    // System.out.println(lemma + "\t" + pos);
    for (Synset synset : synsets) {
      SynsetType actualType = synset.getType();
      if (!synsetTypes.contains(actualType))
        continue;
      WordSense[] wss = actualType == SynsetType.VERB ? synset.getDerivationallyRelatedForms(lemma) : new WordSense[] { new WordSense(lemma, synset) };
      for (WordSense w : wss) {
        Synset s = w.getSynset();
        if (pos.startsWith("vb") && s.getType() != SynsetType.NOUN)
          continue;

        String[] forms = s.getWordForms();
        for (String form : forms) {
          Integer prevdVal = aggrFreqs.get(form);
          aggrFreqs.put(form, (prevdVal == null ? 0 : prevdVal) + s.getTagCount(form));
        }
      }
    }
    String argMax = lemma.replaceAll("([a-z])\\1{2,}", "$1$1");
    int freqMax = 0;
    for (Entry<String, Integer> transformationEntry : aggrFreqs.entrySet()) {
      if (transformationEntry.getValue() > freqMax && transformationEntry.getKey().split(" ").length == 1) {
        freqMax = transformationEntry.getValue();
        argMax = transformationEntry.getKey();
      }
    }
    return ps.stemString(argMax);
  }

  /**
   * Sets and returns the normalized representation of a CoreLabel object at the same time.
   * 
   * @param cl
   *          - the CoreLabel to determine the normalized form of
   * @return - the normalized representation of the CoreLabel parameter (or null if the word is known to be a stopword)
   */

  public static String getNormalizedCoreLabel(CoreLabel cl) {
    boolean isStopWord = cl.has(StopWordAnnotation.class) && cl.get(StopWordAnnotation.class);
    String normalization = cl.get(NormalizerAnnotation.class);
    if (normalization != null)
      return isStopWord ? null : normalization;

    if (isStopWord) {
      cl.set(NormalizerAnnotation.class, cl.lemma().toLowerCase());
      return null;
    } else if (wn_database != null) {
      normalization = getTransformedForm(cl);
    } else {
      normalization = ps.stemString(cl.lemma());
    }
    cl.set(NormalizerAnnotation.class, normalization);
    return normalization;
  }

  public void setNormalizedForm() {
    List<String> normalizedTokens = new LinkedList<String>();
    for (CoreLabel cl : this) {
      String normalized = getNormalizedCoreLabel(cl);
      if (normalized != null)
        normalizedTokens.add(normalized);
    }
    Collections.sort(normalizedTokens);
    StringBuilder sb = new StringBuilder();
    for (String token : normalizedTokens) {
      sb.append(token + ' ');
    }
    normalizedForm = sb.toString().trim();
  }

  public String[] getSequence(SequenceType type) {
    String[] sb = new String[size()];
    for (int o = 0; o < size(); ++o) {
      CoreLabel ew = get(o);
      switch (type) {
      case tag:
        sb[o] = ew.get(PartOfSpeechAnnotation.class);
        break;
      case stem:
        sb[o] = ew.get(NormalizerAnnotation.class);
        break;
      case lemma:
        sb[o] = ew.get(LemmaAnnotation.class);
        break;
      case wikiForm:
        sb[o] = (o == size() - 1) ? ew.get(LemmaAnnotation.class) : ew.word();
        break;
      case original:
        sb[o] = ew.word();
        break;
      }
    }
    return sb;
  }

  public String getSequenceAsString(SequenceType type) {
    return getSequenceAsString(type, ' ');
  }

  public String getSequenceAsString(SequenceType type, char joiner) {
    return NLPUtils.join(getSequence(type), joiner);
  }

  public String getCanonicalForm() {
    return normalizedForm;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (CoreLabel cl : this)
      sb.append(cl.word() + ' ');
    return sb.toString().trim();
  }
}
