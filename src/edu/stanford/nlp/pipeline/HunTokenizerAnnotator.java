package edu.stanford.nlp.pipeline;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.HunTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Timing;

/**
 * This class will PTB tokenize the input string. There's at present an old and a new version (both are tried). The old version looks for an
 * original String or List&lt;String&gt; is under the Annotation.OriginalStringPLAnnotation and it will add the output from the
 * InvertiblePTBTokenizer (List&lt;CoreLabel&gt;) under Annotation.WordsPLAnnotation and it will make a copy, as a List&lt;CoreLabel&gt;
 * which it will put under Annotation.OriginalWordsPLAnnotation. The new version assumes that the original String or List&lt;String&gt; is
 * under the Annotation.TextAnnotation field and it will add the output from the InvertiblePTBTokenizer (List&lt;CoreLabel&gt;) under
 * Annotation.TokensAnnotation. If the original input was a List, then it will make a List&lt;List&lt;CoreLabel&gt;&gt;, and otherwise it
 * will make a List&lt;CoreLabel&lt; for both of these entries. The reason why there are copies is so that future Annotators can muck around
 * with the copy under WORDS_KEY, but can still access the original words if necessary.
 * 
 * @author Jenny Finkel
 */
public class HunTokenizerAnnotator implements Annotator {

  private TokenizerFactory<CoreLabel> factory = HunTokenizer.factory(false, true);
  private Timing timer = new Timing();

  private boolean VERBOSE = true;

  private String options = "invertible,ptb3Escaping=true";

  public HunTokenizerAnnotator() {
    this(true);
  }

  public HunTokenizerAnnotator(boolean verbose) {
    this(verbose, "invertible,ptb3Escaping=true");
  }

  public HunTokenizerAnnotator(String options) {
    this(true, options);
  }

  public HunTokenizerAnnotator(boolean verbose, String options) {
    this.VERBOSE = verbose;
    this.options = options;
    factory = HunTokenizer.factory(new CoreLabelTokenFactory(), this.options);
  }

  public void annotate(Annotation annotation) {
    if (VERBOSE) {
      timer.start();
      System.err.print("Hun tokenizing ... ");
    }

    if (annotation.has(CoreAnnotations.TextAnnotation.class)) {
      String text = annotation.get(CoreAnnotations.TextAnnotation.class);
      Reader r = new StringReader(text); // don't wrap in BufferedReader. It gives you nothing for in memory String
                                         // unless you need the readLine() method!
      List<CoreLabel> tokens = this.factory.getTokenizer(r).tokenize();
      // cdm 2010-05-15: This is now unnecessary, as it is done in CoreLabelTokenFactory
      // for (CoreLabel token: tokens) {
      // token.set(CoreAnnotations.TextAnnotation.class, token.get(TextAnnotation.class));
      // }
      annotation.set(CoreAnnotations.TokensAnnotation.class, tokens);
      if (VERBOSE) {
        timer.stop("done.");
        System.err.println("output: " + annotation.get(CoreAnnotations.TokensAnnotation.class) + "\n");
      }
    } else {
      throw new RuntimeException("unable to find text in annotation: " + annotation);
    }
  }

  public Pair<List<CoreLabel>, List<CoreLabel>> doOneSentence(String origText) {
    Reader r = new StringReader(origText);
    List<CoreLabel> words = factory.getTokenizer(r).tokenize();
    List<CoreLabel> wordsCopy = new ArrayList<CoreLabel>();
    for (CoreLabel w : words) {
      CoreLabel fl = new CoreLabel(w);
      wordsCopy.add(fl);
    }
    return new Pair<List<CoreLabel>, List<CoreLabel>>(words, wordsCopy);
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
