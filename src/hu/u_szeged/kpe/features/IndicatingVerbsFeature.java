package hu.u_szeged.kpe.features;

import hu.u_szeged.kpe.aspirants.NGram;
import hu.u_szeged.kpe.aspirants.NGramStats;
import hu.u_szeged.kpe.main.KPEFilter;
import hu.u_szeged.kpe.readers.DocumentData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.NormalizerAnnotator.NormalizerAnnotation;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Generics;

/**
 * Calculates the average "distance" in the parse trees of a candidate phrase to the frequent verbs in the corpus.
 */
public class IndicatingVerbsFeature extends NominalFeature {

  private static final long serialVersionUID = -6214178223025494415L;

  public IndicatingVerbsFeature() {
    scale = Scale.NUMERIC;
  }

  public void setFeatureField(KPEFilter kf) {
    nominalVals = kf.determineIndicatingVerbs(20);
  }

  public void value(String phrase, int[] length, Entry<NGram, NGramStats> ngramForm, boolean train, int docToCheck,
      List<Map<String, Map<NGram, NGramStats>>> listOfHashs, List<CoreMap> sentences, DocumentData... docs) {

    Map<String, Integer> indicatorHeights = new HashMap<String, Integer>();
    Map<String, Integer> indicatorOccurrences = new HashMap<String, Integer>();
    // boolean containsEpinionated = false;
    // for (DocumentData doc : docs) {
    // if (doc.getDocumentType() == DocumentType.Epinion) {
    // containsEpinionated = true;
    // break;
    // }
    // }

    for (CoreMap sentence : sentences) {
      List<CoreLabel> sentenceTokens = sentence.get(TokensAnnotation.class);
      Tree sentenceTree = sentence.get(TreeAnnotation.class);
      if (sentenceTree != null) {
        int[] checkedInterval = { Integer.MAX_VALUE, Integer.MIN_VALUE };

        List<Tree> leaves = sentenceTree.getLeaves();
        for (int token = 0; token < sentenceTokens.size(); ++token) {
          CoreLabel cl = sentenceTokens.get(token);
          String word = cl.word();
          // replacing "www." serves the only dummy purpose of not to regard hyperlinks as strange-orthography
          // tokens
          int difference = word.length() - word.replaceAll("(?i)www.", "????").replaceAll("(?i)([a-z])\\1{2,}", "$1").length();
          // boolean strangeOrthography = containsEpinionated && difference > 0 && difference != word.length();
          boolean strangeOrthography = difference > 0 && difference != word.length();
          if (!strangeOrthography && !nominalVals.contains(cl)) {
            continue;
          }
          Tree targetLeaf = leaves.get(token);
          boolean containsNGram = false;
          for (int h = 2; !containsNGram && h <= sentenceTree.depth(targetLeaf); ++h) {
            Tree ancestor = targetLeaf.ancestor(h, sentenceTree);
            List<Tree> ancestorLeaves = ancestor.getLeaves();
            int startIndex = leaves.indexOf(ancestorLeaves.get(0));
            int endIndex = startIndex + ancestorLeaves.size() - ngramForm.getKey().size();
            for (int i = startIndex; i <= endIndex; ++i) {
              if (i >= checkedInterval[0] && i <= checkedInterval[1])
                continue;

              List<CoreLabel> successiveTokens = sentenceTokens.subList(i, i + ngramForm.getKey().size());
              NGram dummyNGram = new NGram(successiveTokens);
              if ((i < token || i > token) && dummyNGram.equals(ngramForm.getKey())) {
                containsNGram = true;
                break;
              }
            }
            checkedInterval = new int[] { startIndex, endIndex };
            if (!containsNGram)
              continue;

            int depth = ancestor.depth();
            depth = h;
            Integer prevDepth = indicatorHeights.get(cl);
            if (prevDepth == null || prevDepth < depth) {
              indicatorHeights.put(cl.get(NormalizerAnnotation.class), depth);
            }

            if (strangeOrthography) {
              GrammaticalStructure depStruct = new EnglishGrammaticalStructure(ancestor);
              Collection<TypedDependency> deps = depStruct.allTypedDependencies();
              Iterator<TypedDependency> depIt = deps.iterator();
              List<TreeGraphNode> seedNodes = new LinkedList<TreeGraphNode>();
              while (depIt.hasNext()) {
                TypedDependency typedDep = depIt.next();
                if (typedDep.dep().label().word().equals(word)) {
                  seedNodes.add(typedDep.gov());
                } else if (typedDep.gov().label().word().equals(word)) {
                  seedNodes.add(typedDep.dep());
                }
              }

              Iterator<TreeGraphNode> nodeIt = seedNodes.iterator();
              while (nodeIt.hasNext()) {
                TreeGraphNode next = nodeIt.next();
                nodeIt.remove();

                Set<TreeGraphNode> newNodes = Generics.newHashSet();
                Set<Tree> nodes = depStruct.root().subTrees();
                for (Iterator<Tree> it = nodes.iterator(); it.hasNext();) {
                  TreeGraphNode node = (TreeGraphNode) it.next();
                  TreeGraphNode gov = GrammaticalStructure.getGovernor(node);
                  if (gov != null && gov == next) {
                    newNodes.add(node);
                  }
                }
                seedNodes.addAll(newNodes);
                nodeIt = seedNodes.iterator();
              }
            }

          }
        }
      } else {
        // TODO implement what should happen for Hungarian language
        for (CoreLabel ew : sentenceTokens) {
          if (nominalVals.contains(ew)) {
            Integer num = indicatorOccurrences.get(ew);
            indicatorOccurrences.put(ew.get(NormalizerAnnotation.class), num == null ? 1 : ++num);
          }
        }
      }
    }

    for (Entry<String, Integer> indicator : indicatorOccurrences.entrySet()) {
      updateFeatureVals(this.getClass().getName() + "_" + indicator.getKey(), indicator.getValue(), docToCheck);
    }

    for (Entry<String, Integer> indicator : indicatorHeights.entrySet()) {
      double d = 1 / (double) indicator.getValue();
      updateFeatureVals(this.getClass().getName() + "_" + indicator.getKey(), d, docToCheck, TreeSet.class);
    }
  }
}
