package hu.u_szeged.ml.mallet;

import hu.u_szeged.ml.ClassificationResult;
import hu.u_szeged.ml.DataHandler;
import hu.u_szeged.ml.DataMiningException;
import hu.u_szeged.ml.Model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEntOptimizableByLabelLikelihood;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureSelection;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.MalletLogger;
import cc.mallet.util.MalletProgressMessageLogger;

public class MalletDataHandler extends DataHandler implements Serializable {

  private static final long serialVersionUID = 7555593484247132956L;
  public InstanceList data;
  public Map<String, Integer> instanceIds;
  protected LabelAlphabet labelAlphabet;
  protected FeatureSequence featureAlphabet;
  // protected FeatureSequence featureSequence;
  protected Map<String, List<String>> nominalValues;
  protected ClassifierTrainer<?> trainer = null;

  static {
    ((MalletLogger) MalletProgressMessageLogger.getLogger(MaxEntOptimizableByLabelLikelihood.class.getName())).getRootLogger().setLevel(Level.WARNING);
  }

  private Instance getInstance(String instanceId) {
    if (!instanceIds.containsKey(instanceId)) {
      Instance inst = new Instance(new AugmentableFeatureVector(featureAlphabet.getAlphabet(), 0, false), labelAlphabet.lookupLabel("false"), instanceId,
          instanceId);
      data.add(inst);
      inst.unLock();
      instanceIds.put(instanceId, data.size() - 1);
    }
    return data.get(instanceIds.get(instanceId));
  }

  public FeatureSequence getFeatureSequence() {
    return featureAlphabet;
  }

  public Alphabet getAlphabet(String type) {
    if (type.equalsIgnoreCase("feature")) {
      return featureAlphabet.getAlphabet();
    } else if (type.equals("label")) {
      return labelAlphabet;
    } else
      return null;
  }

  public AugmentableFeatureVector getInstanceData(String instanceId) {
    return (AugmentableFeatureVector) getInstance(instanceId).getData();
  }

  public double getDoubleValue(String instanceId, String featureName) throws DataMiningException {
    if (!featureAlphabet.getAlphabet().contains(featureName))
      throw new DataMiningException("getter for unexisting feature: " + featureName);
    return getInstanceData(instanceId).value(featureAlphabet.getAlphabet().lookupIndex(featureName));
  }

  protected void setDoubleValue(String instanceId, String featureName, double value) {
    AugmentableFeatureVector fv = getInstanceData(instanceId);
    int index = featureAlphabet.getAlphabet().lookupIndex(featureName);
    if (index < 0) {
      return; // it can occur when featureAlphabet.getStopGrowth()==true and the featureset does not contain the feature
    }
    int location = fv.location(index);
    if (location < 0) {
      fv.add(index, value);
      featureAlphabet.add(index);
    } else {
      fv.setValueAtLocation(location, value);
    }
  }

  public void createNewDataset(Map<String, Object> parameters) {
    Object params = parameters != null ? parameters.get("useFeatureSet") : null;
    if (params != null) {
      if (params instanceof MalletDataHandler) {
        MalletDataHandler dh = (MalletDataHandler) params;
        featureAlphabet = dh.featureAlphabet;
        labelAlphabet = dh.labelAlphabet;
      } else if (params instanceof List<?>) {
        for (Object o : (List<?>) params) {
          if (o instanceof LabelAlphabet) {
            labelAlphabet = (LabelAlphabet) o;
          } else if (o instanceof Alphabet) {
            featureAlphabet = new FeatureSequence((Alphabet) o);
          }
        }
        if (featureAlphabet == null || labelAlphabet == null) {
          System.err.println("Uninitialized alphabet");
          System.exit(1);
        }
        featureAlphabet.getAlphabet().stopGrowth();
        labelAlphabet.stopGrowth();
      } else {
        System.err.println("Unsupported useFeatureSet parameter (neither List, nor a MalletDataHandler.)");
        System.exit(1);
      }
    } else {
      featureAlphabet = new FeatureSequence(new Alphabet());
      labelAlphabet = new LabelAlphabet();
      featureAlphabet.getAlphabet().startGrowth();
      labelAlphabet.startGrowth();
    }

    if (labelAlphabet.size() == 0) {
      @SuppressWarnings("unchecked")
      Set<String> classLabels = parameters == null ? null : (Set<String>) parameters.get("classLabels");
      Set<String> defaultFeatures = new HashSet<String>(Arrays.asList(new String[] { new Boolean(true).toString(), new Boolean(false).toString() }));
      if (classLabels == null) {
        classLabels = defaultFeatures;
        System.err.println("A binary classifier is being built now (which might take some time).");
      }
      for (String label : classLabels) {
        labelAlphabet.lookupIndex(label);
      }
      labelAlphabet.stopGrowth();
    }

    data = new InstanceList(featureAlphabet.getAlphabet(), labelAlphabet);
    instanceIds = new HashMap<String, Integer>();
    nominalValues = new HashMap<String, List<String>>();
  }

