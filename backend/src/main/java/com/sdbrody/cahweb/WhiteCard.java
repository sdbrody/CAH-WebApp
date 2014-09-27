package com.sdbrody.cahweb;

import java.io.Serializable;

public class WhiteCard implements Serializable {
  private static final long serialVersionUID = 1L;
  public WhiteCard(int id) {
    this.id = id;
  }
  @Override
  public String toString() {
    return "WhiteCard_" + id;
  }
  public int getId() {
    return id;
  }
  protected int id;
}
