package hu.u_szeged.kpe.main;

import hu.u_szeged.kpe.aspirants.CoreLabelComparator;
import hu.u_szeged.kpe.aspirants.NGram;
import hu.u_szeged.kpe.aspirants.NGramStats;
import hu.u_szeged.kpe.features.Feature;
import hu.u_szeged.kpe.features.FeatureHolder;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.kpe.readers.KpeReader;
import hu.u_szeged.ml.ClassificationResult;
import hu.u_szeged.ml.DataHandler;
import hu.u_szeged.ml.DataMiningException;
import hu.u_szeged.ml.Model;
import hu.u_szeged.ml.mallet.MalletDataHandler;
import hu.u_szeged.utils.ClassificationInstance;
import hu.u_szeged.utils.NLPUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.types.Alphabet;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StopWordAnnotator;
import edu.stanford.nlp.pipeline.StopWordAnnotator.StopWordAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class KPEFilter implements Serializable {

  private static final long serialVersionUID = -4424060992087580944L;
  /** The maximum length of phrases */
  private int maxPhraseLength = 5;
  /** The minimum length of phrases */
  private int minPhraseLength = 1;
  /** The minimum number of occurrences of a phrase */
  private int minNumOccur = 1;
  /** The dictionary containing the global corpus statistics of the keyphrase candidates */
  private Map<String, Integer[]> m_Dictionary;
  /** The number of documents in the global frequencies corpus */
  private int numDocs = 0;
  /** The number of sections in the global frequencies corpus */
  private int numSections = 0;
  /** The name of the classifier to apply */
  private String classifierName;
  /** The list of alphabets of the classifier (i.e. labels and features) */
  private List<Alphabet> m_Alphabets;
  /** The actual classifier used to compute probabilities */
  private Model learnedModel;
  /** The list of features */
  private FeatureHolder features;
  /** Collection of interesting words collected for feature value calculation during the processing of documents */
  private Map<CoreLabel, Integer> indicatorWords;
  /** Some sort of external knowledge storing Collection */
  public static Map<CoreLabel, List<double[]>> wordList;
  // /** Container that contains possible synonyms */
  // public static Map<CoreLabel, Set<String>> synsets;
  /** Regexp matching only those POS codes that are welcomed in the current setting */
  private Pattern posRegexp;
  /** Set of ngrams regarded as too commons thus as such ones that are not supposed to be built in the model */
  private Set<String> commonNGrams = new HashSet<String>();
  /** Indicates if stopwords-based pruning of keyphrases is _not_ performed */
  private boolean noStopWordPruning;
  /** Indicates if POS ending-based pruning of keyphrases is _not_ performed */
  private boolean noPosEndingPruning;

  public KPEFilter(boolean noSWpruning, boolean noPOSpruning) {
    noStopWordPruning = noSWpruning;
    noPosEndingPruning = noPOSpruning;
  }

  public FeatureHolder getFeatures() {
    return features;
  }

  public Map<CoreLabel, List<double[]>> getWordList() {
    return wordList;
  }

  public String getClassifierName() {
    return classifierName;
  }

  /**
   * @param limitThreshold
   * @return those ExtendedWords that were present more than limitThreshold times in the training corpus
   */
  public Set<CoreLabel> determineIndicatingVerbs(int limitThreshold) {
    Set<CoreLabel> indicators = new TreeSet<CoreLabel>(new CoreLabelComparator());
    for (Entry<CoreLabel, Integer> entry : indicatorWords.entrySet()) {
      if (entry.getValue() >= limitThreshold)
        indicators.add(entry.getKey());
    }
    return indicators;
  }

  //
  // public boolean getAcceptSynonyms() {
  // return acceptSynonyms;
  // }
  //
  // public void setAcceptSynonyms(boolean synonyms) {
  // acceptSynonyms = synonyms;
  // }

  public void setClassifierName(String classifier) {
    classifierName = classifier;
  }

  /**
   * Sets the number of documents in the training corpus.
   * 
   * @param numOfDocs
   */
  public void setDocsNumber(int numOfDocs) {
    numDocs = numOfDocs;
    System.err.println(numDocs + " training documents.");
  }

  /**
   * @return Number of documents in the training corpus.
   */
  public int getDocsNumber() {
    return numDocs;
  }

  /**
   * @return Number of sections in the training corpus.
   */
  public int getNumSections() {
    return numSections;
  }

  public Map<String, Integer[]> getDictionary() {
    return m_Dictionary;
  }

  /**
   * Get the value of MinNumOccur.
   * 
   * @return Value of MinNumOccur.
   */
  public int getMinNumOccur() {
    return minNumOccur;
  }

  /**
   * Set the value of MinNumOccur.
   * 
   * @param newMinNumOccur
   *          Value to assign to MinNumOccur.
   */
  public void setMinNumOccur(int newMinNumOccur) {
    minNumOccur = newMinNumOccur;
  }

  /**
   * Get the value of MaxPhraseLength.
   * 
   * @return Value of MaxPhraseLength.
   */
  public int getMaxPhraseLength() {
    return maxPhraseLength;
  }

  /**
   * Set the value of MaxPhraseLength.
   * 
   * @param newMaxPhraseLength
   *          Value to assign to MaxPhraseLength.
   */
  public void setMaxPhraseLength(int newMaxPhraseLength) {
    maxPhraseLength = newMaxPhraseLength;
  }

  /**
   * Get the value of MinPhraseLength.
   * 
   * @return Value of MinPhraseLength.
   */
  public int getMinPhraseLength() {
    return minPhraseLength;
  }

  /**
   * Set the value of MinPhraseLength.
   * 
   * @param newMinPhraseLength
   *          Value to assign to MinPhraseLength.
   */
  public void setMinPhraseLength(int newMinPhraseLength) {
    minPhraseLength = newMinPhraseLength;
  }

  public List<Alphabet> getAlphabets() {
    return m_Alphabets;
  }

  public void setAlphabets(List<Alphabet> alphabet) {
    m_Alphabets = alphabet;
  }

  public void setModel(Model model) {
    learnedModel = model;
  }

  public Model getModel() {
    return learnedModel;
  }

  public void setNumFeature(List<String> feature, String classifier, Map<String, Boolean> employBIESfeatureMarkup) throws Exception {
    posRegexp = Pattern.compile("(?i)nnp?s?|jj[rs]?|vb[dgnpz]?");
    features = new FeatureHolder(feature.size());
    // no longer should it be necessary to involve the two basic KEA features into our feature set
    // (esp. for the opinion phrase extraction)
    // features.add(new TfIdfFeature());
    // features.add(new FirstIndexFeature());
    classifierName = classifier;
    for (String feat : feature) {
      try {
        Feature f = Class.forName("hu.u_szeged.kpe.features." + feat).asSubclass(Feature.class).newInstance();
        Boolean employBIES = employBIESfeatureMarkup.get(feat);
        f.setEmployBIESmarkup(employBIES);
        features.add(f);
        if (feat.equals("IndicatingVerbsFeature")) {
          indicatorWords = new TreeMap<CoreLabel, Integer>(new CoreLabelComparator());
        }
      } catch (Exception e) {
        System.err.println("Problem with feature: " + feat);
        e.printStackTrace();
      }
    }
    features.get(0).setBinarization(!classifierName.equalsIgnoreCase("naivebayes"));
  }

  /**
   * Determines whether featureType is used in the current feature set.
   * 
   * @param featureType
   *          Feature name of interest.
   */
  // private boolean isUsed(String featureType) {
  // for (Feature f : features)
  // if (f.getClass().getName().contains(featureType))
  // return true;
  // return false;
  // }

  public void initializeFeatureFields() {
    features.get(0).setBinarization(!classifierName.equalsIgnoreCase("naivebayes"));
    for (Feature f : features)
      f.setFeatureField(this);
  }

  private String trasformSWNtoStanfordPOS(String swnPOS) {
    if (swnPOS.equalsIgnoreCase("a")) {
      return "JJ";
    } else if (swnPOS.equalsIgnoreCase("n")) {
      return "NN";
    } else if (swnPOS.equalsIgnoreCase("v")) {
      return "VB";
    } else {
      return "RB";
    }
  }

  @SuppressWarnings("unchecked")
  public void fillWordList(String file) {
    if (wordList == null && new File(file + ".ser").exists()) {
      try {
        wordList = (Map<CoreLabel, List<double[]>>) new ObjectInputStream(new BufferedInputStream(new FileInputStream(file + ".ser"))).readObject();
      } catch (Exception e) {
        System.err.println("Error in deserializing file " + file + ".ser");
        e.printStackTrace();
        System.exit(1);
      }
    } else if (wordList == null) {
      doTheRest(file);
    }

    // try {
    // if (acceptSynonyms && new File(file + "Synsets.ser").exists()) {
    // synsets = (Map<CoreLabel, Set<String>>) new ObjectInputStream(new BufferedInputStream(new
    // FileInputStream(file + "Synsets.ser")))
    // .readObject();
    // }
    // } catch (Exception e) {
    // System.err.println("Error in reading deserializing file " + file + "Synsets.ser");
    // System.exit(1);
    // }
  }

  private void doTheRest(String file) {
    wordList = new TreeMap<CoreLabel, List<double[]>>(new CoreLabelComparator());
    // synsets = new TreeMap<CoreLabel, Set<String>>(new CoreLabelComparator());
    List<String> lines = new LinkedList<>();
    NLPUtils.readDocToCollection(file, lines);
    for (String line : lines) {
      String[] parts = line.split("\t");
      double posValue = Double.parseDouble(parts[2]), negValue = Double.parseDouble(parts[3]);
      if ((posValue + negValue) == 0.0)
        continue;
      String[] tokens = parts[4].split(" ");
      Set<String> tokensSet = new HashSet<String>();
      Set<CoreLabel> coreLabelSet = new HashSet<CoreLabel>();
      for (String token : tokens) {
        token = token.replaceAll("(#[a-z])?#\\d+", ""); // "(#[a-z])?" is for the different versions of SWN
        String[] tokenParts = token.split("_");
        if (tokenParts.length > 1)
          continue;
        String pos = trasformSWNtoStanfordPOS(parts[0]);
        CoreLabel key = new CoreLabel();
        key.set(LemmaAnnotation.class, tokenParts[0]);
        key.setTag(pos);
        String normalized = NGram.getNormalizedCoreLabel(key);
        tokensSet.add(normalized);
        coreLabelSet.add(key);
        if (!wordList.containsKey(key))
          wordList.put(key, new LinkedList<double[]>());
        wordList.get(key).add(new double[] { posValue, negValue });
      }
    }
    NLPUtils.serialize(wordList, file + ".ser");
  }

  /**
   * Updates the global dictionaries with some statistics.
   */
  public void updateGlobalDictionary(Map<NGram, Integer> keyNGrams, Map<Integer, List<CoreMap>> sections) {
    if (m_Dictionary == null)
      m_Dictionary = new HashMap<String, Integer[]>();
    // m_Dictionary = new HashMap<String, Entry<Set<NGram>, Integer[]>>();
    Map<String, Entry<Set<NGram>, Integer[]>> hash = getPhrasesForDict(keyNGrams, sections);
    for (Entry<String, Entry<Set<NGram>, Integer[]>> normalizedPhrase : hash.entrySet()) {
      Integer[] toUpdate = m_Dictionary.get(normalizedPhrase.getKey());
      // Entry<Set<NGram>, Integer[]> toUpdate = m_Dictionary.get(normalizedPhrase.getKey());
      if (toUpdate == null) {
        // Set<NGram> ngramVariations = new HashSet<NGram>();
        // toUpdate = new SimpleEntry<Set<NGram>, Integer[]>(ngramVariations, new Integer[]{0, null, 0, 0});
        toUpdate = new Integer[] { null, null, 0, 0 };
      }
      for (int j = 0; j < normalizedPhrase.getValue().getValue().length; ++j) {
        if (normalizedPhrase.getValue().getValue()[j] != null) {
          toUpdate[j] = (toUpdate[j] != null ? toUpdate[j] : 0) + normalizedPhrase.getValue().getValue()[j];
          // toUpdate.getValue()[j] = (toUpdate.getValue()[j] != null ? toUpdate.getValue()[j] : 0)
          // + normalizedPhrase.getValue().getValue()[j];
        }
      }
      // toUpdate.getKey().addAll(normalizedPhrase.getValue().getKey());
      m_Dictionary.put(normalizedPhrase.getKey(), toUpdate);
    }
  }

  public void setCommonWords(double commonWordThreshold, int numOfDocs) {
    if (numOfDocs < 500) {
      System.err.println("Note that no term frequency-based pruning will take place as there are less than 500 training documents.");
      return;
    }

    for (Entry<String, Integer[]> normalizedForm : m_Dictionary.entrySet()) {
      // for (Entry<String, Entry<Set<NGram>, Integer[]>> normalizedForm : m_Dictionary.entrySet()){
      // if (normalizedForm.getValue().getValue()[0] > commonWordThreshold * numOfDocs){
      Integer[] stats = normalizedForm.getValue();
      int nonKeyphraseOccurrance = (stats[1] == null ? 0 : stats[1]);
      if (nonKeyphraseOccurrance > commonWordThreshold * numOfDocs) {
        commonNGrams.add(normalizedForm.getKey());
        System.err.println("Normalized form too common: " + normalizedForm + "\t" + nonKeyphraseOccurrance + " non-keyphrase occurrences.");
      }
    }
    System.err.println(commonNGrams.size() + "\t" + m_Dictionary.size());
  }

  public static String nGram2StemmedString(NGram ngram) {
    List<String> tokens = new LinkedList<String>();
    for (CoreLabel cl : ngram) {
      tokens.add(cl.word().toLowerCase());
    }
    Collections.sort(tokens);
    String ordered = NLPUtils.join(tokens.toArray(new String[tokens.size()]));
    return NGram.ps.stemString(ordered);
  }

  /**
   * Computes the feature values for a given phrase.
   */
  public double[] addFeats(boolean target, Entry<String, Map<NGram, NGramStats>> phraseForms, List<int[]> length,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<Map<Integer, List<CoreMap>>> grammars, DataHandler dh, boolean train, DocumentData... docs) {
    Set<String> stemmedPhraseAlternations = new HashSet<String>();
    for (Entry<NGram, NGramStats> orthographicForm : phraseForms.getValue().entrySet()) {
      stemmedPhraseAlternations.add(nGram2StemmedString(orthographicForm.getKey()));
    }

    double[] dedicatedFeats = new double[3];
    StringBuffer actualDocId = new StringBuffer((train && target ? "target_" : ""));
    if (train)
      for (DocumentData doc : docs)
        actualDocId.append(doc.getDocId() + "_");

    String instanceId = (actualDocId.append(phraseForms.getKey()).toString()).replaceAll(" ", "_");
    // calculate feature values
    features.updateDataHandler(dedicatedFeats, phraseForms.getKey(), instanceId, length, listOfHashs, grammars, dh, train, docs);

    Map<String, Integer> normalizedKeyphrases = new HashMap<String, Integer>();
    Map<String, Integer> stemKeyphrases = new HashMap<String, Integer>();
    Map<NGram, Integer> keyphrases = new HashMap<NGram, Integer>();

    for (DocumentData doc : docs) {
      for (Entry<NGram, Integer> keyphrase : doc.getKeyphrases().entrySet()) {
        Integer prevVal = normalizedKeyphrases.get(keyphrase.getKey().getCanonicalForm());
        normalizedKeyphrases.put(keyphrase.getKey().getCanonicalForm(), (prevVal == null ? 0 : prevVal) + keyphrase.getValue());

        String stemmedForm = nGram2StemmedString(keyphrase.getKey());
        prevVal = stemKeyphrases.get(stemmedForm);
        stemKeyphrases.put(stemmedForm, (prevVal == null ? 0 : prevVal) + keyphrase.getValue());

        prevVal = keyphrases.get(keyphrase.getKey());
        keyphrases.put(keyphrase.getKey(), (prevVal == null ? 0 : prevVal) + keyphrase.getValue());
      }
    }

    boolean trueClassLabel = normalizedKeyphrases.containsKey(phraseForms.getKey());
    if (!trueClassLabel) {
      for (String stemmedPhraseAlternation : stemmedPhraseAlternations) {
        if (stemKeyphrases.containsKey(stemmedPhraseAlternation)) {
          trueClassLabel = true;
          break;
        }
      }
    }
    // calculate class value
    if (trueClassLabel) {
      dh.setLabel(instanceId, true);
      dedicatedFeats[2] = 1.0;
    } else {
      // if (acceptSynonyms && containsEpinionated) {
      // fillWordList("data/SentiWordNet");
      // for (NGram keyphrase : keyphrases.keySet()) {
      // int matchingPhrases = 0;
      // for (CoreLabel keyPart : keyphrase) {
      // Set<String> synset = synsets.get(keyPart);
      // if (synset != null && synset.contains(keyPart)) {
      // matchingPhrases++;
      // break;
      // }
      // }
      // if (matchingPhrases == keyphrase.size() && matchingPhrases > 0) {
      // dh.setLabel(instanceId, true);
      // return dedicatedFeats;
      // }
      // }
      // }
      dh.setLabel(instanceId, false);
    }
    return dedicatedFeats;
  }

  /**
   * Returns the keyphrase aspirants of a document ranked by their posterior probabilities.
   */
  public List<ClassificationInstance> rankDocumentInstances(KpeReader r, List<int[]> lengths, boolean serialize, DocumentData... documents)
      throws DataMiningException {
    DataHandler mdh = new MalletDataHandler();
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("useFeatureSet", m_Alphabets);
    mdh.createNewDataset(parameters);

    List<Map<Integer, List<CoreMap>>> grammars = new ArrayList<Map<Integer, List<CoreMap>>>(documents.length);
    List<Map<String, Map<NGram, NGramStats>>> listOfHashs = new ArrayList<Map<String, Map<NGram, NGramStats>>>(documents.length);
    for (DocumentData doc : documents) {
      // XXX as assumed this is just a dummy value here, ensuring that we do not throw away anything with respect that constraining
      // at some point it might turn out that some value having some real effect would better suit here
      int maxSection = Integer.MAX_VALUE;
      Map<String, Map<NGram, NGramStats>> hash = new HashMap<String, Map<NGram, NGramStats>>();
      TreeMap<Integer, List<CoreMap>> sections = doc.getSections(r, serialize);
      lengths.add(getPhrases(hash, doc, maxSection, sections));
      grammars.add(sections);
      listOfHashs.add(hash);
    }

    Set<String> phrasesOfDocuments = new HashSet<String>();
    List<ClassificationInstance> instances = new ArrayList<ClassificationInstance>();
    for (Map<String, Map<NGram, NGramStats>> map : listOfHashs) {
      for (Entry<String, Map<NGram, NGramStats>> ngram : map.entrySet()) {
        if (!commonNGrams.contains(ngram.getKey()) && phrasesOfDocuments.add(ngram.getKey())) {
          double[] vals = addFeats(true, ngram, lengths, listOfHashs, grammars, mdh, false, documents);
          instances.add(new ClassificationInstance(ngram.getKey(), vals, ngram.getValue()));
        }
      }
    }

    ClassificationResult res = mdh.classifyDataset(learnedModel);
    for (ClassificationInstance inst : instances) {
      Map<Comparable<?>, Double> probabilities = res.getPredictionProbabilities(inst.getId());
      inst.setProbability(probabilities.get(new Boolean(true).toString()));
    }

    // Sort phrases according to their distance (stable sort)
    int index = 0;
    double[] vals = new double[instances.size()];
    for (ClassificationInstance inst : instances)
      vals[index++] = inst.getFirstOccurr();

    List<ClassificationInstance> newInstances = new ArrayList<ClassificationInstance>(instances.size());
    int[] sortedIndices = NLPUtils.stableSort(vals);
    for (int i = 0; i < vals.length; i++)
      newInstances.add(instances.get(sortedIndices[i]));

    instances = newInstances;
    index = 0;

    // Sort phrases according to their tf-idf value (stable sort)
    for (ClassificationInstance inst : instances)
      vals[index++] = -inst.getTfIdf();

    newInstances = new ArrayList<ClassificationInstance>(instances.size());
    sortedIndices = NLPUtils.stableSort(vals);
    for (int i = 0; i < vals.length; i++)
      newInstances.add(instances.get(sortedIndices[i]));

    instances = newInstances;
    index = 0;

    // Sort phrases according to their probability (stable sort)
    for (ClassificationInstance inst : instances)
      vals[index++] = 1 - inst.getProbability();

    newInstances = new ArrayList<ClassificationInstance>(instances.size());
    sortedIndices = NLPUtils.stableSort(vals);
    for (int i = 0; i < vals.length; i++) {
      newInstances.add(instances.get(sortedIndices[i]));
    }
    instances = newInstances;

    // Compute rank of phrases. Check for subphrases that are ranked lower than
    // superphrases and assign probability -1 and set the rank to Integer.MAX_VALUE
    int rank = 1;
    for (int i = 0; i < instances.size(); ++i) {
      // Short cut: if phrase very unlikely make rank very low and continue
      ClassificationInstance ci = instances.get(i);
      double falseProbability = vals[sortedIndices[i]];
      if (NLPUtils.grOrEq(falseProbability, 1.0)) {
        ci.setRanking(Integer.MAX_VALUE);
        continue;
      }

      // Otherwise look for super phrase starting with first phrase in list that has same
      // probability, TFxIDF value, and distance as current phrase. We do this to catch all
      // superphrases that have same probability, TFxIDF value and distance as current phrase.
      int startInd = i;
      while (startInd < vals.length) {
        ClassificationInstance inst = instances.get(startInd);
        if ((inst.getTfIdf() != ci.getTfIdf()) || (inst.getProbability() != ci.getProbability()) || (inst.getFirstOccurr() != ci.getFirstOccurr())) {
          break;
        }
        startInd++;
      }
      ci.setRanking(rank++);
    }
    return instances;
  }

  /**
   * Returns a HashMap that is filled with the normalized n-grams occurring in the given document and the number of times it occurs in various
   * situations.
   */
  public Map<String, Entry<Set<NGram>, Integer[]>> getPhrasesForDict(Map<NGram, Integer> keyNGrams, Map<Integer, List<CoreMap>> sectionSentences) {
    CoreLabel[] buffer = new CoreLabel[maxPhraseLength];
    Map<String, Entry<Set<NGram>, Integer[]>> hash = new HashMap<String, Entry<Set<NGram>, Integer[]>>();
    Map<String, Set<Integer>> ngramSections = new HashMap<String, Set<Integer>>();
    Map<String, Integer> keys = new HashMap<String, Integer>();
    for (Entry<NGram, Integer> key : keyNGrams.entrySet()) {
      Integer prevVal = keys.get(key.getKey().getCanonicalForm());
      keys.put(key.getKey().getCanonicalForm(), (prevVal == null ? 0 : prevVal) + key.getValue());
    }
    int sectionsToAdd = 0;

    for (Entry<Integer, List<CoreMap>> section : sectionSentences.entrySet()) {
      if (section.getValue().size() > 0)
        sectionsToAdd++;
      for (CoreMap sentence : section.getValue()) {
        int numSeen = 0;
        for (CoreLabel word : sentence.get(TokensAnnotation.class)) {
          // TODO only to create dictionaries for profiling experiments
          // if (tabuIndex != -1 && word.get(CharacterOffsetEndAnnotation.class) >= tabuIndex)
          // if (word.get(OriginalTextAnnotation.class).equals("Recommended:"))
          // break review;
          String wordForm = word.word();
          NGram.getNormalizedCoreLabel(word);
          boolean isStopWord = word.get(StopWordAnnotation.class);
          if ((!isStopWord && !posRegexp.matcher(word.tag()).matches()) || wordForm.split("\\s").length > 1
              || wordForm.matches("-[LR]RB-|\\p{Punct}+|.*[._:;].*")) {
            numSeen = 0;
            continue;
          }

          for (int i = 0; i < maxPhraseLength - 1; i++) {
            if (buffer[i + 1] != null) {
              buffer[i] = buffer[i + 1];
            }
          }
          buffer[maxPhraseLength - 1] = word;
          numSeen = Math.min(++numSeen, maxPhraseLength);

          if (word.get(StopWordAnnotation.class))
            continue;

          if (indicatorWords != null && !wordForm.matches("\\d+") && word.tag().matches("(?i)vb.?")) {
            Integer prevVal = indicatorWords.get(word);
            indicatorWords.put(word, prevVal == null ? 1 : ++prevVal);
          }

          NGram phraseBuffer = new NGram(maxPhraseLength);
          for (int i = 1; i <= numSeen; i++) {
            CoreLabel actWord = buffer[maxPhraseLength - i];
            phraseBuffer = (NGram) phraseBuffer.clone();
            phraseBuffer.add(0, actWord);

            if (i < minPhraseLength || actWord.get(StopWordAnnotation.class))
              continue;

            String canonicalForm = phraseBuffer.getCanonicalForm();
            Set<Integer> sections = ngramSections.get(canonicalForm);
            sections = sections == null ? new HashSet<Integer>() : sections;
            sections.add(section.getKey());
            ngramSections.put(canonicalForm, sections);

            Entry<Set<NGram>, Integer[]> prevValue = hash.get(canonicalForm);
            if (prevValue == null) {
              Set<NGram> ngramVariations = new HashSet<NGram>();
              ngramVariations.add(phraseBuffer);
              boolean properPhrase = keys.containsKey(canonicalForm);
              prevValue = new SimpleEntry<Set<NGram>, Integer[]>(ngramVariations, new Integer[] { properPhrase ? 1 : null, properPhrase ? null : 1, 0, 1 });
            } else {
              prevValue.getValue()[3]++;
            }
            hash.put(canonicalForm, prevValue);
          }
        }
      }
    }
    numSections += sectionsToAdd;
    for (Entry<String, Set<Integer>> ngramSection : ngramSections.entrySet()) {
      hash.get(ngramSection.getKey()).getValue()[2] = ngramSection.getValue().size();
    }
    return hash;
  }

  private static final Pattern REFERENCE_PATTERN = Pattern.compile("(R(eferences?|EFERENCES?)|B(ibiliography|IBLIOGRAPHY))");

  /**
   * Fills in the HashMap with statistics of NGrams present in the document. Statistics stored in the Map: 0.) list of positions of the NGrams
   * (List<Integer>)<br>
   * - set of section numbers they are present (Set<Integer>)<br>
   * - number of times they are present in sentences with reference in it (Integer)<br>
   * - list of parse trees (or if no parse tree is available, the list of POS sequences) of the containing sentences<br>
   * - number of times they are included in section headings (Integer)
   * 
   * @param hash
   *          Map to store and update statistics of the NGrams present in the document
   * @param document
   *          document to be investigated
   * @return total number of tokens and sections in the given document
   */
  public int[] getPhrases(Map<String, Map<NGram, NGramStats>> hash, DocumentData document, int maxSection, TreeMap<Integer, List<CoreMap>> sections) {
    Map<String, int[]> acronymsMap = new HashMap<String, int[]>();
    CoreLabel[] buffer = new CoreLabel[maxPhraseLength];
    int pos = 1, lastSectionNumber = sections.lastKey(), referencePosition = Integer.MAX_VALUE;
    for (Entry<Integer, List<CoreMap>> sectionSentences : sections.entrySet()) {
      int sentenceNumber = 0;
      if (sectionSentences.getKey() > maxSection)
        break;
      for (CoreMap sentence : sectionSentences.getValue()) {
        // if the line contains no lower case letters, it might indicate that it is a section header, so it seems
        // to be a good idea not to regard anything occurring in it as an acronym, so rather continue
        boolean lookForAcronyms = !sentence.get(TextAnnotation.class).matches("[^a-z]+");
        boolean containsReference = document.containsReference(sentence);
        int numSeen = 0;
        for (CoreLabel word : sentence.get(TokensAnnotation.class)) {
          // this condition was intended to be useful for the model creation for opinion holder profiling
          // experiments TODO so remove it A.S.A.P.
          // if (word.get(OriginalTextAnnotation.class).equals("Recommended:"))
          // break review;
          if (REFERENCE_PATTERN.matcher(word.word()).matches()) {
            referencePosition = pos;
          }
          String wordForm = word.word();
          boolean isStopWord = word.get(StopWordAnnotation.class);
          if ((!isStopWord && !posRegexp.matcher(word.tag()).matches()) || wordForm.split("\\s").length > 1
              || wordForm.matches("-[LR]RB-|\\p{Punct}+|.*[._:;].*")) {
            numSeen = 0;
            continue;
          }

          for (int i = 0; i < maxPhraseLength - 1; i++) {
            if (buffer[i + 1] != null) {
              buffer[i] = buffer[i + 1];
            }
          }
          pos++;
          buffer[maxPhraseLength - 1] = word;
          numSeen = Math.min(++numSeen, maxPhraseLength);

          // Don't consider phrases that end with a stop word
          if (isStopWord) {
            continue;
          }

          if (lookForAcronyms) {
            updatePossibleAcronyms(wordForm, pos, acronymsMap);
          }

          NGram phraseBuffer = new NGram(maxPhraseLength);
          for (int i = 1; i <= numSeen; i++) {
            CoreLabel actWord = buffer[maxPhraseLength - i];
            phraseBuffer = (NGram) phraseBuffer.clone();
            phraseBuffer.add(0, actWord);

            // Don't consider phrases less than minimal length or beginning with a stop word
            if (i < minPhraseLength || actWord.get(StopWordAnnotation.class))
              continue;

            Map<NGram, NGramStats> orthologicalForms = hash.get(phraseBuffer.getCanonicalForm());
            orthologicalForms = orthologicalForms == null ? new HashMap<NGram, NGramStats>() : orthologicalForms;
            NGramStats info = orthologicalForms.get(phraseBuffer);
            if (info != null) {
              info.updatePositions(pos + 1 - i);
              info.addSentence(new int[] { sectionSentences.getKey(), sentenceNumber });
              info.updateContainsReference((!document.isScientific() || lastSectionNumber == 0 || sectionSentences.getKey() != lastSectionNumber)
                  && containsReference);
            } else if (!document.isScientific() || lastSectionNumber == 0 || sectionSentences.getKey() != lastSectionNumber) {
              info = new NGramStats(pos + 1 - i, new int[] { sectionSentences.getKey(), sentenceNumber }, containsReference);
              orthologicalForms.put(phraseBuffer, info);
              hash.put(phraseBuffer.getCanonicalForm(), orthologicalForms);
            }
          }
        }
        sentenceNumber++;
      }
    }

    // System.err.println(document.getFile());
    if (document.isScientific() && referencePosition == Integer.MAX_VALUE) {
      System.err.println("No refs were found for file " + document.getFile());
    }

    pruneCandidates(hash, referencePosition);
    setAcronymsForDoc(document, referencePosition, acronymsMap);
    return new int[] { pos, sections.size() };
  }

  private void pruneCandidates(Map<String, Map<NGram, NGramStats>> hash, int referencesPosition) {
    Iterator<String> phrases = hash.keySet().iterator();
    while (phrases.hasNext()) {
      String normalizedNGram = phrases.next();
      boolean containsNonAlphabetic = false;
      for (int i = 0; i < normalizedNGram.length(); ++i) {
        char c = normalizedNGram.charAt(i);
        if (!Character.isLetter(c) && c != ' ' && c != '-') {
          containsNonAlphabetic = true;
          break;
        }
      }
      int totalOccurrences = 0, forbiddenPosEnding = 0, containsStopWord = 0, presentInRefs = 0;
      for (Entry<NGram, NGramStats> orthographicForm : hash.get(normalizedNGram).entrySet()) {
        List<Integer> positionsOfOrthographicForm = orthographicForm.getValue().getPositions();
        for (Integer position : positionsOfOrthographicForm) {
          if (position >= referencesPosition) {
            presentInRefs++;
          }
        }

        orthographicForm.getValue().setPresentInReferences(presentInRefs > 0);

        int actualOccurrences = positionsOfOrthographicForm.size();
        totalOccurrences += actualOccurrences;
        boolean hasStopWord = false;
        for (int token = 0; token < orthographicForm.getKey().size(); ++token) {
          if (!hasStopWord && orthographicForm.getKey().get(token).get(StopWordAnnotation.class)) {
            containsStopWord += actualOccurrences;
            hasStopWord = true;
          }
          if (token != orthographicForm.getKey().size() - 1)
            continue;
          String lastTag = orthographicForm.getKey().get(token).tag().toLowerCase();
          if (lastTag.startsWith("vb")) {
            forbiddenPosEnding += actualOccurrences;
          }
        }
      }

      boolean onlyInReferences = presentInRefs == totalOccurrences;
      boolean stopWordOk = noStopWordPruning || containsStopWord != totalOccurrences;
      boolean posOK = noPosEndingPruning || ((double) forbiddenPosEnding / totalOccurrences) < 0.5;
      if (onlyInReferences || !posOK || !stopWordOk || normalizedNGram.length() < 3 || totalOccurrences < minNumOccur || containsNonAlphabetic) {
        // if (onlyInReferences) {
        // System.err.println("'" + normalizedNGram + "' present only in references.");
        // }
        phrases.remove();
      }
    }
  }

  private void setAcronymsForDoc(DocumentData doc, int referenceOffset, Map<String, int[]> map) {
    Map<String, Integer> acronyms = new HashMap<String, Integer>();
    for (Entry<String, int[]> acronym : map.entrySet()) {
      if (acronym.getValue()[0] < referenceOffset) {
        acronyms.put(acronym.getKey(), acronym.getValue()[1]);
      }
    }
    // System.err.println(acronyms);
    doc.setAcronyms(acronyms);
  }

  private void updatePossibleAcronyms(String token, int positionInFile, Map<String, int[]> mapToUpdate) {
    token = token.replaceAll("^\\p{Punct}*|\\p{Punct}*s?\\p{Punct}*$", "").trim();
    int low = 0, up = 0;
    for (int i = 0; i < token.length(); ++i)
      if (Character.isLowerCase(token.charAt(i)))
        low++;
      else if (Character.isUpperCase(token.charAt(i)))
        up++;
      else if (i == 0 && !Character.isLetter(token.charAt(i)))
        break;
    if (up > low) {
      if (token.length() > 1 && token.length() < 10 && !StopWordAnnotator.stopWord.isStopword(token)) {
        int[] value = mapToUpdate.get(token);
        if (value == null)
          mapToUpdate.put(token, new int[] { positionInFile, 1 });
        else
          mapToUpdate.put(token, new int[] { value[0], ++value[1] });
      }
    }
  }

}