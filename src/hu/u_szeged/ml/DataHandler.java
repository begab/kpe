package hu.u_szeged.ml;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DataHandler implements Cloneable {

  // dataset create/save/load
  /**
   * creates a new empty dataset using the underlying native datatype
   */
  abstract public void createNewDataset(Map<String, Object> parameters);

  // /*
  // * returns the underlying native datatype
  // */
  // public Object getNativeDataset();

  public DataHandler createEmptyDataHandler() {
    try{
      return this.getClass().newInstance();
    }catch (InstantiationException e){
      e.printStackTrace();
    }catch (IllegalAccessException e){
      e.printStackTrace();
    }
    return null;
  }

  /**
   * loads a native dataset from the given source
   * 
   * @param source
   *          A String denotes the source of the native dataset it contains a native dataset implementation dependent
   *          resource string
   */
  abstract public void loadDataset(String source) throws DataMiningException;

  /**
   * saves the current dataset to the given target
   * 
   * @param target
   *          A String denotes the target of the native dataset it contains a native dataset implementation dependent
   *          resource string
   */
  abstract public void saveDataset(String target);

  // data manipulation

  /**
   * sets the class label of the given instance
   */
  abstract public <T extends Comparable<?>> void setLabel(String instanceId, T label);

  /**
   * @return the class label f the given instance
   */
  abstract public <T extends Comparable<?>> T getLabel(String instanceId);

  /**
   * Sets the value of a numeric feature
   * 
   * @param instanceId
   *          instance identifier
   * @param featureName
   *          name of the feature
   * @param value
   */
  abstract public void setNumericValue(String instanceId, String featureName, double value);

  /**
   * Sets the value of a nominal feature if this is a new nominal value it is added to the dataset
   * 
   * @param instanceId
   *          instance identifier
   * @param featureName
   *          name of the feature
   * @param value
   */
  abstract public void setNominalValue(String instanceId, String featureName, String value);

  /**
   * Sets the value of a binary feature
   * 
   * @param instanceId
   *          instance identifier
   * @param featureName
   *          name of the feature
   * @param value
   */
  abstract public void setBinaryValue(String instanceId, String featureName, Boolean value);

  abstract public void setBinaryValue(String instanceId, String featureName, Boolean value, boolean ternal);

  /**
   * Sets the value of a feature, the type of the feature is given by the beginning of the feature name b_ binary
   * feature n_ numeric feature m_ nominal feature t_ ternal feature
   * 
   * @param instanceId
   *          instance identifier
   * @param featureName
   *          name of the feature
   * @param value
   */
  abstract public <T extends Comparable<?>> void setValue(String instanceId, String featureName, T value)
      throws DataMiningException;

  abstract public Double getNumericValue(String instanceId, String featureName) throws DataMiningException;

  abstract public String getNominalValue(String instanceId, String featureName) throws DataMiningException;

  abstract public Boolean getBinaryValue(String instanceId, String featureName) throws DataMiningException;

  abstract public <T extends Comparable<?>> T getValue(String instanceId, String featureName)
      throws DataMiningException;

  // instance manipulation
  abstract public void removeInstance(String instanceId) throws DataMiningException;

  abstract public Set<String> getInstanceIds();

  abstract public int getInstanceCount();

  // feature manipulation
  abstract public void removeFeature(String featureName) throws DataMiningException;

  abstract public Set<String> getFeatureNames();

  abstract public int getFeatureCount();

  // The value set of a nominal feature.
  // It returns null for numerical and binary features.
  abstract public List<String> getFeatureValues(String featureName);

  // It throws exception for numerical and binary features.
  abstract public void setDefaultFeatureValue(String featureName, String value) throws DataMiningException;

  // classifier functions
  abstract public void initClassifier(Map<String, Object> parameters) throws DataMiningException;

  abstract public Model trainClassifier() throws DataMiningException;

  abstract public ClassificationResult classifyDataset(Model model) throws DataMiningException;

  /**
   * creates a subset of the dataset where only the given instances and/or features are present
   * 
   * @param instancesSelected
   * @param featuresSelected
   * @return
   */
  abstract public DataHandler createSubset(Collection<String> instancesSelected, Set<String> featuresSelected)
      throws DataMiningException;

  abstract public void addDataHandler(DataHandler dh) throws DataMiningException;
}
