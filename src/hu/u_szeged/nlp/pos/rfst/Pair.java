package hu.u_szeged.nlp.pos.rfst;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Pair<A, B> implements Serializable {
  protected A a;
  protected B b;

  // for deserialize extensions
  protected Pair() {
  }

  public Pair(A a, B b) {
    this.a = a;
    this.b = b;
  }

  public A getA() {
    return a;
  }

  public B getB() {
    return b;
  }

  boolean myEq(Object o1, Object o2) {
    if (o1 == null) {
      return o2 == null;
    }
    return o1.equals(o2);
  }

  @SuppressWarnings("unchecked")
  public boolean equals(Object obj) {
    if (!(obj instanceof Pair)) {
      return false;
    }
    Pair p = (Pair) obj;
    return myEq(a, p.a) && myEq(b, p.b);
  }

  int myHash(Object o) {
    if (o == null) {
      return 0;
    }
    return o.hashCode();
  }

  public int hashCode() {
    return 31 * myHash(a) + myHash(b);
  }

  public String toString() {
    return "(" + a + "; " + b + ")";
  }
}
