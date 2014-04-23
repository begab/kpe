package hu.u_szeged.kpe.main;

import hu.u_szeged.kpe.KpeMain;
import hu.u_szeged.kpe.aspirants.NGram;
import hu.u_szeged.kpe.aspirants.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.kpe.readers.DocumentSet;
import hu.u_szeged.ml.DataMiningException;
import hu.u_szeged.ml.Model;
import hu.u_szeged.ml.mallet.MalletDataHandler;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSelector;
import cc.mallet.types.InfoGain;
import edu.stanford.nlp.util.CoreMap;

/**
 * Class responsible for the training phrase of keyphrase extraction.
 */
public class ExtractionModelBuilder {

  private KPEFilter m_KPEFilter = null;
  /** The maximum length of phrases */
  private int m_MaxPhraseLength = 5;
  /** The minimum length of phrases */
  private int m_MinPhraseLength = 1;
  /** The minimum number of occurrences of a phrase */
  private int m_MinNumOccur = 2;
  /** The reader for the processing of the training data */
  private DocumentSet docSet;

  public void setDocSet(DocumentSet docs) {
    docSet = docs;
  }

  public DocumentSet getDocSet() {
    return docSet;
  }

  /**
   * Get the value of MinNumOccur.
   * 
   * @return Value of MinNumOccur.
   */
  public int getMinNumOccur() {
    return m_MinNumOccur;
  }

  public KPEFilter getKPEFilter() {
    return m_KPEFilter;
  }

  /**
   * Set the value of MinNumOccur.
   * 
   * @param newMinNumOccur
   *          Value to assign to MinNumOccur.
   */
  public void setMinNumOccur(int newMinNumOccur) {
    m_MinNumOccur = newMinNumOccur;
  }

  /**
   * Get the value of MaxPhraseLength.
   * 
   * @return Value of MaxPhraseLength.
   */
  public int getMaxPhraseLength() {
    return m_MaxPhraseLength;
  }

  /**
   * Set the value of MaxPhraseLength.
   * 
   * @param newMaxPhraseLength
   *          Value to assign to MaxPhraseLength.
   */
  public void setMaxPhraseLength(int newMaxPhraseLength) {
    m_MaxPhraseLength = newMaxPhraseLength;
  }

  /**
   * Get the value of MinPhraseLength.
   * 
   * @return Value of MinPhraseLength.
   */
  public int getMinPhraseLength() {
    return m_MinPhraseLength;
  }

  /**
   * Set the value of MinPhraseLength.
   * 
   * @param newMinPhraseLength
   *          Value to assign to MinPhraseLength.
   */
  public void setMinPhraseLength(int newMinPhraseLength) {
    m_MinPhraseLength = newMinPhraseLength;
  }

  public void buildModel(int foldNum, int totalFolds, List<String> features, String classifier, double commonWordsThreshold, double selectedFeatureRatio,
      boolean[] employBIESmarkup, DocumentSet targetDomainDocs, boolean noSWpruning, boolean noPOSpruning, boolean serialize) throws Exception {
    m_KPEFilter = new KPEFilter(noSWpruning, noPOSpruning);
    m_KPEFilter.setMaxPhraseLength(getMaxPhraseLength());
    m_KPEFilter.setMinPhraseLength(getMinPhraseLength());
    m_KPEFilter.setMinNumOccur(getMinNumOccur());
    String[] BIEScompatibleFeatures = { "PosFeature", "StrangeOrthographyFeature", "SuffixFeature" };
    Map<String, Boolean> employBIES = new HashMap<>();
    for (int i = 0; i < employBIESmarkup.length; ++i) {
      employBIES.put(BIEScompatibleFeatures[i], employBIESmarkup[i]);
    }
    m_KPEFilter.setNumFeature(features, classifier, employBIES);
    // m_KPEFilter.setAcceptSynonyms(useSynonymsForTraining);

    List<DocumentData> documents = docSet.determineDocumentSet(foldNum, totalFolds, true, targetDomainDocs);
    m_KPEFilter.setDocsNumber(documents.size());
    int i = 0;
    boolean containsScientific = false;
    for (DocumentData doc : documents) {
      containsScientific = containsScientific || doc.isScientific();
      if (++i % 500 == 0)
        System.err.print(i + "\t");
      m_KPEFilter.updateGlobalDictionary(doc.getKeyphrases(), doc.getSections(docSet.getReader(), serialize));
    }

    if (containsScientific) {
      m_KPEFilter.setCommonWords(commonWordsThreshold, documents.size());
    }

    buildClassifier(foldNum, documents, selectedFeatureRatio, serialize);
    System.err.println("Classifier built of " + documents.size() + " documents in " + ((System.currentTimeMillis() - KpeMain.time) / 1000d) + " seconds.");
  }