  public DataHandler createSubset(Collection<String> instancesSelected, Set<String> featuresSelected) throws DataMiningException {
    MalletDataHandler dh = new MalletDataHandler();
    Map<String, Object> param = new HashMap<String, Object>();
    param.put("useFeatureSet", this);
    dh.createNewDataset(param);
    dh.featureAlphabet = new FeatureSequence(new Alphabet());
    dh.featureAlphabet.getAlphabet().startGrowth();
    dh.data = new InstanceList(dh.featureAlphabet.getAlphabet(), dh.labelAlphabet);
    for (String inst : instancesSelected) {
      AugmentableFeatureVector fv = this.getInstanceData(inst);
      for (int i = 0; i < fv.numLocations(); ++i) {
        String featurename = (String) fv.getAlphabet().lookupObject(fv.getIndices()[i]);
        if (featuresSelected.contains(featurename)) {
          dh.setDoubleValue(inst, featurename, fv.getValues()[i]);
        }
      }
      dh.setLabel(inst, this.getLabel(inst));
    }
    return dh;
  }

  /**
   * This feature creates a subset of the InstanceList that the features coded are not pruned (just the instances).
   * 
   * @param instancesSelected
   * @return
   * @throws DataMiningException
   */
  public DataHandler createSubset(Collection<String> instancesSelected) throws DataMiningException {
    return createSubset(instancesSelected, getFeatureNames());
  }

  public void addDataHandler(DataHandler dh) throws DataMiningException {
    if (!(dh instanceof MalletDataHandler)) {
      throw new DataMiningException("MalletDataHandler can add just MalletDataHandlers");
    }
    for (String inst : dh.getInstanceIds()) {
      AugmentableFeatureVector fv = ((MalletDataHandler) dh).getInstanceData(inst);
      for (int i = 0; i < fv.numLocations(); ++i) {
        this.setDoubleValue(inst, (String) fv.getAlphabet().lookupObject(fv.getIndices()[i]), fv.getValues()[i]);
      }
      this.setLabel(inst, dh.getLabel(inst));
    }
  }

  public Boolean getBinaryValue(String instanceId, String featureName) throws DataMiningException {
    return getDoubleValue(instanceId, featureName) > 0.0;
  }

  public int getFeatureCount() {
    return data.getAlphabet().size();
  }

  public Set<String> getFeatureNames() {
    Set<String> featurenames = new HashSet<String>();
    for (Object o : featureAlphabet.getAlphabet().toArray()) {
      featurenames.add((String) o);
    }
    return featurenames;
  }

  public List<String> getFeatureValues(String featureName) {
    return nominalValues.containsKey(featureName) ? nominalValues.get(featureName) : null;
  }

  public int getInstanceCount() {
    return data.size();
  }

  public Set<String> getInstanceIds() {
    return instanceIds.keySet();
  }

  @SuppressWarnings("unchecked")
  public <T extends Comparable<?>> T getLabel(String instanceId) {
    return (T) ((Label) getInstance(instanceId).getTarget()).getEntry();
  }

