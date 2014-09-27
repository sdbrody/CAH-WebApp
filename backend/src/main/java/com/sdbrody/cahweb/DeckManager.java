package com.sdbrody.cahweb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class DeckManager {
  public static final String kDeckKind = "Deck";
  public static final String kDeckName = "Deck";  
  
  private ArrayList<Integer> whiteCards;
  private ArrayList<BlackCard> blackCards;
  private int whiteTop;
  private int blackTop;
  

  /*
   * @numCards is an array with the number of white cards in cell 0, number of black cards
   * with one slot in cell 1, number of black cards with two slots in cell 2, etc. 
   */
  public DeckManager(int[] numCards) {
    System.out.println("white cards:" + numCards[0]);
    whiteCards = new ArrayList<Integer>(numCards[0]);
    for (int i = 0; i < numCards[0]; ++i) {
      whiteCards.add(i);
    }
    restartDeck(false);
    
    blackCards = new ArrayList<BlackCard>();
    int id = 0;
    for (int numSlots = 1; numSlots <  numCards.length; ++numSlots) {
      System.out.print("black cards with one slot: " + id);
      for (int j = 0; j < numCards[numSlots]; ++j, ++id) {
        blackCards.add(new BlackCard(id, numSlots));
      }
      System.out.println(" - " + (id - 1));
    }
    restartDeck(true);
  }

  @SuppressWarnings("unchecked")
  private DeckManager(DatastoreService datastore, Key gameId) throws StatusException {
    Key key = KeyFactory.createKey(gameId, kDeckKind, kDeckName);
    Entity entity;
    try {
      entity = datastore.get(key);
    } catch (EntityNotFoundException e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Deck not found in datastore");
    }
    DatastoreHelpers.Reader reader = new DatastoreHelpers.Reader(entity);
    try {
      this.blackCards = (ArrayList<BlackCard>) reader.in.readObject();
      this.blackTop = reader.in.readInt();
      this.whiteCards = (ArrayList<Integer>) reader.in.readObject();
      this.whiteTop = reader.in.readInt();
    } catch (Exception e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Error reading round state from datastore");
    }
  }
  
  public static DeckManager retrieve(DatastoreService datastore, Key gameId) throws StatusException {
    return new DeckManager(datastore, gameId);
  }
  
  public void store(DatastoreService datastore, Key gameId) throws StatusException {
    DatastoreHelpers.Writer writer = new DatastoreHelpers.Writer();
    try {
      writer.out.writeObject(this.blackCards);
      writer.out.writeInt(this.blackTop);
      writer.out.writeObject(this.whiteCards);
      writer.out.writeInt(this.whiteTop);
    } catch (IOException e) {
      throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, "Error while saving deck: " + e.getMessage());
    }
    datastore.put(writer.done(kDeckKind, kDeckName, gameId));
  }
  
  public BlackCard getBlackCard() throws StatusException {
    if (blackTop < 0)
      throw new StatusException(StatusException.StatusType.ILLEGAL, "Black card deck is empty");
    // else
    
    return blackCards.get(blackTop--);
  }

  public void draw(HashSet<Integer> hand, int num_cards) throws StatusException {
    //boolean noshuffle = true;
    if (whiteTop < num_cards - 1) {
      //Log.d("DeckManager", "Reshuffling white deck");  
      //restartDeck(false);
      //noshuffle = false;
      throw new StatusException(StatusException.StatusType.BAD_INPUT, "Draw from deck: not enough cards");
    }
    // else
    for (int i = 0; i < num_cards; ++i) {
      hand.add(whiteCards.get(whiteTop--));
    }
  }

  public void discard(HashSet<Integer> hand, final int[] cards) throws StatusException {
    for (int card : cards) {
      if (!hand.remove(card)) {
        throw new StatusException(StatusException.StatusType.BAD_INPUT, "DeckManager: Requested to remove card " + card + " from hand of player, but it wasn't there!");
      }
    }
  }

  // Returns false if had to reshuffle deck.
  public void drawToSize(HashSet<Integer> hand, int size) throws StatusException {
    if (hand.size() >= size) {
      throw new StatusException(StatusException.StatusType.BAD_INPUT, "DeckManager: Requested to fill hand of size " + hand.size() + " to " +size);  
    }
    draw(hand, size - hand.size());
  }

  public void swap(HashSet<Integer> hand, final int[] cards) throws StatusException {
    if (whiteTop < cards.length - 1)
      throw new StatusException(StatusException.StatusType.BAD_INPUT, "Asked to swap to many cards - white deck is empty");
    // else
    discard(hand, cards);
    draw(hand, cards.length);
  }

  public void restartDeck(boolean isBlack) {
    if (isBlack) {
      Collections.shuffle(blackCards);
      blackTop = blackCards.size() - 1;
    } else {
      Collections.shuffle(whiteCards);
      whiteTop = whiteCards.size() - 1;
    }
  }
}
