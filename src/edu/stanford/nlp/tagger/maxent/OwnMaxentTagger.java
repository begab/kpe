package edu.stanford.nlp.tagger.maxent;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;

public class OwnMaxentTagger extends MaxentTagger {

  /**
   * 
   */
  private static final long serialVersionUID = 3836391520167535831L;

  public OwnMaxentTagger(String modelFile) {
    super(modelFile, new TaggerConfig("-model", modelFile));
    this.setVerbose(false);
  }

  public OwnMaxentTagger(String modelFile, TaggerConfig config) {
    super(modelFile, config);
  }

  public ArrayList<TaggedWord> apply(List<? extends HasWord> in) {
    TestSentence testSentence = new OwnTestSentence(this);
    return testSentence.tagSentence(in, false);
  }

  public void setVerbose(boolean verbose) {
    super.VERBOSE = verbose;
    super.config.setProperty("verbose", verbose ? "true" : "false");
  }

}
