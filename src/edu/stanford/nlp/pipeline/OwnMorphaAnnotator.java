package edu.stanford.nlp.pipeline;

import hu.u_szeged.nlp.pos.HungarianMorphology;

import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Timing;

public class OwnMorphaAnnotator extends MorphaAnnotator {

  private Timing timer = new Timing();
  private boolean VERBOSE = false;
  private boolean isEnglish;

  public OwnMorphaAnnotator(boolean english) {
    this(false, english);
  }

  public OwnMorphaAnnotator(boolean verbose, boolean english) {
    super(verbose);
    VERBOSE = verbose;
    isEnglish = english;
  }

  public void annotate(Annotation annotation) {
    if (VERBOSE) {
      timer.start();
      System.err.print("Finding lemma...");
    }
    if (isEnglish) {
      super.annotate(annotation);
    } else {
      if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
        List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
          List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
          HungarianMorphology.recoverTags(tokens);
        }
      } else {
        throw new RuntimeException("unable to find words/tokens in: " + annotation);
      }
    }
  }
}
