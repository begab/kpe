package hu.u_szeged.kpe.readers;

import hu.u_szeged.kpe.aspirants.NGram;

import java.util.Map;

public class ContestDocumentData extends DocumentData {

  private static final long serialVersionUID = 748218057710981109L;
  private Map<NGram, Integer> authorAssignedPhrases;

  public ContestDocumentData(String keyphrases, String fileName, Class<?> docType) {
    super(keyphrases, fileName, docType);
  }

  public ContestDocumentData(String keyphrases, String authorassigned, String fileName, Class<?> docType) {
    super(keyphrases, fileName, docType);
    this.authorAssignedPhrases = transformKeyphrases(authorassigned);
  }

  public Map<NGram, Integer> getAuthorAssignedPhrases() {
    return authorAssignedPhrases;
  }

  public void setAuthorAssignedPhrases(String authorAssignedPhrases) {
    this.authorAssignedPhrases = transformKeyphrases(authorAssignedPhrases);
  }
}