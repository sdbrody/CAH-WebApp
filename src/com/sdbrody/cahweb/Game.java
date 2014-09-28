package com.sdbrody.cahweb;

import java.util.ArrayList;
import java.util.HashSet;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Key;
import com.sdbrody.cahweb.Player.PlayerType;

public class Game {
  protected DatastoreService datastore;
  protected final Key id;

  /*
   * @numCards is an array with the number of white cards in cell 0, number of black cards
   * with one slot in cell 1, number of black cards with two slots in cell 2, etc. 
   */
  public void create(GameConfiguration config, int[] numCards) throws StatusException {
    if (numCards.length < 2) {
      throw new StatusException(StatusException.StatusType.BAD_INPUT,
          "Cards array must have at least two numbers - white and black cards. Saw " + numCards);
    }
    config.store(datastore, id);
    
    (new DeckManager(numCards)).store(datastore, id);
    
    (new RoundManager()).store(datastore, id);
  }

  public Game(DatastoreService datastore, final Key id) {
    this.datastore = datastore;
    this.id = id;
  }
  
  public void registerPlayer(String pid, String name, PlayerType type, boolean isPassive) throws StatusException {
    RoundManager round = RoundManager.retrieve(datastore, id);
    
    if (round.getRoundNumber() != 0)
      throw new StatusException(StatusException.StatusType.ILLEGAL, "Attempted to register player " + name + " after game started");
    
    GameConfiguration config = GameConfiguration.retrieve(datastore, id);
    
    boolean isOwner = (config.getPlayers().size() == 0);
    config.AddPlayer(pid, new Player(name, type, isPassive, isOwner));
    
    config.store(datastore, id);
  }

  public void startGame(String ownerId) throws StatusException {
    GameConfiguration config = GameConfiguration.retrieve(datastore, id);
    
    Player player = config.getPlayers().get(ownerId);
    
    if (player == null)
      throw new StatusException(StatusException.StatusType.BAD_INPUT, "Nonexistent player " +  ownerId + " tried to start game " + id);
    
    if (!player.isOwner)
      throw new StatusException(StatusException.StatusType.ILLEGAL, "Player " +  player.name + " (not owner) tried to start game " + id);
       
    // TODO: check state and conditions
    
    if (config.players.size() < 3)
      throw new StatusException(StatusException.StatusType.ILLEGAL, "Can't start game with less than three players.");
        
    DeckManager deck = DeckManager.retrieve(datastore, id);
    
    RoundManager round = RoundManager.retrieve(datastore, id);
    
    if (round.getRoundNumber() != 0)
      throw new StatusException(StatusException.StatusType.ILLEGAL, "Tried to start game " + id + 
          " while already in progress (round " + round.getRoundNumber());
    
    
    BlackCard black = deck.getBlackCard();
    
    // init hands
    for (String pid : config.getPlayers().keySet()) {
      HashSet<Integer> hand = new HashSet<Integer>();
      deck.draw(hand, config.cardsPerHand);
      round.setHand(pid, hand);
    }
    
    round.firstRound(black);
    
    // store state, deck (hands)
    round.store(datastore, id);
    
    deck.store(datastore, id);
  }

  
  public final RoundManager getRound() throws StatusException {
    // get round
    return RoundManager.retrieve(datastore, id);
  }

  
  public HistoricRound getHistoricRound(int index) throws StatusException {
    return HistoricRound.retrieve(datastore, id, index);
  }
  
  public final HistoricRound[] getHistory(int first, int last) throws StatusException {
    return HistoricRound.retrieve(datastore, id, first, last);
  }

  public ArrayList<BlackCard> getHand(String player) {
    // get deckman
    return null;
  }
  
  public final GameConfiguration getConfig() throws StatusException {
    return GameConfiguration.retrieve(datastore, id);
  }
}
