package com.sdbrody.cahweb;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

public class HistoricRound implements Serializable {
  public static final String kHistoricRoundStateKind = "HistoricRound";
  public static final String kRoundProperty ="round";
  public static final String roundName(int roundNumber) {
    return kHistoricRoundStateKind + "#" + roundNumber;
  }
  
  private static final long serialVersionUID = 1L;
  public final int roundNumber;
  public final int blackCard;
  public final int[][] selections;
  public final int winningIndex;
  public final String winningPlayerId;
  
  public HistoricRound(final RoundManager round, String winner) {
    this.roundNumber = round.getRoundNumber();
    this.blackCard = round.getBlackCard().id;
    this.winningPlayerId = winner;
    int windex = 0;
    final Map<String, int[]> selections = round.getSelections();
    int[][] selectionArray = new int[selections.size()][];
    int i = 0;
    for (Map.Entry<String, int[]> entry : selections.entrySet()) {
      selectionArray[i] = entry.getValue();
      if (entry.getKey() == winner)
        windex = i;
      ++i;
    }
    this.selections = selectionArray;
    this.winningIndex = windex;
  }
  
  private HistoricRound(Entity entity) throws StatusException {
    DatastoreHelpers.Reader reader = new DatastoreHelpers.Reader(entity);
    try {
      this.roundNumber = ((Long) entity.getProperty(kRoundProperty)).intValue();
      this.blackCard = reader.in.readInt();
      this.selections = (int[][]) reader.in.readObject();
      this.winningIndex = reader.in.readInt();
      this.winningPlayerId = (String) reader.in.readObject();
    } catch (Exception e) {
      e.printStackTrace();
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Error reading round state from datastore");
    }
  }
  
  public static HistoricRound retrieve(DatastoreService datastore, Key gameId, int index) throws StatusException {
    Key key = KeyFactory.createKey(gameId, kHistoricRoundStateKind, roundName(index));
    try {
      Entity entity = datastore.get(key);
      HistoricRound historic = new HistoricRound(entity);
      return historic;
      
    } catch (EntityNotFoundException e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Round " + index + " not found in datastore");
    }
  }
  
  public static HistoricRound[] retrieve(DatastoreService datastore, Key gameId, int first, int last) throws StatusException {
    ArrayList<HistoricRound> ret = new ArrayList<HistoricRound>();
    
    try {
      Query query = new Query(gameId).addSort(kRoundProperty, Query.SortDirection.ASCENDING);
      Iterable<Entity> historic = datastore.prepare(query).asIterable();
      for (Entity entity : historic) {
        if (((Integer) entity.getProperty(kRoundProperty)) >= first)
          ret.add(new HistoricRound(entity));
        if (((Integer) entity.getProperty(kRoundProperty)) > last)
          break;
      }
    } catch (Exception e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, e.getMessage());
    }
    return ret.toArray(new HistoricRound[ret.size()]);
  }
  
  public void store(DatastoreService datastore, Key gameId) throws StatusException{
    DatastoreHelpers.Writer writer = new DatastoreHelpers.Writer();
    try {
      writer.out.writeInt(this.blackCard);
      writer.out.writeObject(this.selections);
      writer.out.writeInt(this.winningIndex);
      writer.out.writeObject(this.winningPlayerId);
    } catch (IOException e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Error while saving run state: " + e.getMessage());
    }
    Entity entity = writer.done(kHistoricRoundStateKind, roundName(this.roundNumber), gameId);
    entity.setProperty(kRoundProperty, this.roundNumber);
    datastore.put(entity);
  }
  
  public JSONObject toJSON(final Map<String, Player> players) {
    JSONObject ret = new JSONObject();
    ret.put("round", this.roundNumber);
    ret.put("black", this.blackCard);
    ret.put("winner", this.winningIndex);
    ret.put("name", players.get(this.winningPlayerId).name);
    JSONArray answers = new JSONArray();
    for (int[] cards : selections)
      answers.put(new JSONObject().put("cards", new JSONArray(cards)));
    ret.put("answers", answers);
    return ret;
  }
}
