package com.sdbrody.cahweb;

public class StatusException extends Exception{

  private static final long serialVersionUID = 1L;

  public enum StatusType {
    DATASTORE_ERROR, BAD_INPUT, ILLEGAL
  }
  
  public StatusException(StatusType type, String message) {
    super(message);
    this.type = type;
  }
  
  protected StatusType type;
  public StatusType getType() {
    return type;
  }
  public String toString() {
    String ret;
    switch (type) {
    case DATASTORE_ERROR: ret = "DataStore Error: "; break;
    case BAD_INPUT: ret = "Bad Input: "; break;
    case ILLEGAL: ret = "Illegal Instruction: "; break;
    default: ret = "Unknown Error: "; break;
    }
    return ret + getMessage();
  }
}
