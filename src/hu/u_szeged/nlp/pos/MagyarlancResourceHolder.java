package hu.u_szeged.nlp.pos;

import hu.u_szeged.nlp.pos.converter.KRToMSD;
import hu.u_szeged.nlp.pos.converter.MSDReducer;
import hu.u_szeged.nlp.pos.rfst.RFSA;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.io.IOUtils;

public class MagyarlancResourceHolder {

  private static final String DEFAULT_CORPUS = "szeged.lex"; // "treebank.corpus"
  private static final String DEFAULT_FREQUENCIES = "szeged.freq";// "treebank.freq";

  private static final String DEFAULT_RFSA = "rfsa.txt";
  private static final String DEFAULT_CORRDIC = "corrdic.txt";

  private static Set<String> punctations = null;
  private static Set<String> morPhonDir = null;

  private static MSDReducer msdReducer = null;

  private static Map<String, Set<MorAna>> corpus = null;
  private static Map<String, Integer> frequencies = null;
  private static Map<String, String> corrDic = null;

  private static RFSA rfsa = null;

  private static KRToMSD krToMsd = null;

  // MorPhonDir
  public static Set<String> getMorPhonDir() {
    if (morPhonDir == null)
      initMorPhonDir();

    return morPhonDir;
  }

  public static void initMorPhonDir() {
    if (morPhonDir == null) {
      morPhonDir = Util.loadMorPhonDir();
      System.err.println("morPhonDir loaded " + morPhonDir);
    }
  }

  // KRToMSD
  public static KRToMSD getKRToMSD() {
    if (krToMsd == null)
      initKRToMSD();

    return krToMsd;
  }

  public static void initKRToMSD() {
    if (krToMsd == null) {
      krToMsd = new KRToMSD();
      System.err.println("krToMsd loaded");
    }
  }

  // RFSA
  public static RFSA getRFSA() {
    return rfsa;
  }

  public static void initRFSA(String location) {
    if (rfsa == null) {
      try {
        rfsa = RFSA.read(IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(location), "UTF-8");
        System.err.println("rfsa loaded (" + rfsa.getStateCount() + " states, " + rfsa.getEdgeCount() + " edges)");
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // corpus
  public static Map<String, Set<MorAna>> getCorpus() {
    if (corpus == null)
      initCorpus();
    return corpus;
  }

  public static void initCorpus(String corpusFile) {
    if (corpus == null) {
      corpus = Util.readCorpus(corpusFile);
      System.err.println("corpus loaded (" + corpus.size() + " wordshapes)");
    }
  }

  public static void initCorpus() {
    initCorpus(DEFAULT_CORPUS);
  }

  // corrDic
  public static Map<String, String> getCorrDic() {
    if (corrDic == null)
      initCorrDic();

    return corrDic;
  }

  public static void initCorrDic() {
    initCorrDic(DEFAULT_CORRDIC);
  }

  public static void initCorrDic(String location) {
    if (corrDic == null) {
      corrDic = Util.readCorrDic(location);
      System.err.println("corrDic loaded (" + corrDic.size() + " standardized form)");
    }
  }

  // Frequencies
  public static Map<String, Integer> getFrequencies() {
    if (frequencies == null) {
      initFrequencies();
    }

    return frequencies;
  }

  public static void initFrequencies(String freqFile) {
    if (frequencies == null) {
      frequencies = Util.readFrequencies(freqFile);
      System.err.println("frequencies loaded (" + frequencies.size() + " MSD)");
    }
  }

  public static void initFrequencies() {
    initFrequencies(DEFAULT_FREQUENCIES);
  }

  // MSDReducer
  public static MSDReducer getMSDReducer() {
    if (msdReducer == null)
      initMSDReducer();

    return msdReducer;
  }

  public static void initMSDReducer() {
    if (msdReducer == null) {
      msdReducer = new MSDReducer();
      System.err.println("msdReducer loaded");
    }
  }

  // punctuations

  public static Set<String> getPunctations() {
    if (punctations == null)
      initPunctations();

    return punctations;
  }

  public static void initPunctations() {
    if (punctations == null) {
      punctations = Util.loadPunctations();
      System.err.println("punctations loaded " + punctations);
    }
  }

}