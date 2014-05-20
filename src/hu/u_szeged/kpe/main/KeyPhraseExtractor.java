package hu.u_szeged.kpe.main;

import hu.u_szeged.kpe.readers.DocumentData;
import hu.u_szeged.kpe.readers.DocumentSet;
import hu.u_szeged.utils.ClassificationInstance;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The class that executes keyphrase extraction itself.
 */
public class KeyPhraseExtractor {

  private KPEFilter m_KPEFilter = null;
  private boolean m_prune;
  /** The number of phrases to extract. */
  private int m_numPhrases = 10;
  /** The m_reader for the processing of the test data */
  private DocumentSet docSet;

  /**
   * Flag indicating whether potentially synonymous words should be treated specially when determining class labels
   */

  public KeyPhraseExtractor() {
  }

  public void setDocSet(DocumentSet docs) {
    docSet = docs;
  }

  public DocumentSet getDocSet() {
    return docSet;
  }

  /**
   * Get the value of numPhrases.
   * 
   * @return Value of numPhrases.
   */
  public int getNumPhrases() {
    return m_numPhrases;
  }

  /**
   * Set the value of numPhrases.
   * 
   * @param newnumPhrases
   *          Value to assign to numPhrases.
   */
  public void setNumPhrases(int newnumPhrases) {
    m_numPhrases = newnumPhrases;
  }

  public void setPrune(boolean prune) {
    m_prune = prune;
  }

  public boolean getPruning() {
    return m_prune;
  }

  public void loadModel(String fileName) throws Exception {
    ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fileName)));
    m_KPEFilter = (KPEFilter) in.readObject();
    in.close();
  }

  public void setKPEFilter(KPEFilter kf) {
    m_KPEFilter = kf;
  }

  public KPEFilter getKpeFilter() {
    return m_KPEFilter;
  }

  /**
   * @param out
   * @param prunedPhrases
   * @param doc
   */
  private String writeOutKeyphrases(PrintWriter out, List<int[]> length, List<ClassificationInstance> prunedPhrases, DocumentData doc) {
    out.print("\n\n----------" + doc.getFile() + "_" + doc.getLineNumInFile() + "----------");
    if (length != null && length.size() == 1) {
      int[] l = length.get(0);
      out.print("\t" + l[0] + "\t" + l[1]);
    }

    StringBuffer keyphrases = new StringBuffer();

    for (int i = 0; i < prunedPhrases.size(); ++i) {
      ClassificationInstance inst = prunedPhrases.get(i);
      out.print("\n" + inst);
      if (i < 15) {
        keyphrases.append(inst.getProbableForm() + (i < 14 ? "," : ""));
      }
    }
    out.flush();
    return keyphrases.toString();
  }

  public void extractKeyphrases(int fold, int totalFolds, String fileName, boolean serialize) throws Exception {
    Collection<DocumentData> documents = docSet.determineDocumentSet(fold, totalFolds, false);
    System.err.println("Determining keyphrases of the test set of " + documents.size() + " documents...");
    // m_KPEFilter.setAcceptSynonyms(acceptSynonymsForTesting);
    m_KPEFilter.initializeFeatureFields();

    PrintWriter out = new PrintWriter(fileName);
    System.err.println("output file: " + fileName);

    for (DocumentData doc : documents) {
      List<int[]> length = new ArrayList<>(1);
      List<ClassificationInstance> rankedPhrases = m_KPEFilter.rankDocumentInstances(docSet.getReader(), length, serialize, doc);

      String response = writeOutKeyphrases(out, length, rankedPhrases.subList(0, Math.min(m_numPhrases, rankedPhrases.size())), doc);
      out.println("\nEtalon set: " + doc.getKeyphrases().keySet());
      out.println(doc.getFile().replaceAll(".*([CIJH]-\\d+).*", "$1") + (doc.getLineNumInFile() == 0 ? "" : doc.getLineNumInFile()) + " : " + response);
    }
    out.close();
  }
}