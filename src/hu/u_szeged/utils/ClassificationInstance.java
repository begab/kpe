package hu.u_szeged.utils;

import hu.u_szeged.kpe.aspirants.NGram;
import hu.u_szeged.kpe.aspirants.NGram.SequenceType;
import hu.u_szeged.kpe.aspirants.NGramStats;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ClassificationInstance {

  private String id;
  private double tfIdf;
  private double firstOcc;
  private double prob;
  private int rank;
  private Object label;
  private Map<NGram, Integer> orthographicForms;

  /**
   * 
   * @param stringFormat
   */
  public ClassificationInstance(String stringFormat) {
    String[] parts = stringFormat.split("\t");
    id = parts[0];
    tfIdf = Double.parseDouble(parts[1]);
    firstOcc = Double.parseDouble(parts[2]);
    prob = Double.parseDouble(parts[3]);
    rank = Integer.parseInt(parts[4]);
    label = parts[5];
    String[] forms = parts[6].split(", ");
    String[] lemmas = parts[7].split(",");
    orthographicForms = new HashMap<NGram, Integer>();
    for (int f = 0; f < forms.length; ++f) {
      String[] formAndOccurrence = forms[f].replaceAll("^\\{|\\}$", "").split("=");
      int occurrence = Integer.parseInt(formAndOccurrence[1]);
      NGram ng = new NGram(formAndOccurrence[0].split(" "), lemmas[f].split(" "));
      orthographicForms.put(ng, occurrence);
    }
  }

  public ClassificationInstance(String malletId, double[] dedicatedFeatures, Map<NGram, NGramStats> ngramForms) {
    id = malletId.replaceAll(" ", "_");
    tfIdf = dedicatedFeatures[0];
    firstOcc = dedicatedFeatures[1];
    label = new Boolean(dedicatedFeatures[2] == 1.0d);
    orthographicForms = new HashMap<NGram, Integer>();
    for (Entry<NGram, NGramStats> form : ngramForms.entrySet())
      orthographicForms.put(form.getKey(), form.getValue().getPositions().size());
    rank = Integer.MAX_VALUE;
  }

  public int hashCode() {
    return id.hashCode();
  }

  public boolean equals(Object o) {
    return o instanceof ClassificationInstance ? ((ClassificationInstance) o).getId().equals(id) : o.equals(id);
  }

  public double getTfIdf() {
    return tfIdf;
  }

  public double getFirstOccurr() {
    return firstOcc;
  }

  public int getRanking() {
    return rank;
  }

  public void setRanking(int ranking) {
    rank = ranking;
  }

  public double getProbability() {
    return prob;
  }

  public void setProbability(double probability) {
    this.prob = probability;
  }

  public String getId() {
    return id;
  }

  public Object getClassLabel() {
    return label;
  }

  public Map<NGram, Integer> getOrthographicForms() {
    return orthographicForms;
  }

  public String toString() {
    Map<String, Integer> lemmasToFreqs = new HashMap<String, Integer>();
    for (Entry<NGram, Integer> ng : orthographicForms.entrySet()) {
      String lemmaForm = ng.getKey().getSequenceAsString(SequenceType.lemma);
      Integer actualFreq = ng.getValue();
      Integer prevVal = lemmasToFreqs.get(lemmaForm);
      lemmasToFreqs.put(lemmaForm, (prevVal == null ? 0 : prevVal) + actualFreq);
    }
    return id + "\t" + tfIdf + "\t" + firstOcc + "\t" + prob + "\t" + rank + "\t" + label + "\t" + orthographicForms + "\t" + lemmasToFreqs;
  }
}
