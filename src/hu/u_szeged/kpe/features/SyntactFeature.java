package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.candidates.NGramStats;
import hu.u_szeged.kpe.readers.DocumentData;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class SyntactFeature extends Feature {

  private static final long serialVersionUID = 6730283683709835856L;

  public SyntactFeature() {
    scale = Scale.NUMERIC;
    collectionToStoreDocVals = LinkedList.class;
  }

  private List<Integer> syntactTree(CoreMap sentence, NGram orthographicForm) {
    List<Integer> toReturn = new LinkedList<Integer>();
    Tree tree = sentence.get(TreeAnnotation.class);
    if (tree == null)
      return toReturn;
    List<Tree> leaves = tree.getLeaves();
    List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
    NGram ngram = new NGram();
    for (int l = 0; l < leaves.size(); ++l) {
      CoreLabel token = tokens.get(l);
      if (ngram.size() == orthographicForm.size()) {
        ngram.remove(0);
      }
      ngram.add(token);
      if (!ngram.equals(orthographicForm))
        continue;
      int heights[] = new int[ngram.size()];
      for (int t = 0; t < ngram.size(); ++t) {
        int subTreeHeight = tree.depth(leaves.get(l - t));
        heights[t] = subTreeHeight;
        for (int h = 2; h < subTreeHeight; ++h) {
          Tree ancestor = leaves.get(l - t).ancestor(h, tree);
          if (ancestor.value().matches("NP.{0,2}")) {
            heights[t] = h - 1;
            break;
          }
        }
      }
      int combinedHeight = 0;
      for (int h = 0; h < heights.length; ++h) {
        combinedHeight = Math.max(combinedHeight, heights[h]);
      }

      toReturn.add(combinedHeight);
    }
    return toReturn;
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {
    int minHeight = Integer.MAX_VALUE;
    for (CoreMap sentence : sentences) {
      List<Integer> heights = syntactTree(sentence, ngramForm.getKey());
      for (int height : heights) {
        // (double)vals[1] to the denominator???
        updateFeatureVals(this.getClass().getName() + "_MEAN", height, docToCheck);
        minHeight = Math.min(minHeight, height);
      }
    }
    updateFeatureVals(this.getClass().getName() + "_MIN", minHeight, docToCheck, TreeSet.class);
  }
}
