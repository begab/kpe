package hu.u_szeged.kpe;

import hu.u_szeged.kpe.candidates.NGram;
import hu.u_szeged.kpe.main.ExtractionModelBuilder;
import hu.u_szeged.kpe.main.KPEFilter;
import hu.u_szeged.kpe.main.KeyPhraseExtractor;
import hu.u_szeged.kpe.readers.DocumentSet;
import hu.u_szeged.kpe.readers.KpeReader;
import hu.u_szeged.ml.mallet.MalletClassifier;
import hu.u_szeged.utils.NLPUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class KpeMain {

  private int totalFolds;
  private int actualFold;
  private String mode;
  private List<String> selectedFeatures;
  private boolean wordNetUsage;
  private boolean noStopWordPruning;
  private boolean noPosEndingPruning;
  private boolean mweFeatureIsOn;
  private boolean neFeatureIsOn;
  private boolean syntacticFeatureIsOn;

  private ExtractionModelBuilder km;
  private KeyPhraseExtractor ke;

  public static long time;

  public KpeMain(int phrases2Return, boolean prune, int featureCoding, boolean[] prunings, String wnLocation) {
    wordNetUsage = NGram.initWordNet(wnLocation);
    noPosEndingPruning = prunings[0];
    noStopWordPruning = prunings[1];
    km = new ExtractionModelBuilder();
    km.setMaxPhraseLength(4);
    km.setMinNumOccur(1);
    // km.setUseSynonyms(useSynonyms[0]);

    ke = new KeyPhraseExtractor();
    ke.setNumPhrases(phrases2Return);
    ke.setPrune(prune);
    // ke.setUseSynonyms(useSynonyms[1]);

    List<String> featureClasses = new LinkedList<>();
    NLPUtils.readDocToCollection(System.getProperty("user.dir") + "/resources/features", featureClasses);
    selectedFeatures = new LinkedList<String>();
    mode = extendToDesiredLength(Integer.toBinaryString(featureCoding), featureClasses.size());
    // mode = featureCoding;
    String escapedFeatureEncoding = mode.replaceAll("_", "");
    int c = 0;
    for (Iterator<String> it = featureClasses.iterator(); it.hasNext(); ++c) {
      String feature = it.next().split("\t")[1];
      if (escapedFeatureEncoding.charAt(c) == '1') {
        if (feature.equals("MweFeature")) {
          mweFeatureIsOn = true;
        } else if (feature.equals("StrangeOrthographyFeature")) {
          neFeatureIsOn = true;
        }
        selectedFeatures.add(feature);
      }
    }
    System.err.println("Features used:\t" + selectedFeatures);
  }

  public boolean getNoStopWordPruning() {
    return noStopWordPruning;
  }

  public boolean getNoPosEndingPruning() {
    return noPosEndingPruning;
  }

  private List<String> parseLine(String line) {
    List<String> readerAndLocations = new ArrayList<String>();
    int arrowIndex = line.indexOf("->");
    if (line.equals("null") || arrowIndex == -1)
      return readerAndLocations;
    String reader = line.substring(0, arrowIndex);
    readerAndLocations.add(reader);
    String[] differentPaths = line.substring(arrowIndex + 2).split("\\|");

    for (String path : differentPaths) {
      readerAndLocations.add(path);
    }
    return readerAndLocations;
  }

  private Map<Boolean, List<String>> parseReaderSettings(String trainingData, String testData) {
    Map<Boolean, List<String>> readerSettings = new HashMap<Boolean, List<String>>();
    readerSettings.put(true, parseLine(trainingData));
    readerSettings.put(false, parseLine(testData));
    return readerSettings;
  }

  /**
   * @param trainReader
   * @param testReader
   * @param trainLocs
   * @param testLocs
   * @param foldNum
   * @return returns the fact whether a new model is going to be trained
   */

  public boolean setReaders(String trainingData, String testData, int foldNum, boolean[] goldAnn, int adaptation, boolean serialize) {
    totalFolds = foldNum;
    Map<Boolean, List<String>> readerSettings = parseReaderSettings(trainingData, testData);
    for (Entry<Boolean, List<String>> entry : readerSettings.entrySet()) {
      try {
        Boolean train = entry.getKey();
        String readerName = entry.getValue().get(0);
        if ((readerName.equals("null") || entry.getValue().size() < 2) && train) {
          continue;
        }

        KpeReader reader = Class.forName("hu.u_szeged.kpe.readers." + readerName).asSubclass(KpeReader.class).newInstance();
        reader.initGrammar(mweFeatureIsOn, neFeatureIsOn, syntacticFeatureIsOn);
        DocumentSet ds = new DocumentSet(adaptation, reader);

        reader.setUseGoldAnnotation(train ? goldAnn[0] : goldAnn[1]);
        for (int i = 1; i < entry.getValue().size(); ++i) {
          String path = entry.getValue().get(i);
          ds.setBaseDir(path);
          if (serialize) {
            System.err.println("Note that (due to the config parameters) files containing serializations of grammatic analysis will be saved to location: "
                + path + "grammar/");
          }
          reader.addDirectoryOfFiles(path, train, ds);
        }

        if (train) {
          km.setDocSet(ds);
        } else {
          if (ds.size() == 0) {
            System.err.println("No test documents were added. The program will exit now.");
            System.exit(2);
          }
          ke.setDocSet(ds);
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return km.getDocSet() != null;
  }

  protected void setFold(int fold) {
    actualFold = fold;
  }

  protected void createModel(String classifier, boolean synonyms, boolean goldAnn, boolean[] employBIESmarkup, double commonWords, double selectedFeatureRatio,
      String[] loc, boolean serialize) throws Exception {
    String modelName = "models/" + loc[0] + "/" + (loc[1] != null ? loc[1] + "/" : "") + mode + "_"
        + (selectedFeatureRatio < 1.0d ? "fs_" + selectedFeatureRatio + "_" : "") + (employBIESmarkup[0] ? "BIES_pos_" : "")
        + (employBIESmarkup[1] ? "BIES_ne_" : "") + (employBIESmarkup[2] ? "BIES_suffix_" : "") + (wordNetUsage ? "wn_" : "")
        + (!noStopWordPruning ? "sw_" : "") + (!noPosEndingPruning ? "pos_" : "") + (classifier.equals("MaxEntL1") ? "" : "_" + classifier)
        + (totalFolds > 1 ? "fold" + actualFold + "_" : "") + goldAnn + ".model";
    System.err.println(modelName);
    km.buildModel(actualFold, totalFolds, selectedFeatures, classifier, commonWords, selectedFeatureRatio, employBIESmarkup, ke.getDocSet(), noStopWordPruning,
        noPosEndingPruning, serialize);
    KPEFilter kf = km.getKPEFilter();
    if (kf.getClassifierName().contains("MaxEnt")) {
      new File("models/" + loc[0]).mkdirs();
      ((MalletClassifier) kf.getModel()).printModel(new PrintWriter(modelName + "_statistics.txt"), 50);
    }
    NLPUtils.serialize(km.getKPEFilter(), modelName);
  }

  private void extractKeyphrases(String classifier, boolean synonyms, boolean[] goldAnn, boolean[] employBIESmarkup, double selectedFeatureRatio, String[] loc,
      boolean serialize) {
    String modelName = "models/" + loc[0] + "/" + (loc[1] != null ? loc[1] + "/" : "") + mode + "_"
        + (selectedFeatureRatio < 1.0d ? "fs_" + selectedFeatureRatio + "_" : "") + (employBIESmarkup[0] ? "BIES_pos_" : "")
        + (employBIESmarkup[1] ? "BIES_ne_" : "") + (employBIESmarkup[2] ? "BIES_suffix_" : "") + (wordNetUsage ? "wn_" : "")
        + (!noStopWordPruning ? "sw_" : "") + (!noPosEndingPruning ? "pos_" : "") + (classifier.equals("MaxEntL1") ? "" : "_" + classifier)
        + (totalFolds > 1 ? "fold" + actualFold + "_" : "") + goldAnn[0] + ".model";
    try {
      if (!new File(modelName).exists()) {
        System.err.println("The desired model (" + modelName + ") cannot be found on the computer, the config needs to be modified in order to generate it.");
        System.exit(1);
      }
      System.err.println("Extration of keyphrases begins. Output will be located at ./models/" + loc[0] + "/ directory.");
      ke.loadModel(modelName);
      ke.extractKeyphrases(actualFold, totalFolds, modelName.replace(".model", "_" + goldAnn[1] + ".out"), serialize);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String extendToDesiredLength(String toExtend, int length) {
    return toExtend.length() < length ? extendToDesiredLength("0" + toExtend, length) : toExtend;
  }

  private static String[] processParamFile(String file) {
    String[] options = { "train", "test", "classifier", "featureEncoding", "numOfKeyphrases", "wordnet_dir", "posEndPrune", "stopWordPrune", "beisMarkup",
        "serializeAnnotations" };
    List<String> lines = new LinkedList<String>();
    NLPUtils.readDocToCollection(file, lines);
    String[] newArgs = new String[options.length];
    for (String line : lines) {
      int commentIndex = line.indexOf("//");
      if (commentIndex != -1) {
        line = line.substring(0, commentIndex);
      }
      if (line.length() == 0)
        continue;
      String[] parts = line.split("=");
      for (int p = 0; p < options.length; ++p) {
        if (options[p].equalsIgnoreCase(parts[0].trim())) {
          newArgs[p] = parts[1].trim();
          break;
        }
      }
    }
    return newArgs;
  }

  public static void main(String[] args) {
    String modelPrefix = null;
    if (args[0].equalsIgnoreCase("-paramFile")) {
      if (args.length > 2) {
        modelPrefix = args[2];
      }
      System.err.println("Configuration read from config file: " + args[1]);
      args = processParamFile(args[1]);
    }
    String[] params = { "Reader class and location(s) for training:", "Reader class and location(s) for testing:", "Classifier to use:",
        "Feature encoding to use:", "Number of keyphrases to extract?",
        "Location of  WordNet dict directory (or type in 'FALSE' in case you do not wish to use it)?", "stopword candidate phrase pruning not to be used?",
        "pos ending-based candidate phrase pruning not to be used?", "B(egin)I(nside)E(nd)S(ingle) feature markup?", "serialize grammar files?" };
    args = Arrays.copyOf(args, params.length);
    for (int i = 0; i < args.length; ++i) {
      if (args[i] == null) {
        try {
          System.err.print(params[i] + "\t");
          BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
          args[i] = br.readLine();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    for (int i = 0; i < args.length; ++i) {
      System.err.println(params[i] + "\t--->\t" + args[i]);
    }

    String locationPrefix = args[1].replaceAll("->.*[\\/]?", "");
    String classifier = args[2];
    String featureCoding = args[3];
    int numOfKeyphrases = Integer.parseInt(args[4]);
    String wordNetParameter = args[5];
    boolean[] filtration = { Boolean.parseBoolean(args[6]), Boolean.parseBoolean(args[7]) };
    String[] beisSettings = args[8].split(",");
    boolean[] employBIESmarkup = new boolean[3];
    for (int i = 0; i < beisSettings.length; ++i) {
      employBIESmarkup[i] = Boolean.parseBoolean(beisSettings[i]);
    }
    boolean serializeGrammar = Boolean.parseBoolean(args[9]);

    // these are just dummy, burned-in values in order to neutralize experimental and/or not well-tested enough features temporarily
    boolean finalPrune = false;
    int numOfFolds = 1;
    boolean[] goldAnn = { true, true };
    boolean[] useSynonyms = new boolean[2];
    int adaptation = -1;
    double selectedFeatureRatio = 1.0d;

    KpeMain kpe = new KpeMain(numOfKeyphrases, finalPrune, Integer.parseInt(featureCoding), filtration, wordNetParameter);
    String trainParameters = args[0], testParameters = args[1];
    boolean newModel = kpe.setReaders(trainParameters, testParameters, numOfFolds, goldAnn, adaptation, serializeGrammar);

    String[] location = { locationPrefix, modelPrefix };
    for (int fold = 1; fold <= numOfFolds; ++fold) {
      System.err.println("Fold #" + fold);
      time = System.currentTimeMillis();
      kpe.setFold(fold);
      if (newModel) {
        try {
          kpe.createModel(classifier, useSynonyms[0], goldAnn[0], employBIESmarkup, 0.1d, selectedFeatureRatio, location, serializeGrammar);
        } catch (Exception e) {
          e.printStackTrace();
          continue;
        }
        if (fold == 1) {
          System.err.println(featureCoding);
          System.err.println("Reader phrases " + (goldAnn[0] ? "" : "not ") + "used.");
          System.err.println("WordNet is " + (wordNetParameter.equalsIgnoreCase("false") ? "" : "not ") + "used.");
          System.err.println("POS ending pruning is " + (kpe.getNoPosEndingPruning() ? "not " : "") + "used.");
          System.err.println("Stopword pruning is " + (kpe.getNoStopWordPruning() ? "not " : "") + "used.");
          System.err.println(args[1] + " will be keyphrased with a " + classifier + " classifier"
              + (newModel ? " and a new " + numOfFolds + "-fold model is being created." : "."));
        }
        System.err.println("Model done: " + (System.currentTimeMillis() - time) / 1000.0 + " secs");
      }
      kpe.extractKeyphrases(classifier, useSynonyms[0], goldAnn, employBIESmarkup, selectedFeatureRatio, location, serializeGrammar);
      System.err.println((System.currentTimeMillis() - time) / 1000.0);
    }
  }
}