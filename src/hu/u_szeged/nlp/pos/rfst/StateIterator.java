package hu.u_szeged.nlp.pos.rfst;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class StateIterator implements Iterable<Integer>, Iterator<Integer> {
  protected int size;
  protected int next;

  public StateIterator(int size) {
    this.size = size;
  }

  public Iterator<Integer> iterator() {
    return new StateIterator(size);
  }

  public boolean hasNext() {
    return next < size;
  }

  public Integer next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return next++;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
}
