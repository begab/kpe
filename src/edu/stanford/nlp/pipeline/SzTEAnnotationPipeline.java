package edu.stanford.nlp.pipeline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.MutableInteger;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.Timing;
import edu.stanford.nlp.util.logging.Redwood;

/**
 * This class is designed to apply multiple Annotators to an Annotation. The idea is that you first build up the pipeline by adding Annotators, and
 * then you takes the objects you wish to annotate and pass them in and get in return a fully annotated object. Please see package level javadocs for
 * sample usage and a more complete description.
 * <p>
 * At the moment this mainly serves as an example of using the system and actually more complex annotation pipelines are in their own classes that
 * don't extend this one.
 * 
 * @author Jenny Finkel
 */

public class SzTEAnnotationPipeline implements Annotator {

  protected static final boolean TIME = true;

  private List<Annotator> annotators;
  private List<MutableInteger> accumulatedTime;

  public SzTEAnnotationPipeline(List<Annotator> annotators) {
    this.annotators = annotators;
    if (TIME) {
      int num = annotators.size();
      accumulatedTime = new ArrayList<>(annotators.size());
      for (int i = 0; i < num; i++) {
        accumulatedTime.add(new MutableInteger());
      }
    }
  }

  public SzTEAnnotationPipeline() {
    this(new ArrayList<Annotator>());
  }

  public void addAnnotator(Annotator annotator) {
    annotators.add(annotator);
    if (TIME) {
      accumulatedTime.add(new MutableInteger());
    }
  }

  // EXTENSION
  public void annotate(Annotation annotation, Set<String> subAnnotators, boolean ngramAnnotation) {
    Iterator<MutableInteger> it = accumulatedTime.iterator();
    Timing t = new Timing();
    // boolean oneSentence = false;
    for (Annotator annotator : annotators) {
      if (subAnnotators == null || subAnnotators.contains(annotator.getClass().getSimpleName().replace("Annotator", ""))) {
        // if (ngramAnnotation && annotator instanceof WordsToSentencesAnnotator) {
        // oneSentence = ((OwnWordsToSentencesAnnotator) annotator).getIsOneSentence();
        // ((OwnWordsToSentencesAnnotator) annotator).setOneSentence(true);
        // }
        if (TIME) {
          t.start();
        }
        annotator.annotate(annotation);
        // System.out.println();
        if (TIME) {
          int elapsed = (int) t.stop();
          MutableInteger m = it.next();
          m.incValue(elapsed);
        }
        // if (ngramAnnotation && annotator instanceof WordsToSentencesAnnotator) {
        // ((OwnWordsToSentencesAnnotator) annotator).setOneSentence(oneSentence);
        // }
      }
    }
  }

  // ->EXTENSION

  /**
   * Run the pipeline on an input annotation. The annotation is modified in place
   * 
   * @param annotation
   *          The input annotation, usually a raw document
   */
  public void annotate(Annotation annotation) {
    annotate(annotation, null, false);
  }

  /**
   * Annotate a collection of input annotations IN PARALLEL, making use of all available cores.
   * 
   * @param annotations
   *          The input annotations to process
   */
  public void annotate(Iterable<Annotation> annotations) {
    annotate(annotations, Runtime.getRuntime().availableProcessors());
  }

  /**
   * Annotate a collection of input annotations IN PARALLEL, making use of all available cores
   * 
   * @param annotations
   *          The input annotations to process
   * @param callback
   *          A function to be called when an annotation finishes. The return value of the callback is ignored
   */
  public void annotate(final Iterable<Annotation> annotations, final Function<Annotation, Object> callback) {
    annotate(annotations, Runtime.getRuntime().availableProcessors(), callback);
  }

  /**
   * Annotate a collection of input annotations IN PARALLEL, making use of threads given in numThreads
   * 
   * @param annotations
   *          The input annotations to process
   * @param numThreads
   *          The number of threads to run on
   */
  public void annotate(final Iterable<Annotation> annotations, int numThreads) {
    annotate(annotations, numThreads, new Function<Annotation, Object>() {
      public Object apply(Annotation in) {
        return null;
      }
    });
  }

  /**
   * Annotate a collection of input annotations IN PARALLEL, making use of threads given in numThreads
   * 
   * @param annotations
   *          The input annotations to process
   * @param numThreads
   *          The number of threads to run on
   * @param callback
   *          A function to be called when an annotation finishes. The return value of the callback is ignored.
   */
  public void annotate(final Iterable<Annotation> annotations, int numThreads, final Function<Annotation, Object> callback) {
    // case: single thread (no point in spawning threads)
    if (numThreads == 1) {
      for (Annotation ann : annotations) {
        annotate(ann);
        callback.apply(ann);
      }
    }
    // Java's equivalent to ".map{ lambda(annotation) => annotate(annotation) }
    Iterable<Runnable> threads = new Iterable<Runnable>() {
      public Iterator<Runnable> iterator() {
        final Iterator<Annotation> iter = annotations.iterator();
        return new Iterator<Runnable>() {
          public boolean hasNext() {
            return iter.hasNext();
          }

          public Runnable next() {
            final Annotation input = iter.next();
            return new Runnable() {
              public void run() {
                // Jesus Christ, finally the body of the code
                // (logging)
                String beginningOfDocument = input.toString().substring(0, Math.min(50, input.toString().length()));
                Redwood.startTrack("Annotating \"" + beginningOfDocument + "...\"");
                // (annotate)
                annotate(input);
                // (callback)
                callback.apply(input);
                // (logging again)
                Redwood.endTrack("Annotating \"" + beginningOfDocument + "...\"");
              }
            };
          }

          public void remove() {
            iter.remove();
          }
        };
      }
    };
    // Thread
    Redwood.Util.threadAndRun(this.getClass().getSimpleName(), threads, numThreads);
  }

  /**
   * Return the total pipeline annotation time in milliseconds.
   * 
   * @return The total pipeline annotation time in milliseconds
   */
  protected long getTotalTime() {
    long total = 0;
    for (MutableInteger m : accumulatedTime) {
      total += m.longValue();
    }
    return total;
  }

  /**
   * Return a String that gives detailed human-readable information about how much time was spent by each annotator and by the entire annotation
   * pipeline. This String includes newline characters but does not end with one, and so it is suitable to be printed out with a
   * <code>println()</code>.
   * 
   * @return Human readable information on time spent in processing.
   */
  public String timingInformation() {
    StringBuilder sb = new StringBuilder();
    if (TIME) {
      sb.append("Annotation pipeline timing information:\n");
      Iterator<MutableInteger> it = accumulatedTime.iterator();
      long total = 0;
      for (Annotator annotator : annotators) {
        MutableInteger m = it.next();
        sb.append(StringUtils.getShortClassName(annotator)).append(": ");
        sb.append(Timing.toSecondsString(m.longValue())).append(" sec.\n");
        total += m.longValue();
      }
      sb.append("TOTAL: ").append(Timing.toSecondsString(total)).append(" sec.");
    }
    return sb.toString();
  }

  @Override
  public Set<Requirement> requirementsSatisfied() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<Requirement> requires() {
    // TODO Auto-generated method stub
    return null;
  }

}
