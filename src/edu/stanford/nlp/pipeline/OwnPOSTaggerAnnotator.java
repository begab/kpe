package edu.stanford.nlp.pipeline;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.OwnMaxentTagger;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.Timing;
import edu.stanford.nlp.util.concurrent.MulticoreWrapper;
import edu.stanford.nlp.util.concurrent.ThreadsafeProcessor;

/**
 * Wrapper for the maxent part of speech tagger.
 * 
 * @author Anna Rafferty
 */
public class OwnPOSTaggerAnnotator implements Annotator {

  private final OwnMaxentTagger pos;

  private final int maxSentenceLength;

  private final int nThreads;

  /**
   * Create a tagger annotator using the default English tagger from the models jar (and non-verbose initialization).
   */
  public OwnPOSTaggerAnnotator() {
    this(false);
  }

  public OwnPOSTaggerAnnotator(boolean verbose) {
    this(System.getProperty("pos.model", MaxentTagger.DEFAULT_JAR_PATH), verbose);
  }

  public OwnPOSTaggerAnnotator(String posLoc, boolean verbose) {
    this(posLoc, verbose, Integer.MAX_VALUE, 1);
  }

  /**
   * Create a POS tagger annotator.
   * 
   * @param posLoc
   *          Location of POS tagger model (may be file path, classpath resource, or URL
   * @param verbose
   *          Whether to show verbose information on model loading
   * @param maxSentenceLength
   *          Sentences longer than this length will be skipped in processing
   * @param numThreads
   *          The number of threads for the POS tagger annotator to use
   */
  public OwnPOSTaggerAnnotator(String posLoc, boolean verbose, int maxSentenceLength, int numThreads) {
    this(loadModel(posLoc, verbose), maxSentenceLength, numThreads);
  }

  public OwnPOSTaggerAnnotator(OwnMaxentTagger model) {
    this(model, Integer.MAX_VALUE, 1);
  }

  public OwnPOSTaggerAnnotator(OwnMaxentTagger model, int maxSentenceLength, int numThreads) {
    this.pos = model;
    this.maxSentenceLength = maxSentenceLength;
    this.nThreads = numThreads;
  }

  public OwnPOSTaggerAnnotator(String annotatorName, Properties props) {
    boolean verbose = PropertiesUtils.getBool(props, annotatorName + ".verbose", false);
    this.pos = loadModel(annotatorName, verbose);
    this.maxSentenceLength = PropertiesUtils.getInt(props, annotatorName + ".maxlen", Integer.MAX_VALUE);
    this.nThreads = PropertiesUtils.getInt(props, annotatorName + ".nthreads", PropertiesUtils.getInt(props, "nthreads", 1));
  }

  private static OwnMaxentTagger loadModel(String loc, boolean verbose) {
    Timing timer = null;
    if (verbose) {
      timer = new Timing();
      timer.doing("Loading POS Model [" + loc + ']');
    }
    OwnMaxentTagger tagger = new OwnMaxentTagger(loc);
    if (verbose) {
      timer.done();
    }
    return tagger;
  }

  @Override
  public void annotate(Annotation annotation) {
    // turn the annotation into a sentence
    if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
      if (nThreads == 1) {
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
          doOneSentence(sentence);
        }
      } else {
        MulticoreWrapper<CoreMap, CoreMap> wrapper = new MulticoreWrapper<CoreMap, CoreMap>(nThreads, new POSTaggerProcessor());
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
          wrapper.put(sentence);
          while (wrapper.peek()) {
            wrapper.poll();
          }
        }
        wrapper.join();
        while (wrapper.peek()) {
          wrapper.poll();
        }
      }
    } else {
      throw new RuntimeException("unable to find words/tokens in: " + annotation);
    }
  }

  private class POSTaggerProcessor implements ThreadsafeProcessor<CoreMap, CoreMap> {
    @Override
    public CoreMap process(CoreMap sentence) {
      return doOneSentence(sentence);
    }

    @Override
    public ThreadsafeProcessor<CoreMap, CoreMap> newInstance() {
      return this;
    }
  }

  private CoreMap doOneSentence(CoreMap sentence) {
    List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
    if (tokens.size() <= maxSentenceLength) {
      List<TaggedWord> tagged = pos.apply(tokens);

      for (int i = 0, sz = tokens.size(); i < sz; i++) {
        tokens.get(i).set(CoreAnnotations.PartOfSpeechAnnotation.class, tagged.get(i).tag());
      }
    }
    return sentence;
  }

  @Override
  public Set<Requirement> requires() {
    return TOKENIZE_AND_SSPLIT;
  }

  @Override
  public Set<Requirement> requirementsSatisfied() {
    return Collections.singleton(POS_REQUIREMENT);
  }

}