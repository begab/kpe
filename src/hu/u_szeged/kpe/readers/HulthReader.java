package hu.u_szeged.kpe.readers;

import hu.u_szeged.utils.NLPUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HulthReader extends GeneralReader {
  private static final String DEFAULT_EXTENSION = ".abstr";
  private Map<String, String> etalonKeyphrases;

  protected void setDetails() {
    fileType = DEFAULT_EXTENSION;
  }

  @Override
  public List<DocumentData> getContent(String dir, String file) {
    if (etalonKeyphrases == null) {
      readEtalonKeyphrases(dir);
    }
    List<DocumentData> toReturn = new ArrayList<DocumentData>(1);
    toReturn.add(new DocumentData(etalonKeyphrases.get(new File(file).getName().replace(fileType, "")), file, this.getClass()));
    return toReturn;
  }

  public String getText(String file, int numberWithinFile) {
    StringBuffer sb = new StringBuffer();
    List<String> lines = new ArrayList<>();
    NLPUtils.readDocToCollection(file, lines);
    boolean firstLine = true;
    for (String line : lines) {
      sb.append(line + (firstLine ? "\n" : " "));
      firstLine = false;
    }
    return sb.toString();
  }

  private void readEtalonKeyphrases(String dir) {
    etalonKeyphrases = new HashMap<String, String>();
    for (String fileName : new File(dir).list()) {
      if (fileName.endsWith(".uncontr")) {
        List<String> lines = new ArrayList<>();
        NLPUtils.readDocToCollection(dir + "/" + fileName, lines);
        StringBuffer sb = new StringBuffer();
        for (String line : lines) {
          sb.append(line + " ");
        }
        etalonKeyphrases.put(fileName.replace(".uncontr", ""), sb.toString().trim().replaceAll("\\s+", " ").replaceAll(";\\s+", "\n"));
      }
    }
  }
}
