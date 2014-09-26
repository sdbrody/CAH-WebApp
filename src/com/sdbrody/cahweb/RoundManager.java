package com.sdbrody.cahweb;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class RoundManager {
  public static enum RoundPhase {
    SELECTION, VOTING, ROUND_DONE
  }
  public static final String kRoundStateKind = "RoundState";
  public static final String kRoundStateName = "RoundState";
  
  public RoundManager() {
    this.roundNumber = 0;
    this.blackCard = new BlackCard(-1, 0);
    this.hands = new HashMap<String, HashSet<Integer>>();
    this.scores = new HashMap<String, Integer>();
    clearRound();
  }  
  
  private void clearRound() {
    this.selections = new HashMap<String, int[]>();
    this.index = new HashMap<Integer, String>();
    this.votes = new HashMap<String, Integer>();
  }
  
  @SuppressWarnings("unchecked")
  private RoundManager(DatastoreService datastore, Key gameId) throws StatusException {
    Key key = KeyFactory.createKey(gameId, kRoundStateKind, kRoundStateName);
    Entity entity;
    try {
      entity = datastore.get(key);
    } catch (EntityNotFoundException e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Round state not found in datastore");
    }
    DatastoreHelpers.Reader reader = new DatastoreHelpers.Reader(entity);
    try {
      this.roundNumber = reader.in.readInt();
      this.blackCard = new BlackCard(reader.in.readInt(), reader.in.readInt());
      this.hands = (Map<String, HashSet<Integer>>) reader.in.readObject();
      this.selections = (Map<String, int[]>) reader.in.readObject();
      this.index = (Map<Integer, String>) reader.in.readObject();
      this.votes = (Map<String, Integer>) reader.in.readObject();
      this.scores = (Map<String, Integer>) reader.in.readObject();
    } catch (Exception e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Error reading round state from datastore");
    }
  }
  
  public static RoundManager retrieve(DatastoreService datastore, Key gameId) throws StatusException {
    return new RoundManager(datastore, gameId);
  }
  
  public void store(DatastoreService datastore, Key gameId) throws StatusException{
    DatastoreHelpers.Writer writer = new DatastoreHelpers.Writer();
    try {
      writer.out.writeInt(roundNumber);
      writer.out.writeInt(blackCard.id);
      writer.out.writeInt(blackCard.numSlots);
      writer.out.writeObject(hands);
      writer.out.writeObject(selections);
      writer.out.writeObject(index);
      writer.out.writeObject(votes);
      writer.out.writeObject(scores);
    } catch (IOException e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Error while saving run state: " + e.getMessage());
    }
    datastore.put(writer.done(kRoundStateKind, kRoundStateName, gameId));
  }

  public void setHand(String pid, HashSet<Integer> hand) {
    this.hands.put(pid, hand);
  }

  private void createIndex() {
    ArrayList<String> playerList = new ArrayList<String>(selections.keySet());
    Collections.shuffle(playerList);
    for (int i = 0; i < playerList.size(); ++i)
      index.put(i, playerList.get(i));
  }
  
  public void addSelection(String pid, int[] selection) throws StatusException {
    if (!hands.containsKey(pid))
      throw new StatusException(StatusException.StatusType.BAD_INPUT, "Selection attempt from non-existent player " + pid);
    
  
    if (selections.containsKey(pid))
      throw new StatusException(StatusException.StatusType.ILLEGAL, "Player " + pid + " already selected for round " + roundNumber);
        
    if (selection.length != blackCard.numSlots)
      throw new StatusException(StatusException.StatusType.ILLEGAL, "Selection is " + selection.length + " cards, but expected " + blackCard.numSlots);
    
    for (int card : selection)
      if (!hands.get(pid).contains(card))
        throw new StatusException(StatusException.StatusType.ILLEGAL, "Selection contains card number " + card + ", but player is not holding it");
    
    selections.put(pid, selection);
    if (selections.size() == hands.size())
      createIndex();
  }
  
  public void addVote(String pid, int vote) throws StatusException {
    if (!hands.containsKey(pid))
      throw new StatusException(StatusException.StatusType.BAD_INPUT, "Vote attempt from non-existent player " + pid);
    
  
    if (votes.containsKey(pid))
      throw new StatusException(StatusException.StatusType.ILLEGAL, "Player " + pid + " already voted for round " + roundNumber);
    
    votes.put(pid, vote);
  }
  
  public RoundPhase getPhase() {
    // TODO: adapt for judge and voting passes
    if (selections.size() < hands.size())
      return RoundPhase.SELECTION;
    if (votes.size() < hands.size())
      return RoundPhase.VOTING;
    return RoundPhase.ROUND_DONE;
  }
  
  private String getWinner() {
    int[] votesCount = new int[hands.size()];
    Arrays.fill(votesCount, 0);
    for (int index : votes.values())
      ++votesCount[index];
    
    int argmax = 0;
    // Breaks ties by first index, but indexes are random.
    for (int i = 1; i < votesCount.length; ++i) {
      if (votesCount[i] > votesCount[argmax]) {
        argmax = i;
      }  
    }
    return index.get(argmax);
  }
  
  // Hands must be initialized first.
  public void firstRound(BlackCard black) throws StatusException {
    if (roundNumber != 0)
      throw new StatusException(StatusException.StatusType.ILLEGAL, "Attempted to start game that is already in progress");
    clearRound();
    for (String pid : hands.keySet())
      scores.put(pid, 0);
    ++roundNumber;
    this.blackCard = black; 
  }
  
  public HistoricRound nextRound(BlackCard black) throws StatusException {
    if (getPhase() != RoundPhase.ROUND_DONE)
      throw new StatusException(StatusException.StatusType.ILLEGAL, "Attempted to move to next round when in phase " + getPhase() + " of round " + getRoundNumber());
    
    String winner = getWinner();
    HistoricRound historic = new HistoricRound(this, winner);
    scores.put(winner, scores.get(winner) + 1);
    clearRound();
    ++roundNumber;
    this.blackCard = black;    
    return historic; 
  }
  
  // get vote selection for player (pid)
  public Map<Integer, int[]> getVoteSelectionForPlayer(String pid) throws StatusException {
    // TODO: invalid player
    // TODO: if not vote time, return null;
    Map<Integer, int[]> ret = new HashMap<Integer, int[]>();
    for (Map.Entry<Integer, String> entry : index.entrySet()) {
      if (entry.getValue() != pid) {
        ret.put(entry.getKey(), selections.get(entry.getValue()));
      }
    }
    return ret;
  }
  
  public JSONObject voteSelectionToJson(Map<Integer, int[]> selection) {
    JSONObject ret = new JSONObject().
        put("black", blackCard.id);
    ArrayList<JSONObject> answersArray = new ArrayList<JSONObject>();
    for (Map.Entry<Integer, int[]> entry : selection.entrySet()) {
      JSONObject selectionJson = new JSONObject().
          put("id", entry.getKey()).
          put("cards", new JSONArray(entry.getValue()));
      
      answersArray.add(selectionJson);
    }
    ret.put("answers", answersArray);
    return ret;
  }

  
  public HashSet<Integer> getHandForPlayer(String pid) throws StatusException {
    if (!hands.containsKey(pid))
      throw new StatusException(StatusException.StatusType.BAD_INPUT,
          "Requested hand for non-existent player " + pid);
    return hands.get(pid);
  }
  
  
  public JSONObject handToJson(HashSet<Integer> hand) {
    JSONObject ret = new JSONObject().
        put("black", blackCard.id).
        put("numExpected", blackCard.numSlots).
        put("white", new JSONArray(hand));
    
    return ret;
  }
  
  public final Map<String, Integer> getScores() {
    return scores;
  }
  
  public JSONArray scoresToJSon(final Map<String, Player> players) {
    JSONArray ret = new JSONArray();
    for (Map.Entry<String, Integer> entry : this.scores.entrySet()) {
      ret.put(new JSONObject().put("name", players.get(entry.getKey()).name).put("score", entry.getValue()));
    }
    return ret;
  }
  
  public int getRoundNumber() {
    return roundNumber;
  }
  
  public final BlackCard getBlackCard() {
    return blackCard;
  }

  public final Map<String, int[]> getSelections() {
    return selections;
  }

  public final Map<Integer, String> getIndex() {
    return index;
  }

  public final Map<String, Integer> getVotes() {
    return votes;
  }
  
  public JSONObject toJSON() {
    return new JSONObject().
        put("round", roundNumber).
        put("phase", getPhase().toString()).
        put("blackCard", blackCard.id).
        put("moved", selections.size()).
        put("voted", votes.size());
  }
  
  public String toString() {
    return toJSON().toString();
  }
  
  private int roundNumber;
  private BlackCard blackCard;
  private Map<String, HashSet<Integer>> hands;
  private Map<String, int[]> selections;
  private Map<Integer, String> index;
  private Map<String, Integer> votes;
  private Map<String, Integer> scores;
}
