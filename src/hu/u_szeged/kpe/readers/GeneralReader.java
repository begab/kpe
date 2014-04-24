package hu.u_szeged.kpe.readers;

import hu.u_szeged.utils.NLPUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is probably the simplest kind of {@link}KpeReader imaginable. <br>
 * Without making any real assumptions, this Reader simply treats all the contents of files as valuable content. <br>
 * Etalon keyphrases of documents in the directory are expected to be listed in a file named etalon.keys in the same directory where documents reside.<br>
 * Each line of the file etalon.keys should contain the absolute path of a document followed by a TAB and the list of etalon keyphrases for the
 * document. <br>
 * The presence of etalon keyphrases in file etalon.keys is crucial for training data.
 * 
 * @author berend
 * 
 */

public class GeneralReader extends KpeReader {

  private Map<String, String> etalonKeyphrases;

  @Override
  public List<DocumentData> getContent(String dir, String file) {
    if (etalonKeyphrases == null) {
      readEtalonKeyphrases(dir + "/etalon.keys");
    }
    List<DocumentData> toReturn = new ArrayList<DocumentData>(1);
    toReturn.add(new DocumentData(etalonKeyphrases.get(new File(file).getName().replace(fileType, "")), file, this.getClass()));
    return toReturn;
  }

  @Override
  public String getText(String file, int numberWithinFile) {
    StringBuffer sb = new StringBuffer();
    List<String> lines = new ArrayList<>();
    NLPUtils.readDocToCollection(file, lines);
    for (String line : lines) {
      sb.append(line + "\n");
    }
    return sb.toString();
  }

  @Override
  protected boolean mightBeSectionHeader(String line) {
    // as this class intends to be very general, there seem to be no ways of generalizing section headers.
    return false;
  }

  @Override
  protected void setDetails() {
    fileType = DEFAULT_EXTENSION;
  }

  private void readEtalonKeyphrases(String path) {
    etalonKeyphrases = new HashMap<>();
    List<String> lines = new ArrayList<>();
    NLPUtils.readDocToCollection(path, lines);
    for (String line : lines) {
      String[] parts = line.split("\t");
      etalonKeyphrases.put(parts[0], parts[1].replaceAll("\\s*,\\s*", "\n"));
    }
  }

}
