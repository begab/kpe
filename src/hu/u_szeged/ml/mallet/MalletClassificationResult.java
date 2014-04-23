package hu.u_szeged.ml.mallet;

import hu.u_szeged.ml.ClassificationResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cc.mallet.classify.Classification;
import cc.mallet.types.LabelVector;

public class MalletClassificationResult extends ClassificationResult {

  protected ArrayList<Classification> prediction;
  protected MalletDataHandler data;

  public MalletClassificationResult(ArrayList<Classification> pred, MalletDataHandler data) {
    super(data);
    this.data = data;
    prediction = pred;
  }

  @SuppressWarnings("unchecked")
  public <T extends Comparable<?>> T getPredictedLabel(String instanceId) {
    return (T) (prediction.get(data.instanceIds.get(instanceId)).getLabeling().getBestLabel().getEntry());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Comparable<?>> Map<T, Double> getPredictionProbabilities(String instanceId) {
    Map<T, Double> res = new TreeMap<T, Double>();
    Integer in = data.instanceIds.get(instanceId);
    LabelVector vec = prediction.get(in).getLabelVector();
    for (int i = 0; i < vec.numLocations(); ++i)
      res.put((T) vec.labelAtLocation(i).getEntry(), vec.valueAtLocation(i));
    return res;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Comparable<?>> List<T> getPredictions() {
    List<T> pred = new LinkedList<T>();
    for (int i = 0; i < getInstanceCount(); ++i)
      pred.add((T) prediction.get(i).getLabeling().getBestLabel().getEntry());
    return pred;
  }

  @Override
  public void loadPredictions(String source) {
  }

  @Override
  public void savePredictions(String target) {
  }

}
