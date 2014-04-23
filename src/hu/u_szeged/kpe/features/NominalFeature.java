package hu.u_szeged.kpe.features;


import hu.u_szeged.kpe.main.KPEFilter;

import java.util.Set;


public abstract class NominalFeature extends Feature {

  private static final long serialVersionUID = 5580445755951351866L;
  protected Set<?> nominalVals = null;

  public NominalFeature() {
    scale = Scale.NOMINAL;
  }

  public Set<?> getFeatVals() {
    return nominalVals;
  }

  public abstract void setFeatureField(KPEFilter kf);

}
