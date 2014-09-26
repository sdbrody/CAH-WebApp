package com.sdbrody.cahweb;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class GameConfiguration {
  
  public static enum VoteMode {
    JUDGE, VOTE 
  }
  public static final String kConfigKind = "Config";
  public static final String kConfigName = "Config";
  
  public GameConfiguration(int cardsPerHand, VoteMode voteMode,
      boolean swapAllowed, boolean passAllowed) {
    this.cardsPerHand = cardsPerHand;
    this.voteMode = voteMode;
    this.swapAllowed = swapAllowed;
    this.passAllowed = passAllowed;
    this.players = new HashMap<String, Player>();
  }
  
  @SuppressWarnings("unchecked")
  private GameConfiguration(DatastoreService datastore, Key gameId) throws StatusException {
    Key key = KeyFactory.createKey(gameId, kConfigKind, kConfigName);
    Entity entity;
    try {
      entity = datastore.get(key);
    } catch (EntityNotFoundException e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Config not found in datastore");
    }
    DatastoreHelpers.Reader reader = new DatastoreHelpers.Reader(entity);
    try {
      this.cardsPerHand = reader.in.readInt();
      this.voteMode = VoteMode.values()[reader.in.readInt()];
      this.swapAllowed = reader.in.readBoolean();
      this.passAllowed = reader.in.readBoolean();
      this.players = (HashMap<String, Player>) reader.in.readObject();
    } catch (Exception e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Error reading configuration from datastore");
    }
  }
  
  public static GameConfiguration retrieve(DatastoreService datastore, Key gameId) throws StatusException{
    return new GameConfiguration(datastore, gameId);
  }
  
  public void AddPlayer(String pid, Player player) throws StatusException {
    if (players.containsKey(pid))
      throw new StatusException(StatusException.StatusType.BAD_INPUT, "Player " + pid + " already exists");
    players.put(pid, player);
  }
  
  public final Map<String, Player> getPlayers() {
    return players;
  }
  
  public void store(DatastoreService datastore, Key gameId) throws StatusException {
    DatastoreHelpers.Writer writer = new DatastoreHelpers.Writer();
    try {
      writer.out.writeInt(cardsPerHand);
      writer.out.writeInt(voteMode.ordinal());
      writer.out.writeBoolean(swapAllowed);
      writer.out.writeBoolean(passAllowed);
      writer.out.writeObject(this.players);
    } catch (IOException e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Error writing configuration: " + e.getMessage());
    }
    datastore.put(writer.done(kConfigKind, kConfigName, gameId));
  }
  
  public JSONObject toJSON() {
    JSONArray playersJson = new JSONArray();
    for (Player player : players.values()) {
      playersJson.put(player.toJSON());
    }
    JSONObject ret = new JSONObject().
        put("cardsPerHand", cardsPerHand).
        put("voteMode", voteMode.toString()).
        put("swapAllowed", swapAllowed).
        put("passAllowed", passAllowed).
        put("players", playersJson);
    return ret;
  }
  
  @Override
  public String toString() {
    return toJSON().toString();
  }

  public final int cardsPerHand;
  public final VoteMode voteMode;
  public final boolean swapAllowed;
  public final boolean passAllowed;
  public Map<String, Player> players;
}
