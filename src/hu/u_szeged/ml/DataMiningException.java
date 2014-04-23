package hu.u_szeged.ml;

public class DataMiningException extends Exception {
  static final long serialVersionUID = 2451L;
  
  public DataMiningException(String msg){
    super(msg);
  }

  public DataMiningException(String msg, Exception cause){
    super(msg);
    initCause(cause);
  }
  
  public DataMiningException(Exception cause){
    super();
    initCause(cause);
  }
}
