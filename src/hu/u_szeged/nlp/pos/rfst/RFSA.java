package hu.u_szeged.nlp.pos.rfst;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class RFSA {
  protected int stateCount;

  protected int edgeCount;

  protected int startingState;

  protected boolean[] ab;

  protected int[] indices;

  protected int[] targets;

  protected String[] symbols; // input char + output

  protected char[] charsymbols;

  // where we are in states, cf. addEdge
  protected int a;

  // where we are in targets
  protected int at;

  protected boolean sorted;

  protected String[] symbolhistory;

  public static interface Processor {
    void process(int state);
  }

  public RFSA(int startingState, int stateCount, int edgeCount) {
    this.startingState = startingState;
    this.stateCount = stateCount;
    this.edgeCount = edgeCount;

    ab = new boolean[stateCount];

    targets = new int[edgeCount];
    symbols = new String[edgeCount];
    charsymbols = new char[edgeCount];

    indices = new int[stateCount + 1];

    Arrays.fill(indices, -1);
    indices[stateCount] = edgeCount;
  }

  public boolean adeterministic() {
    int undeterministic = 0;
    int undets = 0;
    for (int s : allStates()) {
      Map<String, Integer> labels = new HashMap<String, Integer>();
      boolean b = false;
      for (Pair<String, Integer> p : outgoing(s)) {
        if (labels.containsKey(p.getA())) {
          // System.out.println(getClass().getSimpleName() +
          // ": not deterministic: " + s + "(" + ab[s] + "): " +
          // p.getA() + ": " + labels.get(p.getA()) +
          // "(" + ab[labels.get(p.getA())] + "), " +
          // p.getB() + "(" + ab[p.getB()] + ")");
          b = true;
          undeterministic++;
        }
        labels.put(p.getA(), p.getB());
      }
      if (b) {
        undets++;
      }
    }
    return undeterministic == 0;
  }

  public boolean legal(String s) {
    return true;
  }

  public void binarySearch(int q, char c, Processor p) {
    int i = indices[q];
    int j = indices[q + 1];

    int low = i;
    int high = j - 1;
    int mid;
    while (low <= high) {
      mid = (low + high) >> 1;
      int cmp = charsymbols[mid] - c;

      if (cmp == 0) {
        int l = mid;

        while (++mid < j && charsymbols[mid] == c)
          ;
        while (--l >= i && charsymbols[l] == c)
          ;

        for (int next = l + 1; next < mid; next++) {
          p.process(next);
        }
        break;
      } else if (cmp < 0) {
        low = mid + 1;
      } else if (cmp > 0) {
        high = mid - 1;
      }
    }
  }

  // assume sorted!
  public Collection<String> analyse(String s) {
    char[] ac = s.toLowerCase().toCharArray();
    return analyse(ac);
  }

  public Collection<String> analyse(char[] ac) {
    Collection<String> analyses = new ArrayList<String>();
    symbolhistory = new String[ac.length + 1];
    analyse(startingState, ac, 0, "", analyses);
    return analyses;
  }

  // binary search
  public void analyse(int q, char[] ac, int pos, String symbol, Collection<String> analyses) {
    // System.out.println(symbol);
    // System.out.println(new String(ac).substring(0,pos) + " " + q +
    // (ab[q]?" veg":"") );
    // System.out.println(analyses);
    symbolhistory[pos] = symbol;
    if (pos == ac.length) {
      if (ab[q]) {
        analyses.add(symbol/* +"@"+getMSDLemma(ac) */);
      }
      return;
    }

    char c = ac[pos];
    int i = indices[q];
    int j = indices[q + 1];

    int low = i;
    int high = j - 1;
    int mid;
    while (low <= high) {
      mid = (low + high) >> 1;
      int cmp = charsymbols[mid] - c;

      if (cmp == 0) {
        int l = mid;
        while (++mid < j && charsymbols[mid] == c)
          ;
        while (--l >= i && charsymbols[l] == c)
          ;

        for (int next = l + 1; next < mid; next++) {
          analyse(targets[next], ac, pos + 1, symbol + symbols[next], analyses);
        }
        break;
      } else if (cmp < 0) {
        low = mid + 1;
      } else if (cmp > 0) {
        high = mid - 1;
      }
    }
  }

  // linear search
  public void analyse1(int q, char[] ac, int pos, String symbol, Collection<String> analyses) {
    if (pos == ac.length) {
      if (ab[q]) {
        analyses.add(symbol);
      }
      return;
    }

    char c = ac[pos];
    int i = indices[q];
    int j = indices[q + 1];

    for (int next = i; next < j; next++) {
      if (c == charsymbols[next]) {
        analyse(targets[next], ac, pos + 1, symbol + symbols[next], analyses);
      }
    }
  }

  public void addState(int s, boolean accepting) {
    ab[s] = accepting;
  }

  public void addEdge(int source, String label, int target) {
    if (source < a) {
      throw new IllegalArgumentException();
    }

    if (indices[source] == -1) {
      indices[source] = at;
    }

    char input = label.charAt(0);
    charsymbols[at] = input;
    symbols[at] = label.substring(1);
    targets[at] = target;
    a = source;
    at++;
  }

  public String getKRLemma(String symbol) {
    String KR_szoto = "";
    for (String morph : symbol.split("\\+")) {
      int s = (morph.startsWith("$")) ? 1 : 0;
      int ppp = morph.indexOf('/');
      if (ppp < 0)
        KR_szoto += morph.substring(s);
      else
        KR_szoto += morph.substring(s, ppp);
    }

    KR_szoto = KR_szoto.replace("@", "");
    return KR_szoto;
  }

  protected String getLastPOS(String symbol, String pos) {
    if (symbol.contains(pos))
      return symbol.substring(0, symbol.indexOf(pos)) + pos.substring(0, pos.indexOf("["));
    return symbol;
  }

  protected String getMSDLemma(char[] ac) {
    String symbol = symbolhistory[symbolhistory.length - 1];
    String POS = symbol;
    POS = getLastPOS(POS, "/ADJ[COMPAR]");
    POS = getLastPOS(POS, "/ADJ[SUPERLAT]");
    POS = getLastPOS(POS, "/ADJ[SUPERSUPERLAT]");
    POS = getLastPOS(POS, "/ADJ[MANNER]");
    POS = getLastPOS(POS, "/NOUN[ESS_FOR]");
    int p = POS.lastIndexOf('/');
    int pp = POS.indexOf('<', p);
    if (pp > 0)
      POS = symbol.substring(0, pp);

    int i = 0;
    while (!(symbolhistory[i].startsWith(POS) || (!symbolhistory[i].contains("/") && symbolhistory[i].equals(POS.substring(0, p))))) {
      ++i;
    }

    String szoalak_szoto = new String(ac).substring(0, i);

    // leg...
    if (symbol.contains("/ADJ[SUPERLAT]")) {
      szoalak_szoto = new String(ac).substring(3, i);
    }

    // legesleg...
    if ((symbol.contains("/ADJ[SUPERSUPERLAT]")) && szoalak_szoto.startsWith("legesleg")) {
      szoalak_szoto = new String(ac).substring(8, i);
    }

    String KR_szoto = getKRLemma(symbolhistory[i]);
    String MSDszoto = KR_szoto.length() >= szoalak_szoto.length() ? KR_szoto : szoalak_szoto;
    return MSDszoto;
  }

  public void noedge(int source) {
    if (source < a) {
      throw new IllegalArgumentException();
    }
    indices[source] = at;
    a = source;
  }

  public void setAccepting(int state, boolean b) {
    ab[state] = b;
  }

  public int startingState() {
    return startingState;
  }

  public Iterable<Integer> allStates() {
    return new StateIterator(stateCount);
  }

  public boolean isAccepting(int state) {
    return ab[state];
  }

  public Iterable<Pair<String, Integer>> outgoing(int state) {
    return new EdgeIterable(state);
  }

  // edges of state s are enlisted in
  // [targets[indices[i]], targets[indices[i+1]])
  public int size(int s) {
    if (s >= stateCount) {
      throw new IllegalArgumentException(s + " >= " + stateCount);
    }
    return indices[s + 1] - indices[s];
  }

  public int stateCount() {
    return stateCount;
  }

  public String toString() {
    return getClass().getSimpleName() + "[" + stateCount + ", " + edgeCount + "]";
  }

  public String toDetailedString() {
    StringBuffer sb = new StringBuffer("  " + stateCount + ", " + edgeCount + ", " + startingState + "\n");

    for (int i = 0; i < stateCount; i++) {
      sb.append("    " + i + ", " + ab[i] + ", " + (indices[i + 1] - indices[i]) + "\n");
      for (int j = indices[i]; j < indices[i + 1]; j++) {
        sb.append("      " + targets[j] + ": >" + charsymbols[j] + "|" + symbols[j] + "<\n");
      }
    }
    return sb.toString();
  }

  public class EdgeIter {
    protected int state;

    protected int size;

    protected int start;

    protected int next;

    public EdgeIter(int state) {
      this.state = state;
      size = size(state);
      start = indices[state];
      next = start;
    }

    public boolean hasNext() {
      return next < start + size;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public class EdgeIterable extends EdgeIter implements Iterable<Pair<String, Integer>>, Iterator<Pair<String, Integer>> {
    public EdgeIterable(int state) {
      super(state);
    }

    public Iterator<Pair<String, Integer>> iterator() {
      return new EdgeIterable(state);
    }

    public Pair<String, Integer> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      int target = targets[next];
      String label = charsymbols[next] + symbols[next];
      next++;
      return new Pair<String, Integer>(label, target);
    }
  }

  protected Sorter createSorter(int state) {
    return new Sorter(state);
  }

  public void sort() {
    sorted = true;
    for (int state = 0; state < stateCount; state++) {
      if (indices[state] == indices[state + 1]) {
        continue;
      }

      Sorter sorter = createSorter(state);
      sorter.sort();
    }
  }

  public class Sorter {
    protected int state;

    protected int length;

    public Sorter(int state) {
      this.state = state;
    }

    public void sort() {
      length = indices[state + 1] - indices[state];

      String[] as = new String[length];
      char[] ac = new char[length];
      int[] at = new int[length];

      System.arraycopy(charsymbols, indices[state], ac, 0, length);
      System.arraycopy(symbols, indices[state], as, 0, length);
      System.arraycopy(targets, indices[state], at, 0, length);

      Integer[] ai = new Integer[length];
      for (int i = 0; i < length; i++) {
        ai[i] = i + indices[state];
      }

      Arrays.sort(ai, new Comparator<Integer>() {
        public int compare(Integer arg0, Integer arg1) {
          return charsymbols[arg0] - charsymbols[arg1];
        }
      });

      for (int i = 0; i < length; i++) {
        int j = ai[i] - indices[state];
        charsymbols[i + indices[state]] = ac[j];
        symbols[i + indices[state]] = as[j];
        targets[i + indices[state]] = at[j];
      }
    }
  }

  public int valid() {
    Collection<Integer> valid = new HashSet<Integer>();
    for (int i = 0; i < stateCount; i++) {
      if (ab[i]) {
        valid.add(i);
      }
    }

    System.out.println(getClass().getSimpleName() + ": valid starts with " + valid.size() + " accepting states");

    int size;
    do {
      size = valid.size();
      for (int i = 0; i < stateCount; i++) {
        if (valid.contains(i)) {
          continue;
        }
        for (int j = indices[i]; j < indices[i + 1]; j++) {
          if (valid.contains(targets[j])) {
            valid.add(i);
            break;
          }
        }
      }
    } while (valid.size() != size);

    return valid.size();
  }

  public void print(String file) throws IOException {
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

    pw.println(startingState + "\t" + stateCount + "\t" + edgeCount);

    for (int i = 0; i < stateCount; i++) {
      pw.println(i + "\t" + ab[i]);
      pw.println(indices[i + 1] - indices[i]);
      for (int j = indices[i]; j < indices[i + 1]; j++) {
        pw.println(charsymbols[j] + symbols[j] + "\t" + targets[j]);
      }
    }
    pw.close();
  }

  public int getA() {
    return a;
  }

  public boolean[] getAb() {
    return ab;
  }

  public int getAt() {
    return at;
  }

  public char[] getCharsymbols() {
    return charsymbols;
  }

  public int getEdgeCount() {
    return edgeCount;
  }

  public int[] getIndices() {
    return indices;
  }

  public boolean isSorted() {
    return sorted;
  }

  public int getStartingState() {
    return startingState;
  }

  public int getStateCount() {
    return stateCount;
  }

  public String[] getSymbols() {
    return symbols;
  }

  public int[] getTargets() {
    return targets;
  }

  public static RFSA read(InputStream rfsaStream, String encoding) throws IOException {
    Map<String, String> labelMap = new HashMap<String, String>();

    LineNumberReader reader = new LineNumberReader(new InputStreamReader(rfsaStream, encoding));

    String line = reader.readLine();
    StringTokenizer st = new StringTokenizer(line);

    int startIndex = Integer.parseInt(st.nextToken());
    int stateCount = Integer.parseInt(st.nextToken());
    int edgeCount = Integer.parseInt(st.nextToken());

    RFSA rfsa = new RFSA(startIndex, stateCount, edgeCount);
    for (int i = 0; i < stateCount; i++) {
      // state line with state number and accepting
      line = reader.readLine();
      st = new StringTokenizer(line, "\t");
      int state = Integer.parseInt(st.nextToken());
      boolean accepting = new Boolean(st.nextToken());

      rfsa.addState(state, accepting);

      // line with edgecount
      line = reader.readLine();
      st = new StringTokenizer(line);
      int edges = Integer.parseInt(st.nextToken());
      if (edges == 0) {
        rfsa.noedge(state);
      }
      // lines with edges
      for (int j = 0; j < edges; j++) {
        line = reader.readLine();
        int index = line.indexOf('\t');
        String s = line.substring(0, index);
        if (s.length() == 0) {
          throw new IllegalStateException();
        }
        int target = Integer.parseInt(line.substring(index + 1));
        String label = labelMap.get(s);
        if (label == null) {
          labelMap.put(s, label = s);
        }
        rfsa.addEdge(state, label, target);
      }
    }
    reader.close();
    rfsa.sort();
    return rfsa;
  }

  public static RFSA read(String defaultRfsa) {
    // TODO Auto-generated method stub
    return null;
  }
}
