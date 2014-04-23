package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.main.KPEFilter;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.utils.NLPUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import edu.stanford.nlp.util.CoreMap;

/**
 * Feature classes are responsible for various feature calculation.
 */

public abstract class Feature implements Serializable {
  public enum Scale {
    BINARY, NOMINAL, NUMERIC
  }

  private static final long serialVersionUID = 1L;
  protected static boolean binarizeNominals;
  protected boolean canBeRepresentedAsSequential;
  protected boolean employBIESmarkup;
  public static Map<String, Integer[]> dict;
  protected int numDoc;
  public static int numberOfDocs;

  /** its value represents how many documents have been keyphrased (including the one in progress) **/
  protected int documentToExamine = -1;
  /** dummy value is the one that is applied in those situations when a phrase was not present in a document at all **/
  protected double dummyValue = 0.0d;
  protected Class<?> collectionToStoreDocVals = ArrayList.class;
  protected Scale scale = null;
  protected Map<String, List<Collection<Number>>> featureVals = new HashMap<String, List<Collection<Number>>>();
  protected int maxPhraseLength;

  public void setBinarization(boolean needToBinarizeNominals) {
    binarizeNominals = needToBinarizeNominals;
  }

  public abstract void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs);

  public void setFeatureField(KPEFilter kf) {
    dict = kf.getDictionary();
    numDoc = kf.getDocsNumber();
    maxPhraseLength = kf.getMaxPhraseLength();
  }

  public void setEmployBIESmarkup(Boolean newValue) {
    if (canBeRepresentedAsSequential) {
      if (newValue == null) {
        try {
          throw new Exception("Too little BIES parameters were passed.");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      employBIESmarkup = newValue;
    }
  }

  public double getDummyVal() {
    return dummyValue;
  }

  public Map<String, List<Collection<Number>>> getVals() {
    return featureVals;
  }

  protected void updateFeatureVals(Number val, int docToCheck) {
    if (val.intValue() != -1)
      updateFeatureVals(this.getClass().getName(), val, docToCheck, collectionToStoreDocVals);
  }

  protected void updateFeatureVals(String key, Number val, int docToCheck) {
    updateFeatureVals(key, val, docToCheck, collectionToStoreDocVals);
  }

  @SuppressWarnings("unchecked")
  protected void updateFeatureVals(String key, Number val, int docToCheck, Class<?> collectionClass) {
    List<Collection<Number>> docFeatVals = featureVals.get(key);
    if (docFeatVals == null) {
      docFeatVals = new ArrayList<Collection<Number>>(numberOfDocs);
      for (int i = 0; i < numberOfDocs; ++i) {
        try {
          docFeatVals.add((Collection<Number>) collectionClass.newInstance());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      featureVals.put(key, docFeatVals);
    }

    Collection<Number> vals = docFeatVals.get(docToCheck);
    if (vals instanceof Set<?> || vals instanceof LinkedList<?>) {
      vals.add(val == null ? null : val.doubleValue());
    } else if (vals instanceof ArrayList<?>) {
      if (vals.size() > 0) {
        double lastVal = ((Number) ((ArrayList<?>) vals).get(vals.size() - 1)).doubleValue();
        vals.add(val.doubleValue() + lastVal);
      } else {
        vals.add(val);
      }
    } else {
      System.err.println("No such collection was expected.");
    }
    documentToExamine = docToCheck;
  }

  protected double aggregation(List<Collection<Number>> docVals, String phrase, boolean train, List<int[]> length) {
    double[] perDocFeatureVals = new double[docVals.size()];
    for (int doc = 0; doc < docVals.size(); ++doc) {
      Collection<Number> docNums = docVals.get(doc);
      if (docNums instanceof TreeSet<?>) {
        perDocFeatureVals[doc] = ((TreeSet<Number>) docVals.get(doc)).first().doubleValue();
      } else if (docNums instanceof HashSet<?>) {
        perDocFeatureVals[doc] = docNums.contains(1.0d) ? 1.0d : 0.0d;
      } else if (docNums instanceof ArrayList<?>) {
        perDocFeatureVals[doc] = ((ArrayList<Number>) docNums).get(docNums.size() - 1).doubleValue();
      } else if (docNums instanceof LinkedList<?>) {
        perDocFeatureVals[doc] = NLPUtils.mean(docVals.get(doc));
      } else {
        System.err.println("Unexpected collection type " + docNums.getClass() + "\t" + this.getClass());
      }
    }
    return NLPUtils.mean(perDocFeatureVals);
  }

  public Map<String, Double> aggregateVals(boolean train, String token, List<int[]> length, double[] dedicatedFeatures) {
    Map<String, Double> aggregatedVals = new HashMap<String, Double>();
    for (Entry<String, List<Collection<Number>>> entry : featureVals.entrySet()) {
      double featureVal = aggregation(entry.getValue(), token, train, length);
      aggregatedVals.put(entry.getKey(), featureVal);
      if (this instanceof TfIdfFeature)
        dedicatedFeatures[0] = featureVal;
      else if (this instanceof FirstIndexFeature)
        dedicatedFeatures[1] = featureVal;
    }
    // reset it so the next time a set of documents are to be keyphrased this flag can start to count from the
    // beginning
    documentToExamine = -1;
    featureVals = new HashMap<String, List<Collection<Number>>>();
    return aggregatedVals;
  }
}
