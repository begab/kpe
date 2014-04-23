package hu.u_szeged.ml;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ClassicifationResult {
	protected DataHandler dataHandler;
	
  public ClassicifationResult(DataHandler dataHandler){
		this.dataHandler = dataHandler;
	}
	
  public <T extends Comparable<?>> T getLabel(String instanceId){
		return dataHandler.getLabel(instanceId);
	}
	
  public int getInstanceCount(){
		return dataHandler.getInstanceCount();
	}
	
	public Set<String> getInstanceIds(){
		return dataHandler.getInstanceIds();
	}
	
	public abstract <T extends Comparable<?>> T getPredictedLabel(String instanceId);
	
	public abstract <T extends Comparable<?>> List<T> getPredictions();
	
	public abstract Map<String,Double> getPredictionProbabilities(String instanceId);
	
	public abstract void savePredictions(String target);
	
	public abstract void loadPredictions(String source);
}
