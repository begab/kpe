package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.main.KPEFilter;
import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.utils.NLPUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.NormalizerAnnotator.NormalizerAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * Calculates the average mean and deviation of the sentiment values of the neighboring tokens of a candidate
 * phrase across all of its sentences within the containing document.
 */

public class SentiWordnetFeature extends Feature {

  private static final long serialVersionUID = -2312477566056803372L;

  public SentiWordnetFeature() {
    scale = Scale.NUMERIC;
    collectionToStoreDocVals = LinkedList.class;
  }

  public void setFeatureField(KPEFilter kf) {
    if (KPEFilter.wordList == null) {
      kf.fillWordList("resources/swn/SentiWordNet_3.0.txt");
    }
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {

    List<double[]> sentimentScores = new ArrayList<double[]>(ngramForm.getKey().size());
    boolean ngramSeen = false;
    for (CoreMap sentence : sentences) {
      NGram ngram = new NGram();
      for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
        NGram.getNormalizedCoreLabel(token);
        if (!token.has(NormalizerAnnotation.class)) {
          continue;
        }
        List<double[]> vals = KPEFilter.wordList.get(token);
        double[] avgTokenScore = new double[2];
        if (vals != null) {
          for (double[] d : vals) {
            avgTokenScore = new double[] { avgTokenScore[0] + d[0] / vals.size(), avgTokenScore[1] + d[1] / vals.size() };
          }
        }

        if (ngram.size() == ngramForm.getKey().size()) {
          ngram.remove(0);
          if (!ngramSeen) {
            sentimentScores.remove(0);
          }
        }
        ngram.add(token);
        if (!ngramSeen) {
          sentimentScores.add(avgTokenScore);
        }
        ngramSeen = ngramSeen || ngram.equals(ngramForm.getKey());

        if (avgTokenScore[0] + avgTokenScore[1] >= 0.5) {
          String id = token.tag();
          id = token.getString(NormalizerAnnotation.class) + "_"
              + id.substring(0, Math.min(id.length(), 2)).toLowerCase();
          updateFeatureVals(this.getClass().getName() + "_" + id, 1.0d, docToCheck);
        }
        updateFeatureVals(avgTokenScore[0] + avgTokenScore[1], docToCheck);
      }
      // inserting null indicates the end of a sentence and is needed to compute some stats (such as stdev)
      updateFeatureVals(null, docToCheck);
    }

    for (double[] sentimentScore : sentimentScores) {
      for (double d : sentimentScore) {
        updateFeatureVals(this.getClass().getName() + "_NGRAM", d, docToCheck);
      }
    }
  }

  public Map<String, Double> aggregateVals(boolean train, String token, List<int[]> length, double[] dedicatedFeatures) {

    Map<String, Double> aggregatedVals = new HashMap<String, Double>();

    String className = this.getClass().getName();
    double maximalSentenceMean = 0.0d;
    double maxPositiveTokenScore = 0.0d;
    double maxNegativeTokenScore = 0.0d;
    double maxTotalTokenScore = 0.0d;
    double[] perDocSentenceMeans = null;
    double[] perDocSentenceStDev = null;
    double[] tokenMeanScores = null;
    int sentences = 0;

    for (Entry<String, List<Collection<Number>>> entry : featureVals.entrySet()) {
      int size = entry.getValue().size();
      for (int doc = 0; doc < size; ++doc) {
        Collection<Number> docVals = entry.getValue().get(doc);
        if (entry.getKey().endsWith("Feature")) {
          perDocSentenceMeans = new double[size];
          perDocSentenceStDev = new double[size];
          List<Number> sentenceAvgs = new LinkedList<Number>();
          List<Number> sentence = new LinkedList<Number>();
          for (Number docVal : docVals) {
            if (docVal == null) {
              sentences++;
              sentenceAvgs.add(NLPUtils.mean(sentence));
              sentence = new LinkedList<Number>();
            } else {
              sentence.add(docVal);
            }
          }
          double sentenceAvg = NLPUtils.mean(sentenceAvgs);
          perDocSentenceMeans[doc] = sentenceAvg;
          double summ = 0.0;

          for (Number sentAvg : sentenceAvgs) {
            double sentVal = sentAvg.doubleValue();
            if (sentVal > maximalSentenceMean) {
              maximalSentenceMean = sentVal;
            }
            summ += (sentVal - sentenceAvg) * (sentVal - sentenceAvg);
          }
          perDocSentenceStDev[doc] = Math.sqrt(summ / sentenceAvgs.size());
        } else if (entry.getKey().endsWith("_NGRAM")) {
          tokenMeanScores = new double[size];
          int i = 0;
          double tokenSum = 0.0d, pos = 0.0d, neg = 0.0d, totalSum = 0.0d;
          for (Number docVal : docVals) {
            if (i++ % 2 == 0) {
              pos = docVal.doubleValue();
              totalSum += pos;
              if (pos > maxPositiveTokenScore) {
                maxPositiveTokenScore = pos;
              }
            } else {
              neg = docVal.doubleValue();
              totalSum += neg;
              tokenSum = pos + neg;
              if (neg > maxNegativeTokenScore) {
                maxNegativeTokenScore = neg;
              }

              if (tokenSum > maxTotalTokenScore) {
                maxTotalTokenScore = tokenSum;
              }
            }
          }
          tokenMeanScores[doc] = totalSum / docVals.size();
        } else {
          String key = entry.getKey().substring(entry.getKey().indexOf(className) + className.length()) + "_";
          Double prevVal = aggregatedVals.get(className + key);
          aggregatedVals.put(className + key, (prevVal == null ? 0 : prevVal) + docVals.size());
        }
      }
    }
    aggregatedVals.put(className + "_MAX_SENT_VAL", maximalSentenceMean);
    aggregatedVals.put(className + "_SENT_MEAN", NLPUtils.mean(perDocSentenceMeans));
    aggregatedVals.put(className + "_SENT_STDEV", NLPUtils.mean(perDocSentenceStDev));
    aggregatedVals.put(className + "_PHRASE_MEAN", NLPUtils.mean(tokenMeanScores));
    aggregatedVals.put(className + "_MAX_POS_TOKEN", maxPositiveTokenScore);
    aggregatedVals.put(className + "_MAX_NEG_TOKEN", maxNegativeTokenScore);
    aggregatedVals.put(className + "_MAX_TOKEN", maxTotalTokenScore);
    for (Entry<String, Double> entry : aggregatedVals.entrySet()) {
      if (entry.getKey().endsWith("_")) {
        entry.setValue(entry.getValue() / sentences);
      }
    }
    // reset it so the next time a set of documents are to be keyphrased this flag can start to count from the
    // beginning
    documentToExamine = -1;
    featureVals = new HashMap<String, List<Collection<Number>>>();
    return aggregatedVals;
  }
}
