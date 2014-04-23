package hu.u_szeged.ml;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;

public abstract class Model implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -1653570677836049203L;

  public void printModel(OutputStream out) {
    printModel(new PrintWriter(out));
  }

  abstract public void printModel(PrintWriter out);
}
