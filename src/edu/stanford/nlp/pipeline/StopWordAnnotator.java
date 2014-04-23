package edu.stanford.nlp.pipeline;

import hu.u_szeged.utils.Stopword;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Timing;

public class StopWordAnnotator implements Annotator {

  private Timing timer;
  private boolean verbose;
  public static Stopword stopWord;

  public StopWordAnnotator() {
    this(false);
  }

  public StopWordAnnotator(boolean verbose) {
    stopWord = new Stopword();
    timer = new Timing();
    this.verbose = verbose;
  }

  @Override
  public void annotate(Annotation annotation) {
    if (verbose) {
      timer.start();
      System.err.print("Adding normalized token annotation...");
    }

    if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
      List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
      for (CoreMap sentence : sentences) {
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        for (int i = 0; i < tokens.size(); i++) {
          CoreLabel token = tokens.get(i);
          token.set(StopWordAnnotation.class, stopWord.isStopword(token));
        }
      }
    } else {
      throw new RuntimeException("unable to find words/tokens in: " + annotation);
    }

    if (verbose)
      timer.stop("done.");
  }

  public static class StopWordAnnotation implements CoreAnnotation<Boolean> {
    public Class<Boolean> getType() {
      return Boolean.class;
    }
  }

  @Override
  public Set<Requirement> requires() {
    return Collections.singleton(TOKENIZE_REQUIREMENT);
  }

  @Override
  public Set<Requirement> requirementsSatisfied() {
    return Collections.singleton(TOKENIZE_REQUIREMENT);
  }
}