package edu.stanford.nlp.pipeline;

import hu.u_szeged.utils.NLPUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Timing;

public class MweDictAnnotator implements Annotator {

  private Timing timer;
  private boolean VERBOSE;
  private Map<List<String>, Map<String, Integer>> mweFreqs;

  public MweDictAnnotator(String file) {
    this(false, file);
  }

  public MweDictAnnotator(boolean verbose, String file) {
    timer = new Timing();
    this.VERBOSE = verbose;
    if (VERBOSE) {
      timer.start();
      System.err.print("Adding normalized token annotation...");
    }
    mweFreqs = new HashMap<List<String>, Map<String, Integer>>();
    List<String> lines = new LinkedList<String>();
    NLPUtils.readDocToCollection(file, lines);
    for (String line : lines) {
      String[] lineParts = line.split("\t");
      Map<String, Integer> frequencies = new HashMap<String, Integer>();
      for (int col = 1; col < lineParts.length; ++col) {
        frequencies.put(lineParts[col], Integer.parseInt(lineParts[++col]));
      }
      List<String> tokens = new LinkedList<String>();
      for (String token : lineParts[0].split(" ")) {
        tokens.add(token);
      }
      mweFreqs.put(tokens, frequencies);
    }
    if (VERBOSE) {
      System.err.print("MWE dictionary ");
      timer.done();
    }
  }

  private List<String> getLemmatizedListForm(List<CoreLabel> coreLabels) {
    List<String> lemmatized = new ArrayList<String>(coreLabels.size());
    for (int st = 0; st < coreLabels.size(); ++st) {
      if (st == coreLabels.size() - 1)
        lemmatized.add(coreLabels.get(st).getString(LemmaAnnotation.class).toLowerCase());
      else
        lemmatized.add(coreLabels.get(st).word().toLowerCase());
    }
    return lemmatized;
  }

  @Override
  public void annotate(Annotation annotation) {
    if (VERBOSE) {
      timer.start();
      System.err.print("Adding MWE token annotation...");
    }

    if (annotation.has(SentencesAnnotation.class)) {
      List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
      for (CoreMap sentence : sentences) {
        List<CoreLabel> successiveTokens = new ArrayList<CoreLabel>(4);
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        for (int i = 0; i < tokens.size(); i++) {
          CoreLabel token = tokens.get(i);
          token.set(MWEAnnotation.class, "O");
          if (successiveTokens.size() == 4) {
            successiveTokens.remove(0);
          }
          successiveTokens.add(token);
          int size = successiveTokens.size();
          for (int p = 0; size > 1 && p < size - 1; ++p) {
            List<String> lemmatized = getLemmatizedListForm(successiveTokens.subList(p, size));
            Map<String, Integer> freqs = mweFreqs.get(lemmatized);
            if (freqs != null) {
              boolean firstFreq = true;
              for (Entry<String, Integer> freq : freqs.entrySet()) {
                String type = transformType(freq.getKey());
                for (int s = 0; s < lemmatized.size(); s++) {
                  CoreLabel prevToken = tokens.get(i - s);
                  String prevMweLabel = prevToken.get(MWEAnnotation.class).replace("O", "");
                  boolean insertDelimiter = prevMweLabel.length() > 0 && firstFreq;
                  if (s == 0) {
                    prevMweLabel += (prevMweLabel.length() == 0 ? "" : (insertDelimiter ? "@" : "|")) + "E-";
                  } else if (s == lemmatized.size() - 1) {
                    prevMweLabel += (prevMweLabel.length() == 0 ? "" : (insertDelimiter ? "@" : "|")) + "B-";
                  } else {
                    prevMweLabel += (prevMweLabel.length() == 0 ? "" : (insertDelimiter ? "@" : "|")) + "I-";
                  }
                  prevMweLabel += type + "_" + Integer.toString(freq.getValue());
                  tokens.get(i - s).set(MWEAnnotation.class, prevMweLabel);
                }
                firstFreq = false;
              }
            }
          }
        }
      }
    } else {
      throw new RuntimeException("unable to find words/tokens in: " + annotation);
    }

    if (VERBOSE)
      timer.stop("done.");
  }

  private String transformType(String type) {
    if (type.equals("link:")) {
      return "L";
    } else if (type.equals("italic:")) {
      return "I";
    } else if (type.equals("linkItalicBold:")) {
      return "LIB";
    } else if (type.equals("bold:")) {
      return "B";
    } else if (type.equals("linkBold:")) {
      return "LB";
    } else if (type.equals("linkItalic:")) {
      return "LI";
    } else if (type.equals("boldItalic:")) {
      return "IB";
    } else {
      return "N/A";
    }
  }

  public static class MWEAnnotation implements CoreAnnotation<String> {
    public Class<String> getType() {
      return String.class;
    }
  }

  public static class MweEtalonAnnotation implements CoreAnnotation<String> {
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