  /**
   * Builds the classifier.
   */
  private void buildClassifier(int foldNum, List<DocumentData> docsToLearn, double featureRatio, boolean serialize) {
    m_KPEFilter.initializeFeatureFields();
    System.err.println("Global dictionaries built...\t" + (System.currentTimeMillis() - KpeMain.time) / 1000d);
    Map<String, Object> initClassifier = new HashMap<String, Object>();
    initClassifier.put("classifier", m_KPEFilter.getClassifierName());
    MalletDataHandler dh = new MalletDataHandler();
    try {
      dh.initClassifier(initClassifier);
    } catch (DataMiningException dme) {
      dme.printStackTrace();
    }
    dh.createNewDataset(null);
    int id = 1;
    for (int docNum = 0; docNum < docsToLearn.size(); ++docNum) {
      boolean belongsToTargetDomain = docNum >= docSet.size();
      DocumentData doc = docsToLearn.get(docNum);
      doc.setDocId(id++);
      Map<String, Map<NGram, NGramStats>> hash = new HashMap<String, Map<NGram, NGramStats>>();
      List<int[]> length = new ArrayList<int[]>(1);
      // training is based on separate documents always
      List<Map<Integer, List<CoreMap>>> grammars = new ArrayList<Map<Integer, List<CoreMap>>>(1);
      TreeMap<Integer, List<CoreMap>> sections = doc.getSections(docSet.getReader(), serialize);
      grammars.add(sections);
      length.add(m_KPEFilter.getPhrases(hash, doc, Integer.MAX_VALUE, sections));
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs = new ArrayList<Map<String, Map<NGram, NGramStats>>>(1);
      listOfHashs.add(hash);
      for (Entry<String, Map<NGram, NGramStats>> phrase : hash.entrySet()) {
        m_KPEFilter.addFeats(belongsToTargetDomain, phrase, length, listOfHashs, grammars, dh, true, doc);
      }
    }

    for (Entry<String, Map<Double, Integer>> feats : m_KPEFilter.getFeatures().getFeatureValDistribution().entrySet()) {
      int i = 0, sum = 0;
      if (feats.getKey().contains("TfIdf") && foldNum == 9) {
        for (Entry<Double, Integer> v : feats.getValue().entrySet()) {
          if (++i < 10 || i > 6000)
            System.err.println(i + "\t" + v);
          sum += v.getValue();
        }
        System.err.println(sum + "\t" + i);
      }
    }

    try {
      int instanceNum = dh.getInstanceCount();
      int featureNum = dh.getFeatureCount();
      if (featureRatio < 1.0) {
        FeatureSelector fs = new FeatureSelector(new InfoGain.Factory(), Double.MIN_VALUE);
        fs.selectFeaturesFor(dh.data);
        List<String> selection = new LinkedList<String>();
        BitSet selectedFeatures = dh.data.getFeatureSelection().getBitSet();
        Object[] alphabet = dh.data.getFeatureSelection().getAlphabet().toArray();
        for (int j = 0; j < Math.min(featureNum, featureNum * featureRatio); ++j) {
          boolean selected = selectedFeatures.get(j);
          if (selected) {
            selection.add(alphabet[j].toString());
          }
        }
        // System.err.println("Selected features: " + selection);
        fs.selectFeaturesFor(dh.data);
      }

      // dh.removeFeature(m_KPEFilter.getFeatures().getRareFeatures(1));
      List<Alphabet> alphabets = new LinkedList<Alphabet>();
      alphabets.add(dh.getAlphabet("feature"));
      alphabets.add(dh.getAlphabet("label"));
      m_KPEFilter.setAlphabets(alphabets);
      System.err.println(instanceNum + " inst.\t" + featureNum + " (" + dh.getFeatureCount() + ") features\t" + dh.getFeatureNames().size()
          + " pruned features\t" + (System.currentTimeMillis() - KpeMain.time) / 1000.0d);

      Model learnedModel = dh.trainClassifier();
      m_KPEFilter.setModel(learnedModel);
      // if (m_KPEFilter.getClassifierName().contains("MaxEnt"))
      // ((MalletClassifier) learnedModel).printModel(new PrintWriter(foldNum + "_statistics.txt"),
      // Math.min(dh.getFeatureCount(), 25));
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.err.println((System.currentTimeMillis() - KpeMain.time) / 1000d);
  }
}