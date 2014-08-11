package hu.u_szeged.kpe.readers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public class DocumentSet extends TreeSet<DocumentData> {

  private static final long serialVersionUID = -5225565570903384980L;
  private String baseDir;
  private KpeReader reader;
  public static int adaptationType;
  private static boolean isGenuineDAon;

  public DocumentSet(KpeReader r) {
    this(-1, "", r);
  }

  public DocumentSet(int adaptation, KpeReader r) {
    this(adaptation, "", r);
  }

  public DocumentSet(String bd, KpeReader r) {
    this(-1, bd, r);
  }

  public DocumentSet(int adaptation, String bd, KpeReader r) {
    adaptationType = adaptation;
    baseDir = bd;
    reader = r;
  }

  public void setBaseDir(String bd) {
    baseDir = bd;
  }

  public KpeReader getReader() {
    return reader;
  }

  public Collection<DocumentData> determineDocumentSet(int foldNum, int totalFolds, boolean forTraining) {
    if (adaptationType == -1 || !forTraining) {
      return determineDocumentSet(foldNum, totalFolds, forTraining, null);
    } else {
      try {
        throw new Exception("Test document set would be needed to carry out domain adaptation properly.");
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  public List<DocumentData> determineDocumentSet(int foldNum, int totalFolds, boolean forTraining, DocumentSet otherData) {
    if (otherData != null) {
      isGenuineDAon = !baseDir.equals(otherData.baseDir);
    }
    double foldSize = (otherData != null && !baseDir.equals(otherData.baseDir) && adaptationType > -1 && forTraining ? otherData
        .size() : size())
        * Math.pow(totalFolds, -1);
    List<DocumentData> docs = new ArrayList<DocumentData>(this);
    if (totalFolds == 1) {
      return docs;
    }

    int fromIndex = (int) Math.floor((foldNum - 1) * foldSize), toIndex;
    if (foldNum == totalFolds) {
      toIndex = adaptationType > -1 && forTraining ? otherData.size() : size();
    } else {
      toIndex = (int) Math.floor(foldNum * foldSize);
    }

    if (forTraining) {
      if (otherData != null && !baseDir.equals(otherData.baseDir) && adaptationType > -1) {
        List<DocumentData> targetSubDomain = new ArrayList<DocumentData>(otherData).subList(fromIndex, toIndex);
        docs.addAll(targetSubDomain);
      } else {
        List<DocumentData> docsToRemove = new ArrayList<DocumentData>(docs).subList(fromIndex, toIndex);
        docs.removeAll(docsToRemove);
      }
      return docs;
    } else {
      if (isGenuineDAon && adaptationType > -1) {
        List<DocumentData> docsToRemove = new ArrayList<DocumentData>(docs.subList(fromIndex, toIndex));
        docs.removeAll(docsToRemove);
        return docs;
      } else {
        return docs.subList(fromIndex, toIndex);
      }
    }
  }
}
