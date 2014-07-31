package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.features.Feature.Scale;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.kpe.readers.DocumentSet;
import hu.u_szeged.ml.DataHandler;
import hu.u_szeged.utils.NLPUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import edu.stanford.nlp.util.CoreMap;

public class FeatureHolder extends ArrayList<Feature> {

  /**
   * Class for storing the various Feature object and the mapping to their list of double values.
   */
  private static final long serialVersionUID = 4577522680430595054L;
  private transient Map<String, Map<Double, Integer>> featureValDistribution;

  public FeatureHolder(int initialCapacity) {
    super(initialCapacity);
    featureValDistribution = new HashMap<String, Map<Double, Integer>>();
  }

  public void updateDataHandler(double[] dedicatedFeats, String phrase, String instanceId, List<int[]> length,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<Map<Integer, List<CoreMap>>> grammars,
      DataHandler dataHandler, boolean train, DocumentData... docs) {

    Feature.numberOfDocs = listOfHashs.size();
    double[] ngramAlternations = new double[listOfHashs.size()];
    for (int docToCheck = 0; docToCheck < listOfHashs.size(); ++docToCheck) {
      // DocumentType docType = docs[docToCheck].getDocumentType();
      Map<Integer, List<CoreMap>> documentSentences = grammars.get(docToCheck);
      Map<String, Map<NGram, NGramStats>> docVocabulary = listOfHashs.get(docToCheck);
      Map<NGram, NGramStats> docPhrasesStats = docVocabulary.get(phrase);
      // when keyphrasing multiple documents it is possible that one keyphrase aspirant is not present in some
      // of the documents. Add some
      // dummy feature values in these cases.
      if (docPhrasesStats == null) {
        for (Feature f : this) {
          f.updateFeatureVals(f.getDummyVal(), docToCheck);
        }
        continue;
      }
      ngramAlternations[docToCheck] = docPhrasesStats.size();
      int[] docLength = length.get(docToCheck);
      for (Entry<NGram, NGramStats> ngramModificationOfDoc : docPhrasesStats.entrySet()) {
        List<int[]> sentenceLocations = ngramModificationOfDoc.getValue().getSentencePositions();
        List<CoreMap> sentences = new LinkedList<CoreMap>();
        for (int[] sentenceLocation : sentenceLocations) {
          sentences.add(documentSentences.get(sentenceLocation[0]).get(sentenceLocation[1]));
        }
        for (Feature f : this) {
          f.value(phrase, docLength, ngramModificationOfDoc, train, docToCheck, listOfHashs, sentences, docs);
        }
      }
    }

    double averageAlternations = NLPUtils.mean(ngramAlternations);
    dataHandler.setNumericValue(instanceId, "numOfAppearances", averageAlternations);
    if (DocumentSet.adaptationType % 2 == 1) {
      if (!train || instanceId.matches("target_(\\d+_)+.*")) {
        dataHandler.setNumericValue(instanceId, "T_numOfAppearances", averageAlternations);
      } else {
        dataHandler.setNumericValue(instanceId, "S_numOfAppearances", averageAlternations);
      }
    }

    // set the DataHandler Object from feature to feature
    for (Feature f : this) {
      Map<String, Double> values = f.aggregateVals(train, phrase, length, dedicatedFeats);
      for (Entry<String, Double> val : values.entrySet()) {
        if (Double.isNaN(val.getValue()) || Double.isInfinite(val.getValue())) {
          System.err.println(f + "\n" + phrase + "\n" + val);
          System.exit(2);
        }
        if (f.scale == Scale.BINARY) {
          dataHandler.setBinaryValue(instanceId, val.getKey(), val.getValue() >= 0.5);
          if (DocumentSet.adaptationType % 2 == 1) {
            if (!train || instanceId.matches("target_(\\d+_)+.*")) {
              dataHandler.setBinaryValue(instanceId, "T_" + val.getKey(), val.getValue() >= 0.5);
            } else {
              dataHandler.setBinaryValue(instanceId, "S_" + val.getKey(), val.getValue() >= 0.5);
            }
          }
          if (train) {
            Map<Double, Integer> valsForFeature = featureValDistribution.get(val.getKey());
            valsForFeature = valsForFeature == null ? new TreeMap<Double, Integer>() : valsForFeature;
            featureValDistribution.put(val.getKey(), valsForFeature);
            Integer prevVal = valsForFeature.get(val.getValue() >= 0.5 ? 1.0d : 0.0d);
            valsForFeature.put(val.getValue() >= 0.5 ? 1.0d : 0.0d, prevVal == null ? 1 : ++prevVal);
          }
        } else if (f.scale == Scale.NOMINAL) {
          // TODO implement such cases
          System.out.println("Dealing with nominal attributes in class FeatureHolder is not implemented yet.");
        } else {
          dataHandler.setNumericValue(instanceId, val.getKey(), val.getValue());
          if (DocumentSet.adaptationType % 2 == 1) {
            if (!train || instanceId.matches("target_(\\d+_)+.*")) {
              dataHandler.setNumericValue(instanceId, "T_" + val.getKey(), val.getValue());
            } else {
              dataHandler.setNumericValue(instanceId, "S_" + val.getKey(), val.getValue());
            }
          }
          if (train) {
            Map<Double, Integer> valsForFeature = featureValDistribution.get(val.getKey());
            valsForFeature = valsForFeature == null ? new TreeMap<Double, Integer>() : valsForFeature;
            featureValDistribution.put(val.getKey(), valsForFeature);
            Integer prevVal = valsForFeature.get(val.getValue());
            valsForFeature.put(val.getValue(), prevVal == null ? 1 : ++prevVal);
          }
        }
      }
    }
  }

  public Map<String, Map<Double, Integer>> getFeatureValDistribution() {
    return featureValDistribution;
  }

  public Set<String> getRareFeatures(int threshold) {
    Set<String> unwantedFeatureNames = new HashSet<String>();
    try (PrintWriter out = new PrintWriter("feature.stats")) {
      for (Entry<String, Map<Double, Integer>> featVals : featureValDistribution.entrySet()) {
        out.println(featVals.getKey() + "\t" + featVals.getValue());
        int nonZero = 0;
        for (Entry<Double, Integer> featValOfFeature : featVals.getValue().entrySet()) {
          if (featValOfFeature.getKey() > 0.0d) {
            nonZero += featValOfFeature.getValue();
          }
        }
        if (nonZero <= threshold)
          unwantedFeatureNames.add(featVals.getKey());
      }
    } catch (IOException io) {
      io.printStackTrace();
    }
    return unwantedFeatureNames;
  }
}
