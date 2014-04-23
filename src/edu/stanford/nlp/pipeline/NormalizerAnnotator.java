package edu.stanford.nlp.pipeline;

import hu.u_szeged.utils.stemmer.PorterStemmer;
import hu.u_szeged.utils.stemmer.Stemmer;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Timing;

public class NormalizerAnnotator implements Annotator {

  private Timing timer;
  private boolean verbose;
  private static final Stemmer stemmer = new PorterStemmer();

  public NormalizerAnnotator() {
    this(false);
  }

  public NormalizerAnnotator(boolean verbose) {
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
        List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
        for (int i = 0; i < tokens.size(); i++) {
          CoreLabel token = tokens.get(i);
          String text = token.get(TextAnnotation.class);
          String lemma = token.get(LemmaAnnotation.class);
          this.addLemma(NormalizerAnnotation.class, token, lemma, text);
        }
      }
    } else {
      throw new RuntimeException("unable to find words/tokens in: " + annotation);
    }

    if (verbose)
      timer.stop("done.");
  }

  private void addLemma(Class<? extends CoreAnnotation<String>> ann, CoreMap map, String lemma, String text) {
    map.set(ann, stemmer.stemString(lemma == null ? text : lemma));
  }

  public static class NormalizerAnnotation implements CoreAnnotation<String> {
    public Class<String> getType() {
      return String.class;
    }
  }

  @Override
  public Set<Requirement> requires() {
    return Collections.unmodifiableSet(new ArraySet<Requirement>());
  }

  @Override
  public Set<Requirement> requirementsSatisfied() {
    return Collections.singleton(TOKENIZE_REQUIREMENT);
  }
}