  public String getNominalValue(String instanceId, String featureName) throws DataMiningException {
    if (!nominalValues.containsKey(featureName))
      throw new DataMiningException(featureName + " is not a nominal feature");
    return nominalValues.get(featureName).get((int) getDoubleValue(instanceId, featureName));
  }

  public Double getNumericValue(String instanceId, String featureName) throws DataMiningException {
    return getDoubleValue(instanceId, featureName);
  }

  @SuppressWarnings("unchecked")
  public <T extends Comparable<?>> T getValue(String instanceId, String featureName) throws DataMiningException {
    return (T) (Double) getDoubleValue(instanceId, featureName);
  }

  public void initClassifier(Map<String, Object> parameters) throws DataMiningException {
    String classifierName = "MaxEntL1";
    // String classifierName = "C45";
    Double gaussianPrior = null;
    if (parameters != null && parameters.containsKey("classifier")) {
      classifierName = (String) parameters.get("classifier");
    }
    if (classifierName.equals("MaxEntL1") && parameters != null && parameters.containsKey("classifier")) {
      gaussianPrior = (Double) parameters.get("gaussianPrior");
    }
    try {
      trainer = (ClassifierTrainer<?>) Class.forName("cc.mallet.classify." + classifierName + "Trainer").newInstance();
      if (classifierName.equals("MaxEntL1")) {
        if (gaussianPrior != null) {
          ((MaxEntTrainer) trainer).setGaussianPriorVariance(gaussianPrior);
        }
      }
      // ((MaxEntTrainer)trainer).setGaussianPriorVariance(1.0);
      /*
       * ((C45Trainer)trainer).setMinNumInsts(3); ((C45Trainer)trainer).setDepthLimited(true); ((C45Trainer)trainer).setMaxDepth(2);
       * ((C45Trainer)trainer).setDoPruning(true);
       */
    } catch (Exception e) {
      throw new DataMiningException("unknown classifier: " + classifierName, e);
    }
  }

  public Model trainClassifier() throws DataMiningException {
    if (trainer == null) {
      initClassifier(null);
    }
    return new MalletClassifier(trainer.train(data));
  }

  public ClassificationResult classifyDataset(Model model) throws DataMiningException {
    if (!(model instanceof MalletClassifier)) {
      throw new DataMiningException("MalletDataHandler can be used only by MALLET classifiers");
    }
    return new MalletClassificationResult(((MalletClassifier) model).getClassifier().classify(data), this);
  }

  public void removeFeature(String featureName) throws DataMiningException {
    this.removeFeature(new String[] { featureName });
  }

  public void removeFeature(String... featureNames) throws DataMiningException {
    removeFeature(new HashSet<String>(Arrays.asList(featureNames)));
  }

  public void removeFeature(Set<String> featureNames) throws DataMiningException {
    Alphabet alphabet = featureAlphabet.getAlphabet();
    FeatureSelection fs = new FeatureSelection(alphabet);
    double[] counts = new double[alphabet.size()];
    for (int feat = 0; feat < counts.length; ++feat) {
      Object featureName = featureAlphabet.getAlphabet().lookupObject(feat);
      counts[feat] = featureNames.contains(featureName) ? 0 : 1;
    }
    Alphabet reducedAlphabet = new Alphabet();
    featureAlphabet.prune(counts, reducedAlphabet, 1);
    Iterator<?> it = reducedAlphabet.iterator();
    while (it.hasNext()) {
      fs.add(it.next());
    }
    data.setFeatureSelection(fs);
  }

