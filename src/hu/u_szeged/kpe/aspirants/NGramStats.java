package hu.u_szeged.kpe.aspirants;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class NGramStats {// implements Comparable<NGramStats> {
  private List<Integer> positions;
  private TreeSet<Integer> sectionIds;
  private List<int[]> sentences;
  private int seenAsHeading;
  private int numOfReferences;
  private boolean presentInAbstract;
  private boolean presentInReferences;

  /**
   * 
   * @param position
   * @param positionWithinDoc
   *          stores the section and the order of the sentence the ngram is contained within the section
   * @param hasReference
   * @param heading
   */
  public NGramStats(Integer position, int[] positionWithinDoc, boolean hasReference) {
    positions = new LinkedList<Integer>();
    sectionIds = new TreeSet<Integer>();
    sentences = new LinkedList<int[]>();

    if (position != null)
      positions.add(position);
    if (positionWithinDoc != null) {
      sectionIds.add(positionWithinDoc[0]);
      sentences.add(positionWithinDoc);
    }
    seenAsHeading = positionWithinDoc[1] == 0 ? 1 : 0;
    // the value of presentInAbstract is enough to be set at the very construation, as if even the first presence
    // of an ngram is not in the first paragraph, then later ones will definitely not have the chance for it
    presentInAbstract = positionWithinDoc[0] == 0;
    numOfReferences = hasReference ? 1 : 0;
  }

  public List<Integer> getPositions() {
    return positions;
  }

  public void setPositions(List<Integer> positions) {
    this.positions = positions;
  }

  public void updatePositions(Integer position) {
    this.positions.add(position);
  }

  public TreeSet<Integer> getSectionIds() {
    return sectionIds;
  }

  public void setSectionIds(TreeSet<Integer> sectionIds) {
    this.sectionIds = sectionIds;
  }

  public int numWithReferences() {
    return numOfReferences;
  }

  public void updateContainsReference(boolean containsReference) {
    this.numOfReferences += containsReference ? 1 : 0;
  }

  public List<int[]> getSentencePositions() {
    return sentences;
  }

  public void setSentences(List<int[]> positionWithinDoc) {
    this.sentences = positionWithinDoc;
  }

  public void addSentence(int[] sentencePosition) {
    sentences.add(sentencePosition);
    sectionIds.add(sentencePosition[0]);
    seenAsHeading += sentencePosition[1] == 0 ? 1 : 0;
  }

  public int getSeenAsHeading() {
    return seenAsHeading;
  }

  public void setSeenAsHeading(boolean seenAsHeading) {
    this.seenAsHeading += seenAsHeading ? 1 : 0;
  }

  public boolean isPresentInAbstract() {
    return presentInAbstract;
  }

  public boolean isPresentInReferences() {
    return presentInReferences;
  }

  public void setPresentInReferences(boolean presentInReferences) {
    this.presentInReferences = this.presentInReferences || presentInReferences;
  }

  // public int compareTo(NGramStats other) {
  // if (other == null)
  // return 1;
  // int comparison = new Integer(positions.size()).compareTo(other.getPositions().size());
  // if (comparison != 0)
  // return comparison;
  // comparison = new Integer(sectionIds.size()).compareTo(other.getSectionIds().size());
  // if (comparison != 0)
  // return comparison;
  // return 1;
  // }

}
