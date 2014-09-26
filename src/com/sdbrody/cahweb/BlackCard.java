package com.sdbrody.cahweb;

import java.io.Serializable;

public class BlackCard implements Serializable {
  private static final long serialVersionUID = 1L;
  public BlackCard(int id, int numSlots) {
    this.id = id;
    this.numSlots = numSlots;
  }
  public int getNumSlots() {
    return numSlots;
  }
  public int getId() {
    return id;
  }
  
  protected int numSlots;
  protected int id;
}