  // public Map<String, Map<String, Integer>> removeFeature(int threshold) throws DataMiningException {
  // Map<String, Map<String, Integer>> stats = new TreeMap<String, Map<String, Integer>>();
  // stats.put("classLabel", new HashMap<String, Integer>());
  // double[] counts = new double[featureAlphabet.getAlphabet().size()];
  // for (Entry<String, Integer> instance : instanceIds.entrySet()){
  // Instance inst = data.get(instance.getValue());
  // AugmentableFeatureVector fv = (AugmentableFeatureVector) inst.getData();
  // for (int feat = 0; feat < counts.length; ++feat){
  // String featureName = featureAlphabet.getAlphabet().lookupObject(feat).toString();
  // Map<String, Integer> vals = stats.get(featureName);
  // vals = vals == null ? new HashMap<String, Integer>() : vals;
  // stats.put(featureName, vals);
  // String val;
  // try{
  // // in case the feature was nominal one
  // val = getNominalValue(instance.getKey(), featureName);
  // counts[feat] = threshold;
  // }catch (Exception e){
  // double value = fv.value(feat);
  // if (value > 0.0d)
  // counts[feat]++;
  // val = Double.toString(value);
  // }
  // Integer i = vals.get(val);
  // if (i == null){
  // vals.put(val, 1);
  // }else{
  // vals.put(val, ++i);
  // }
  // }
  // Map<String, Integer> val = stats.get("classLabel");
  // String label = ((Label) inst.getTarget()).getEntry().toString();
  // Integer v = val.get(label);
  // val.put(label, (v == null ? 1 : ++v));
  // stats.put("classLabel", val);
  // }
  // FeatureSelection fs = new FeatureSelection(featureAlphabet.getAlphabet());
  // Alphabet reducedAlphabet = new Alphabet();
  // featureAlphabet.prune(counts, reducedAlphabet, threshold);
  // Iterator<?> it = reducedAlphabet.iterator();
  // while (it.hasNext()){
  // fs.add(it.next());
  // }
  // data.setFeatureSelection(fs);
  // return stats;
  // }

  public void removeInstance(String instanceId) {
    Integer number = instanceIds.remove(instanceId);
    if (number == null) {
      return;
    }
    data.remove((int) number);
    for (Entry<String, Integer> indexPair : instanceIds.entrySet()) {
      if (indexPair.getValue() > number) {
        indexPair.setValue(indexPair.getValue() - 1);
      }
    }
    // throw new DataMiningException("removeFeature is not implemented yet in MalletDataHandler");
  }

  public void removeInstances(Collection<String> instanceIdsToRemove) {
    Iterator<Instance> instanceIt = data.iterator();
    while (instanceIt.hasNext()) {
      Instance next = instanceIt.next();
      if (instanceIdsToRemove.contains(next.getName())) {
        data.remove(next);
        instanceIt = data.iterator();
        Integer prevVal = instanceIds.remove(next.getName());
        if (prevVal != null) {
          for (Entry<String, Integer> indexPair : instanceIds.entrySet()) {
            if (indexPair.getValue() > prevVal) {
              indexPair.setValue(indexPair.getValue() - 1);
            }
          }
        }
      }
    }
  }

