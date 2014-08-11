package hu.u_szeged.kpe.readers;

import hu.u_szeged.utils.NLPUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringEscapeUtils;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class EpinionReader extends KpeReader {

  private static final String DEFAULT_EXTENSION = ".xml";
  private static final Pattern triggerCodes = Pattern.compile("(?i)JJ[RS]?|NN.{0,2}|VB.?|(ADJ|N|V)P");
  private static Pattern targetWords = Pattern
      .compile("(?i)ha(rd|ve|s)|difficult|like|love|w(ill|o)|comes|miss(es|ing)?|can|may|might|be(en)?|['i]s|are|include(d|s)?|lack(s|ed|ing)?");
  private static Map<String, List<String[]>> opinionAnnotations;

  // public static PrintWriter out;

  protected void setDetails() {
    fileType = DEFAULT_EXTENSION;
    m_encoding = Charset.forName("UTF-8");
  }

  private void setEtalonPhrases(String dir) {
    if (opinionAnnotations != null) {
      return;
    }
    opinionAnnotations = new HashMap<String, List<String[]>>();
    for (File f : new File(dir + "/keyphrases/").listFiles()) {
      for (Entry<String, List<String[]>> annotation : readInNotation(f.getAbsolutePath()).entrySet()) {
        opinionAnnotations.put(annotation.getKey(), annotation.getValue());
      }
    }
  }

  private Map<String, List<String[]>> readInNotation(String file) {
    Map<String, List<String[]>> reviewAnnotations = new HashMap<String, List<String[]>>();
    List<List<String>> lines = NLPUtils.readAsList(file, "\t", Charset.forName("Cp1250"));
    String reviewId = "";
    for (List<String> l : lines) {
      if (l.size() > 1 && l.get(1).startsWith("http://www10.epinions.com/review/")) {
        reviewAnnotations.put((reviewId = l.get(1).replaceAll("http://www10.epinions.com/review/(.*)", "$1")),
            new LinkedList<String[]>());
      } else if (l.size() > 2 && !l.get(1).equals("AUTHOR_OPINIONS:")) {
        reviewAnnotations.get(reviewId).add(new String[] { l.get(2), "" });
        // update with automatic keyphrases if possible
        if (l.size() > 3 && l.get(3).length() > 0) {
          List<String[]> opinions = reviewAnnotations.get(reviewId);
          opinions.get(opinions.size() - 1)[1] = l.get(3);
        }
      }
    }
    return reviewAnnotations;
  }

  private Tree getFirstInterestingSubTree(Tree original, Tree tree) {
    if (tree == null) {
      return null;
    }
    String label = tree.label().toString();
    if (triggerCodes.matcher(label).matches() && !label.matches("VB.?")) {
      return tree;
    } else if (!tree.isLeaf()) {
      Tree originalTree = tree.deepCopy();
      List<Tree> siblings = tree.siblings(original);
      if (siblings != null) {
        for (Tree sibling : siblings) {
          return getFirstInterestingSubTree(originalTree, sibling);
        }
      }
      for (Tree child : originalTree.children()) {
        return getFirstInterestingSubTree(originalTree, child);
      }
    }
    return null;
  }

  private Tree getDependantPhrase(Tree tree) {
    List<Tree> leaves = tree.getLeaves();
    Tree targetNode = null, interestingSubTree = null;
    leaves: for (Tree leaf : leaves) {
      if (targetWords.matcher(leaf.toString()).matches()) {
        targetNode = leaf;
        continue;
      } else if (targetNode == null) {
        continue;
      }
      for (int depth = 1; depth < tree.depth(leaf); ++depth) {
        Tree ancestor = leaf.ancestor(depth, tree);
        if (ancestor.dominates(targetNode)) {
          Tree[] children = ancestor.children();
          for (int c = 0; c < children.length; ++c) {
            List<Tree> childLeaves = children[c].getLeaves();
            for (int lc = 0; lc < childLeaves.size(); ++lc) {
              if (targetWords.matcher(childLeaves.get(lc).toString()).matches()) {
                if (++c < children.length) {
                  if (ancestor.getChild(c).getLeaves().get(0).toString().matches("(?i)n't|not")
                      && c + 1 < children.length) {
                    c++;
                  }
                }
                interestingSubTree = ancestor.getChild(c);
                break leaves;
              }
            }
          }
        }
      }
    }
    return getFirstInterestingSubTree(interestingSubTree, interestingSubTree);
  }

  private Tree getPreviousPhrase(Tree tree, String phraseType) {
    List<Tree> leaves = tree.getLeaves();
    Tree targetNode = null, interestingSubTree = null;
    for (Tree leaf : leaves) {
      if (targetWords.matcher(leaf.toString()).matches()) {
        targetNode = leaf;
        break;
      }
    }
    leaves: for (int l = 0; l < leaves.size(); ++l) {
      Tree leaf = leaves.get(l);
      if (targetWords.matcher(leaf.toString()).matches()) {
        break;
      }
      for (int depth = 1; depth < tree.depth(leaf); ++depth) {
        Tree ancestor = leaf.ancestor(depth, tree);
        if (ancestor.dominates(targetNode)) {
          continue leaves;
        }
        if (ancestor.label().toString().matches(phraseType)) {
          interestingSubTree = ancestor;
        }
      }
    }
    return interestingSubTree;
  }

  private String combinePhrases(Tree parsing) {
    StringBuffer phrase = new StringBuffer();
    Tree preceding = getPreviousPhrase(parsing, "NP");
    Tree following = getDependantPhrase(parsing);
    List<String> leaves = getPosLeaves(preceding);
    for (String goodLeaf : leaves) {
      phrase.append(goodLeaf.toString().toLowerCase() + " ");
    }
    // if (pos.equals("JJ"))
    // containedAdjective = true;

    int offset = 0;
    leaves = getPosLeaves(following);
    for (String goodLeaf : leaves) {
      if (!phrase.toString().contains(goodLeaf.toLowerCase())) {
        phrase.insert(offset, goodLeaf + " ");
        offset += goodLeaf.length() + 1;
      }
    }
    return phrase.toString().trim();
  }

  private List<String> retrieveOpinions(List<String> opinionUnits) {
    List<String> opinions = new LinkedList<String>();
    for (String op : opinionUnits) {
      if (op.length() == 0) {
        continue;
      }
      op = op.replaceAll("(^|\\s+)[^\\p{Alnum}.,\\-]+|[^\\p{Alnum}.,\\-]+(\\s+|$)", " ").replaceAll(" while .*", "")
          .trim();
      if (op.matches(".*[^\\s]/[^\\s][^/]*")) {
        String[] parts = op.split("/");
        String prefix = parts[0].replaceAll("(.*\\s)[^\\s]+", "$1");
        if (!parts[0].contains(" ")) {
          String postfix = parts[1].replaceAll("[^\\s]+(\\s.*)", "$1");
          parts[1] = parts[1].split("\\s")[0];
          if (parts[1].equals(postfix)) {
            opinions.add(prefix);
            opinions.add(postfix);
            continue;
          }
          for (String part : parts) {
            opinions.add(part + postfix);
          }
          continue;
        }
        parts[0] = parts[0].split("\\s")[parts[0].split("\\s").length - 1];
        for (String part : parts) {
          opinions.add(prefix + part);
        }
        continue;
      }
      opinions.add(op);
    }
    return opinions;
  }

  private List<String> getPosLeaves(Tree t) {
    return getPosLeaves(t, triggerCodes);
  }

  private List<String> getPosLeaves(Tree t, String pattern) {
    Pattern p = Pattern.compile(".*");
    try {
      p = Pattern.compile(pattern);
    } catch (PatternSyntaxException e) {
      System.err.println("Malformed regexp.\nNOTE: all leaves will be accepted.");
    }
    return getPosLeaves(t, p);
  }

  private List<String> getPosLeaves(Tree t, Pattern pos) {
    List<String> list = new LinkedList<String>();
    if (t == null) {
      return list;
    }
    List<Tree> leaves = t.getLeaves();
    for (Tree leaf : leaves) {
      if (pos.matcher(leaf.ancestor(1, t).label().toString()).matches()) {
        list.add(leaf.toString());
      }
    }
    return list;
  }

  private List<String> segmentateOpinions(String line) {
    List<String> opinionSegments = new ArrayList<String>(Arrays.asList(line
        .split("\\s+with\\s+|\t|\\s*[,.:;?!&\\-]+(\\s+|$)")));
    segments: for (int i = 0; i < opinionSegments.size(); ++i) {
      String op = opinionSegments.get(i);
      if (op.matches("(?i).+\\s+and\\s+.*")) {
        Annotation document = new Annotation(op);
        sentenceAnalyzer.annotate(document);
        Tree parseTree = document.get(SentencesAnnotation.class).get(0).get(TreeAnnotation.class);
        List<Tree> leaves = parseTree.getLeaves();
        for (Tree leaf : leaves) {
          if (leaf.toString().matches("and|but|because")) {
            opinionSegments.remove(i);
            for (String part : getGeneratedStructures(parseTree, leaf, op)) {
              opinionSegments.add(i++, part);
            }
            continue segments;
          }
        }
      } else if (op.matches("(?i)(and|but|because( of)?)\\s+.*")) {
        opinionSegments.set(i, op.replaceAll("(?i)^(and|but|because( of)?)\\s+", ""));
      } else if (op.matches("(?i).*\\s+(but|because( of)?)\\s+.*")) {
        opinionSegments.remove(i);
        String[] parts = op.split("(?i)\\s+(but|because( of)?)\\s+");
        for (String part : parts) {
          opinionSegments.add(i++, part);
        }
      }
    }
    return opinionSegments;
  }

  private String conCat(Collection<String> container) {
    String result = "";
    for (String element : container) {
      result += element + " ";
    }
    return result.trim();
  }

  private List<String> getGeneratedStructures(Tree parse, Tree leaf, String op) {
    Tree anc = leaf.ancestor(3, parse);
    if (anc.label().toString().equals("ROOT")) {
      return Arrays.asList(op.split(" and "));
    }
    Tree[] children = leaf.ancestor(2, parse).children();
    if (children.length == 1) {
      return Arrays.asList(op.split(" and "));
    } else if (children.length != 3) {
      List<List<String>> leafStrings = new ArrayList<List<String>>(2);
      leafStrings.add(new LinkedList<String>());
      for (Tree child : children) {
        if (child.label().toString().equals("CC") && child.getLeaves().get(0).label().toString().equals("and")) {
          leafStrings.add(new LinkedList<String>());
          continue;
        }
        leafStrings.get(leafStrings.size() - 1).addAll(getPosLeaves(child, ".*"));
      }
      if (leafStrings.size() > 2 || leafStrings.get(0).size() > 0
          || (leafStrings.size() > 1 && leafStrings.get(1).size() > 0)) {
        return Arrays.asList(op.split(" and "));
      }
      String[] leftAndRight = { conCat(leafStrings.get(0)), conCat(leafStrings.get(1)) };
      Tree[] trees = new Tree[2];
      int i = 0;
      for (String side : leftAndRight) {
        Annotation ann = new Annotation(side);
        sentenceAnalyzer.annotate(ann);
        trees[i++] = ann.get(SentencesAnnotation.class).get(0).get(TreeAnnotation.class);
      }
      return produceNewExpressions(parse, trees[0], trees[1], op);
    }
    return produceNewExpressions(parse, children[0], children[2], op);
  }

  private List<String> produceNewExpressions(Tree full, Tree left, Tree right, String op) {
    String[] subTreeLabels = { left.label().toString(), right.label().toString() };
    StringBuffer lText = new StringBuffer(conCat(getPosLeaves(left, ".*")));
    StringBuffer rText = new StringBuffer(conCat(getPosLeaves(right, ".*")));
    StringBuffer beginning = new StringBuffer(op.replaceAll(lText + ".*", ""));
    if (subTreeLabels[0].matches("JJ.?|ADJP") && subTreeLabels[1].matches("N(N.{0,2}|P)")) {
      String[] parts = op.split(" and ");
      parts[0] += " " + conCat(getPosLeaves(right, "NN.{0,2}"));
      return Arrays.asList(parts);
    } else if (subTreeLabels[0].matches("N(N.{0,2}|P)") && subTreeLabels[1].matches("N(N.{0,2}|P)")) {
      return Arrays.asList(op.split(" and "));
    } else if (subTreeLabels[0].matches("V(P|B.{0,2})") && subTreeLabels[1].matches("V(P|B.{0,2})"))
      return Arrays.asList(new String[] { beginning + lText.toString(), beginning + rText.toString() });
    return Arrays.asList(op.split(" and "));
  }

  private String numerateOpinions(String line) {
    StringBuffer opinion = new StringBuffer();
    List<String> opinions = retrieveOpinions(segmentateOpinions(line.replaceAll("\\([^)]+\\)", "")));
    for (String op : opinions) {
      op = op.replaceAll(".* ability to (.*)", "$1").replaceAll("([^\\s])'s ", "$1 's ").replaceAll("^\\s*-", "");
      String generatedPhrase = "";
      if (op.trim().length() == 0) {
        continue;
      }
      Annotation annotatedOp = new Annotation(op);
      sentenceAnalyzer.annotate(annotatedOp);
      for (CoreMap sentence : annotatedOp.get(SentencesAnnotation.class)) {
        Tree parsing = sentence.get(TreeAnnotation.class);
        if (op.split(" ").length == 1) {
          generatedPhrase = op;
        } else if (!op.matches("(?i).* missing calls.*")
            && op
                .matches("(?i)(^|.+ )((difficult|hard) to|like|love|comes with|miss(es|ing)?|w(ill|on't)|ha(ve|s)|can|may|might|be(en)?|(it)?'s|(is|are)(n't)?|include(d|s)?|lack(s|ed|ing)?) .*")) {
          generatedPhrase = combinePhrases(parsing);
        } else if (op.matches("(?i)(.* )?(not )?as .* as .*")) {
          String np = op.replaceAll("(?i)(.* )?(not )?as (.*) as .*", "$1").replaceAll("(?i) not", "").trim();
          String adjp = op.replaceAll("(?i)(.* )?(not )?as (.*) as .*", "$3").trim();
          if (np.equalsIgnoreCase("not")) {
            generatedPhrase = "not " + adjp;
          } else if (op.matches("(?i).*not as.*")) {
            generatedPhrase = "not " + adjp + " " + np;
          } else {
            generatedPhrase = "not " + adjp;
          }
        } else if (op.matches("(?i)\\s*not? [^\\s]+")) {
          generatedPhrase = "not " + op.substring(op.indexOf(" ") + 1);
        } else if (op.split(" ").length > 3) {
          String[] generations = { "", "" }; // first element is for NP, second is for ADJP
          boolean[] foundPhrase = { false, false };
          List<Tree> leaves = parsing.getLeaves();
          for (Tree leaf : leaves) {
            for (int d = 2; d < parsing.depth(leaf) && (!foundPhrase[0] || !foundPhrase[1]); ++d) {
              Tree ancestor = leaf.ancestor(d, parsing);
              if (!foundPhrase[0] && ancestor.label().toString().equals("NP")) {
                for (String goodLeaf : getPosLeaves(ancestor)) {
                  generations[0] += goodLeaf + " ";
                }
                foundPhrase[0] = generations[0].length() > 0;
              } else if (!foundPhrase[1] && ancestor.label().toString().equals("ADJP")) {
                for (String goodLeaf : getPosLeaves(ancestor)) {
                  generations[1] += goodLeaf + " ";
                }
                foundPhrase[1] = generations[1].length() > 0;
              }
            }
          }
          generatedPhrase = generations[0].length() != 0 ? generations[0] : generations[1];
        } else {
          List<Tree> leaves = parsing.getLeaves();
          for (Tree leaf : leaves) {
            if (leaf.ancestor(1, parsing).label().toString().matches("NN.{0,2}|JJ.?|VB.?|TO")) {
              generatedPhrase += leaf + " ";
            }
          }
        }
        generatedPhrase = generatedPhrase.trim();
        if (op
            .matches("(?i)(.* |^)(not?|(wo|will|do(es)?|is|are)(n't| not)|lack(s|ing|ed)?|(c|sh)ould (be|ha(ve|s|d) been)) .*")
            && !generatedPhrase.matches("(?i)not .+")) {
          generatedPhrase = "not " + generatedPhrase.replaceAll("lack(s|ing|ed) ", "");
        }
        // if (generatedPhrase.length() > 0)
        // System.out.println(generatedPhrase + "\t" + op);
        // out.println(op + "\t" + generatedPhrase + "\t" + generatedPhrase);
        opinion.append(generatedPhrase.length() > 0 ? generatedPhrase + "\r\n" : "");
      }
    }
    return opinion.toString();
  }

  public List<DocumentData> getContent(String dir, String file) {
    int reviewNumberInFile = 0;
    setEtalonPhrases(dir);
    List<DocumentData> toReturn = new LinkedList<DocumentData>();
    // if (fileType.equals(".txt") && !file.contains("Readme")){
    // Collection<String> lines = NLPUtils.readDocToCollection(file, new LinkedList<String>());
    // boolean beginningMet = false, title = false;
    // StringBuffer sentences = new StringBuffer();
    // for (String line : lines){
    // title = false;
    // if (line.startsWith("[t]")){
    // beginningMet = true;
    // title = true;
    // if (sentences.length() > 0){
    // toReturn.add(new DocumentData(sentences, "keyphrases", file, toReturn.size(),
    // this.getClass()));
    // }
    // sentences = new StringBuffer();
    // }
    // if (beginningMet){
    // sentences.append(line.substring(line.indexOf(title ? "[t]" : "##") + (title ? 3 : 2)).trim()
    // + "\r\n");
    // }
    // }
    // toReturn.add(new DocumentData(sentences, "keyphrases", file, toReturn.size(),
    // this.getClass()));
    // return toReturn;
    // }
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), getEncoding()))) {
      String line, urlPart = "", reviewId = "", opinionsOfReview = "";
      while ((line = transformLine(br.readLine())) != null) {
        if (line.startsWith("<product name=")) {
          urlPart = line.replaceAll("<product name=\"(.*)\" time=\".*\">", "$1");
        } else if (line.startsWith("<review urlID=")) {
          reviewId = line.replaceAll("<review urlID=\"(.*)\">", "$1");
          List<String[]> opinionList = opinionAnnotations.get(urlPart + "/" + reviewId);
          if (opinionList != null) {
            StringBuffer tempBuffer = new StringBuffer();
            for (String[] op : opinionList) {
              tempBuffer.append(op[goldAnnotation ? 0 : 1] + '\n');
            }
            opinionsOfReview = tempBuffer.toString().trim();
          }
        }

        if (line.equals("</review>")) {
          reviewNumberInFile++;
          if (opinionsOfReview.length() > 0) {
            DocumentData dd = new DocumentData(opinionsOfReview, file, this.getClass());
            dd.setLineNumInFile(reviewNumberInFile);
            toReturn.add(dd);
          }
        }
        // TODO if this were not a pilot project the upcoming part should not be commented
        // else if ((line.equals("<pros>") || line.equals("<cons>"))) {
        // String lineWithOpinions = transformLine(br.readLine());
        // if (lineWithOpinions == null)
        // continue;
        // for (String opinion : numerateOpinions(lineWithOpinions).split("(\\r?\\n)+")) {
        // Annotation annotatedContent = new Annotation(opinion);
        // ngramproc.process(annotatedContent);
        // NGram ng = new NGram(annotatedContent.get(TokensAnnotation.class));
        // Integer value = opinionsOfReview.get(ng);
        // opinionsOfReview.put(ng, value == null ? 1 : ++value);
        // }
        // // out.println("OPINIONS:\t" + lineWithOpinions);
      }
      return toReturn;
    } catch (IOException io) {
      io.printStackTrace();
      return null;
    }
  }

  private String transformLine(String line) {
    if (line == null) {
      return null;
    } else if (line.equals("null")) {
      return "";
    }
    return StringEscapeUtils.unescapeHtml4(line).replaceAll("(?i)\\s+w/o\\s*", " without ")
        .replaceAll("(?i)\\s+w/\\s*", " with ").trim();
  }

  // public TreeMap<Integer, List<CoreMap>> sectionMapping(DocumentData doc) {
  // TreeMap<Integer, List<CoreMap>> documentSections = new TreeMap<Integer, List<CoreMap>>();
  // int sectionNumber = 0;
  // documentSections.put(sectionNumber, new LinkedList<CoreMap>());
  // Annotation docAnnotation = tagAndParse(doc);
  // List<CoreMap> sentences = docAnnotation.get(SentencesAnnotation.class);
  // List<CoreLabel> tokens = docAnnotation.get(TokensAnnotation.class);
  // int tokensPassed = 0, nlOffset = 0;
  // boolean paragraph = false;
  // for (CoreMap sentence : sentences){
  // List<CoreLabel> sentTokens = sentence.get(TokensAnnotation.class);
  // tokensPassed += sentTokens.size();
  //
  // if (sentTokens.size() == 1 && sentTokens.get(0).word().equals("null")){
  // continue;
  // }
  // while (tokensPassed + nlOffset < tokens.size() && tokens.get(tokensPassed +
  // nlOffset).word().equals("*NL*")){
  // nlOffset++;
  // paragraph = true;
  // }
  // documentSections.get(sectionNumber).add(sentence);
  // if (paragraph && documentSections.get(sectionNumber).size() > 0)
  // documentSections.put(++sectionNumber, new LinkedList<CoreMap>());
  // paragraph = false;
  // }
  // return documentSections;
  // }

  @Override
  protected boolean mightBeSectionHeader(String line) {
    return false;
  }

  // public static void main(String[] args) {
  // KpeReader epReader = new EpinionReader();
  // epReader.initGrammar("tokenize, ssplit, cleanxml, pos, lemma, ner");
  // List<DocumentData> docs =
  // epReader.getContent("corpora/phones/Pantech_breEZeTM_C520_Cellular_Phone_reviews.xml");
  // for (DocumentData dd : docs) {
  // // // if (dd.getContent().contains("March, I switched my"))
  // }
  // }

  @Override
  public String getText(String file, int numberWithinFile) {
    // if (fileType.equals(".txt") && !file.contains("Readme")){
    // Collection<String> lines = NLPUtils.readDocToCollection(file, new LinkedList<String>());
    // boolean beginningMet = false, title = false;
    // StringBuffer sentences = new StringBuffer();
    // for (String line : lines){
    // title = false;
    // if (line.startsWith("[t]")){
    // beginningMet = true;
    // title = true;
    // if (sentences.length() > 0){
    // toReturn.add(new DocumentData(sentences, "keyphrases", file, toReturn.size(),
    // this.getClass()));
    // }
    // sentences = new StringBuffer();
    // }
    // if (beginningMet){
    // sentences.append(line.substring(line.indexOf(title ? "[t]" : "##") + (title ? 3 : 2)).trim()
    // + "\r\n");
    // }
    // }
    // toReturn.add(new DocumentData(sentences, "keyphrases", file, toReturn.size(),
    // this.getClass()));
    // return toReturn;
    // }
    StringBuffer review = new StringBuffer();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), getEncoding()))) {
      int reviewNumberInFile = 0;
      boolean needed = false;
      String line;
      while ((line = transformLine(br.readLine())) != null) {
        if (line.startsWith("<review urlID=")) {
          ++reviewNumberInFile;
        }

        if (reviewNumberInFile == numberWithinFile && (line.equals("<title>") || line.equals("<bottomLine>"))) {
          String nextLine = transformLine(br.readLine());
          if (nextLine != null) {
            review.append(nextLine + "\r\n");
          }
        } else if (reviewNumberInFile == numberWithinFile && line.equals("</attributes>")) {
          needed = true;
        } else if (needed && line.equals("</review>")) {
          return review.toString();
        } else if (needed) {
          review.append(line + "\r\n");
        }
      }
    } catch (IOException io) {
      io.printStackTrace();
    }
    return review.toString();
  }
}