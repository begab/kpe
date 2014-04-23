package hu.u_szeged.ml.mallet;

import hu.u_szeged.ml.Model;

import java.io.PrintWriter;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEnt;

public class MalletClassifier extends Model {

  private static final long serialVersionUID = 75264711522776147L;

  protected Classifier classifier;

  public MalletClassifier(Classifier c) {
    classifier = c;
  }

  public Classifier getClassifier() {
    return classifier;
  }

  public void setClassifier(Classifier classifier) {
    this.classifier = classifier;
  }

  public void printModel(PrintWriter out) {
    printModel(out, Math.min(5, classifier.getAlphabet().size()));
  }

  public void printModel(PrintWriter out, int numOfExtremeFeatures) {
    if (classifier instanceof MaxEnt) {
      out.println(((MaxEnt) classifier).getParameters().length + "\t" + ((MaxEnt) classifier).getDefaultFeatureIndex() + "\t"
          + classifier.getAlphabet().size());
      ((MaxEnt) classifier).printExtremeFeatures(out, Math.min(numOfExtremeFeatures, classifier.getAlphabet().size()));
      out.flush();
    } else
      classifier.print(out);
  }
}