  public void loadDataset(String source) throws DataMiningException {
    try (BufferedReader file = new BufferedReader(new FileReader(source))) {
      createNewDataset(null);
      String line;
      while ((line = file.readLine()) != null) {
        String[] tokens = line.split("\\s");
        if (!tokens[0].contains("@")) {
          throw new DataMiningException("Corrput input format. First token should contain @");
        }
        String id = tokens[0].split("@")[0];
        setLabel(id, tokens[0].split("@")[1]);
        for (int i = 1; i < tokens.length; ++i) {
          setDoubleValue(id, tokens[i].split(":")[0], Double.parseDouble(tokens[i].split(":")[1]));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void saveDataset(String target) {
    if (!target.contains("|") || target.split("\\|")[1].equals("mallet")) {
      saveDatasetMallet(target.split("\\|")[0]);
    } else if (target.split("\\|")[1].equals("svm")) {
      saveDatasetSVM(target.split("\\|")[0]);
    } else if (target.split("\\|")[1].equals("weka")) {
      saveDatasetWeka(target.split("\\|")[0]);
    } else {
      System.err.println("unknow output format " + target.split("\\|")[1]);
    }
  }

  public void saveDatasetMallet(String target) {
    try {
      PrintWriter out = new PrintWriter(target);
      for (String id : instanceIds.keySet()) {
        out.print(id + "@" + getLabel(id));
        AugmentableFeatureVector fv = getInstanceData(id);
        for (int i = 0; i < fv.numLocations(); ++i) {
          out.print("\t" + featureAlphabet.getAlphabet().lookupObject(fv.getIndices()[i]) + ":" + fv.getValues()[i]);
        }
        out.println();
      }
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public void saveDatasetSVM(String target) {
    try {
      PrintWriter out = new PrintWriter(target);
      for (String id : instanceIds.keySet()) {
        out.print(getLabel(id) ? "1" : "-1");
        AugmentableFeatureVector fv = getInstanceData(id);
        for (int i = 0; i < fv.numLocations(); ++i) {
          out.print(" " + (fv.getIndices()[i] + 1) + ":" + fv.getValues()[i]);
        }
        out.println();
      }
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public void saveDatasetWeka(String target) {
    try {
      if (!target.endsWith(".arff")) {
        target += ".arff";
      }
      PrintWriter out = new PrintWriter(target);
      out.println("@relation MalletData");
      for (Object f : data.getAlphabet().toArray()) {
        String name = f.toString().replaceAll("'", "");
        out.println("@attribute '" + name + "' numeric");
      }
      out.println("@attribute classlabel {0,1}");
      out.println("@data");
      for (String id : instanceIds.keySet()) {
        out.print("{");
        AugmentableFeatureVector fv = getInstanceData(id);
        for (int i = 0; i < fv.numLocations(); ++i) {
          if (i > 0) {
            out.print(",");
          }
          out.print((fv.getIndices()[i]) + " " + fv.getValues()[i]);
        }
        if (getLabel(id)) {
          out.print("," + data.getAlphabet().size() + " 1");
        }
        out.println("}");
      }
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public void setBinaryValue(String instanceId, String featureName, Boolean value) {
    setDoubleValue(instanceId, featureName, value ? 1.0 : 0.0);
  }

  public void setBinaryValue(String instanceId, String featureName, Boolean value, boolean ternal) {
    System.err.println("ternal isn't implemented in MalletDataHandler");
  }

  public void setDefaultFeatureValue(String featureName, String value) throws DataMiningException {
    if (!nominalValues.containsKey(featureName)) {
      throw new DataMiningException("setDefaultFeatureValue is called for a feature which is not nominal");
    }
    if (nominalValues.get(featureName).contains(value)) {
      nominalValues.get(featureName).remove(value);
    }
    nominalValues.get(featureName).add(0, value);
  }

  public <T extends Comparable<?>> void setLabel(String instanceId, T label) {
    getInstance(instanceId).setTarget(labelAlphabet.lookupLabel(label.toString()));
  }

  public void setNominalValue(String instanceId, String featureName, String value) {
    if (!nominalValues.containsKey(featureName)) {
      nominalValues.put(featureName, new LinkedList<String>());
      nominalValues.get(featureName).add("MISSINGVALUE");
    }
    int pos = nominalValues.get(featureName).indexOf(value);
    if (pos < 0) {
      nominalValues.get(featureName).add(value);
      pos = nominalValues.get(featureName).size() - 1;
    }
    setDoubleValue(instanceId, featureName, (double) pos);
  }

  public void setNumericValue(String instanceId, String featureName, double value) {
    setDoubleValue(instanceId, featureName, value);
  }

  protected MalletDataHandler clone() {
    MalletDataHandler dh = new MalletDataHandler();
    dh.createNewDataset(null);
    try {
      dh.addDataHandler(this);
    } catch (DataMiningException e) {
      e.printStackTrace();
    }
    return dh;
  }

  public <T extends Comparable<?>> void setValue(String instanceId, String featureName, T value) throws DataMiningException {
    if (featureName.startsWith("b_")) {
      setBinaryValue(instanceId, featureName, (Boolean) value);
    } else if (featureName.startsWith("t_")) {
      setBinaryValue(instanceId, featureName, (Boolean) value, true);
    } else if (featureName.startsWith("m_")) {
      setNominalValue(instanceId, featureName, (String) value);
    } else if (featureName.startsWith("m_")) {
      setNumericValue(instanceId, featureName, (Double) value);
    } else {
      throw new DataMiningException("unknown featuretype " + featureName);
    }
  }
}
