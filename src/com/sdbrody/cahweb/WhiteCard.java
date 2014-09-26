package com.sdbrody.cahweb;

import java.io.Serializable;

public class WhiteCard implements Serializable {
  private static final long serialVersionUID = 1L;
  public WhiteCard(int id, String text) {
    this.id = id;
    this.text = text;
  }
  @Override
  public String toString() {
    return text;
  }
  public String getText() {
    return text;
  }
  public int getId() {
    return id;
  }
  protected int id;
  protected final String text;
  
}
