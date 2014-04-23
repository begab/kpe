package hu.u_szeged.kpe.aspirants;

import java.io.Serializable;
import java.util.Comparator;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.NormalizerAnnotator.NormalizerAnnotation;

/**
 * This class is responsible for the proper ordering of ExtendedWord objects, i.e. based on their normalized lemmas.
 */

public class CoreLabelComparator implements Comparator<CoreLabel>, Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Override
  public int compare(CoreLabel labelA, CoreLabel labelB) {
    String baseOfComparisonA = labelA.getString(NormalizerAnnotation.class);
    String baseOfComparisonB = labelB.getString((NormalizerAnnotation.class));
    int comparison = baseOfComparisonA.compareTo(baseOfComparisonB);
    String shortTagA = labelA.tag().toLowerCase().substring(0, Math.min(labelA.tag().length(), 2));
    String shortTagB = labelB.tag().toLowerCase().substring(0, Math.min(labelB.tag().length(), 2));
    return comparison != 0 ? comparison : shortTagA.compareTo(shortTagB);
  }

  public int compareForNGramEquality(CoreLabel labelA, CoreLabel labelB) {
    String baseOfComparisonA = labelA.word();
    String baseOfComparisonB = labelB.word();
    if (baseOfComparisonA.length() > 1)
      baseOfComparisonA = Character.toLowerCase(baseOfComparisonA.charAt(0)) + baseOfComparisonA.substring(1);
    if (baseOfComparisonB.length() > 1)
      baseOfComparisonB = Character.toLowerCase(baseOfComparisonB.charAt(0)) + baseOfComparisonB.substring(1);
    int comparison = baseOfComparisonA.compareTo(baseOfComparisonB);
    String shortTagA = labelA.tag().toLowerCase().substring(0, Math.min(labelA.tag().length(), 2));
    String shortTagB = labelB.tag().toLowerCase().substring(0, Math.min(labelB.tag().length(), 2));
    return comparison != 0 ? comparison : shortTagA.compareTo(shortTagB);
  }
}
