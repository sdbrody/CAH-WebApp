package com.sdbrody.cahweb;

import java.io.Serializable;

import org.json.JSONObject;


public class Player implements Serializable{
  public static enum PlayerType {
    MOBILE, WEB, RANDO
  }
  
  private static final long serialVersionUID = 1L;
  public final String name;
  public final PlayerType type;
  public final boolean isPassive;
  public final boolean isOwner;
  public String channel;
  
  public Player(String name, PlayerType type, boolean isPassive, boolean isOwner, String channel) {
    this.name = name;
    this.type = type;
    this.isPassive = isPassive;
    this.isOwner = isOwner;
    this.channel = channel;
  }

  public JSONObject toJSON() {
    return new JSONObject().
        put("name", name).
        put("type", type.toString()).
        put("passive", isPassive).
        put("owner", isOwner);
  }
  
  @Override
  public String toString() {
    return toJSON().toString();
  }   
}
