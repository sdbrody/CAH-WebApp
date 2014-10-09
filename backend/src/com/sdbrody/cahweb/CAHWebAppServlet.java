package com.sdbrody.cahweb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import net.sf.json.JSONObject;
import org.json.JSONObject;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import org.apache.commons.codec.binary.Base64;
import com.sdbrody.cahweb.GameConfiguration.VoteMode;
import com.sdbrody.cahweb.Player.PlayerType;
import com.sdbrody.cahweb.RoundManager.RoundPhase;

@SuppressWarnings("serial")
public class CAHWebAppServlet extends HttpServlet {
	
  private int[] CreateDecks(final Map<String, String[]> params) throws StatusException{
    int b2 = 0;
    if (params.containsKey("b2")) b2 = getInt(params, "b2");
    int b3 = 0;
    if (params.containsKey("b3")) b3 = getInt(params, "b3"); 
    return new int[]{ getInt(params, "w"),  getInt(params, "b1"), b2, b3};
  }
  
	// Handlers
	// new game Post
  public void handleNewGame(DatastoreService datastore, String gameIdStr, int[] numCards, PrintWriter response) throws StatusException {
    System.out.println("Attempting create new game: " + gameIdStr);

    Key gameId = KeyFactory.createKey("Game", gameIdStr);
    GameConfiguration config = new GameConfiguration(10, VoteMode.VOTE, false,
        false);

    new Game(datastore, gameId).create(config, numCards);
    
    response.println(new JSONObject().put("gameid", gameIdStr).toString());
    // TODO: invites?
  }
	
	public boolean notify(GameConfiguration config, String action) {
	  ChannelService channelService = ChannelServiceFactory.getChannelService();
	  
	  for (final Player player : config.players.values()) {
	    System.out.println("notifying " + player.name + ": " + action);
	    if (player.channel != null) {
	      channelService.sendMessage(new ChannelMessage(player.channel, action));
	    }
	  }
	  return true;
	}
	
	private Boolean getBooleanWithDefault(final Map<String, String[]> params, String key, boolean defaultVal) throws StatusException {
	  if (!params.containsKey(key))
	    return new Boolean(defaultVal);
	  
	  String str = getString(params, key, true);
	  
	  if ("true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)) {
	    return new Boolean(new Boolean(str));
	  }
	  
	  throw new StatusException(StatusException.StatusType.BAD_INPUT, "Expected boolean (\"true\" or \"false\"), got " + str);
	}
	
	private Integer getInt(final Map<String, String[]> params, String key) throws StatusException {
	  String str = getString(params, key, true);
    try {
      return new Integer(str);
    } catch (Exception e) {
      throw new StatusException(StatusException.StatusType.BAD_INPUT, "Error parsing value of " + key);
    }
	}
	
	private String getString(final Map<String, String[]> params, String key, boolean lower) throws StatusException {
	  if (!params.containsKey(key))
	    throw new StatusException(StatusException.StatusType.BAD_INPUT, "Parameter " + key + " not specified!");
	  if (params.get(key).length != 1)
	    throw new StatusException(StatusException.StatusType.BAD_INPUT, "Multiple values for parameter " + key);
	  if (lower)
	    return new String(params.get(key)[0].toLowerCase());
	  else
	    return new String(params.get(key)[0]);
	}
	
	private String getAction(final Map<String, String[]> params) throws StatusException {
	  if (!params.containsKey("gameid"))
	    return new String("newgame");
	  return getString(params, "action", true);
	}
	
	private String randomKey() {
	  byte[] bytes = new byte[4];
	  new Random().nextBytes(bytes);
	  return Base64.encodeBase64URLSafeString(bytes);
	}
		
	private void handlePuts(String gameIdStr, Map<String, String[]> params, DatastoreService datastore, String action, PrintWriter response)  throws StatusException {
	  Key gameId = KeyFactory.createKey("Game", gameIdStr);
	  
	  // Handle puts
    switch(action) {
    case "newgame":
      int[] numCards = CreateDecks(params);
      System.out.println("Starting game with numCards = " + Arrays.toString(numCards));
      handleNewGame(datastore, gameIdStr, numCards, response);
      return;
      
    case "register":
      Player.PlayerType type = null;
      String typeStr = getString(params, "type", true);
      
      switch (typeStr) {
      case "web" : type = PlayerType.WEB; break;
      case "mobile" :
      case "android" : type = PlayerType.MOBILE; break;
      default :
        throw new StatusException(StatusException.StatusType.ILLEGAL, "bad player type : " + typeStr);
      }
      
      String pid = new String("P" + randomKey());
      Boolean isPassive = getBooleanWithDefault(params, "watcher", false);
      
      String name = getString(params, "name", false);
      
      ChannelService channelService = ChannelServiceFactory.getChannelService();

      String channel = channelService.createChannel(gameIdStr + ":" + pid);
      
      new Game(datastore, gameId).registerPlayer(pid, name, type, isPassive, channel);
      
      response.println(new JSONObject().put("playerid", pid).put("channel", channel).toString());
      return;
      
    case "start":
      pid = getString(params, "playerid", false);
      new Game(datastore, gameId).startGame(pid);
      return;
      
    case "move":
      pid = getString(params, "playerid", false);
      
      String selectionStr = getString(params, "cards", false);
      
      String[] parts = selectionStr.split(",");
      int[] selection = new int[parts.length];
      for (int i = 0; i < parts.length; ++i) {
        try {
          selection[i] = Integer.parseInt(parts[i]);
        } catch(NumberFormatException e) {
          throw new StatusException(StatusException.StatusType.BAD_INPUT, "Bad specification of selection: " + selectionStr);
        }
      }
                
      Game game = new Game(datastore, gameId);
                
      GameConfiguration config = game.getConfig();
      
      RoundManager round = game.getRound();
      
      if (round.getPhase() != RoundPhase.SELECTION)
        throw new StatusException(StatusException.StatusType.ILLEGAL, "move request outside of selection phase");      
      
      Player player = config.getPlayers().get(pid);
      if (player == null) throw new StatusException(StatusException.StatusType.BAD_INPUT, "player " + pid + "does not exist");
      if (player.isPassive) throw new StatusException(StatusException.StatusType.ILLEGAL, "Passive player " + pid + " attempted to move");
      
      round.addSelection(pid, selection);
      
      DeckManager deck = DeckManager.retrieve(datastore, gameId);
      
      HashSet<Integer> hand = round.getHandForPlayer(pid);
      deck.discard(hand, selection);
      deck.drawToSize(hand, config.cardsPerHand);
      
      /*if (round.getPhase() == RoundPhase.VOTING) {
        if (!notify(config, RoundPhase.VOTING))
          System.err.println("notification error");
      }*/
      
      round.store(datastore, gameId);
      
      deck.store(datastore, gameId);
      return;      
      
    case "vote":
      pid = getString(params, "playerid", false);
      
      String voteStr = getString(params, "vote", false);
      
      int vote;
      try {
        vote = Integer.parseInt(voteStr);
      } catch(NumberFormatException e) {
        throw new StatusException(StatusException.StatusType.BAD_INPUT, "Bad specification of vote: " + voteStr);
      }
                
      game = new Game(datastore, gameId);
                
      config = game.getConfig();
      
      round = game.getRound();
      
      if (round.getPhase() != RoundPhase.VOTING)
        throw new StatusException(StatusException.StatusType.ILLEGAL, "move request outside of voting phase");      
      
      player = config.getPlayers().get(pid);
      if (player == null) throw new StatusException(StatusException.StatusType.BAD_INPUT, "player " + pid + "does not exist");
      if (player.isPassive) throw new StatusException(StatusException.StatusType.ILLEGAL, "Passive player " + pid + " attempted to vote");
      
      round.addVote(pid, vote);
      
      deck = DeckManager.retrieve(datastore, gameId);
      
      if (round.getPhase() == RoundPhase.ROUND_DONE) {
        BlackCard black = deck.getBlackCard();
        // TODO: game over!!!
          
        HistoricRound historic = round.nextRound(black);
        
        historic.store(datastore, gameId);
        
        // update scores
        /*if (!notify(config, RoundPhase.VOTING)) {
          System.err.println("notification error");
        }*/
      }
      
      round.store(datastore, gameId);
      
      deck.store(datastore, gameId);
      
      return;
    
    default:
      throw new StatusException(StatusException.StatusType.BAD_INPUT, "Action " + action + " is not valid");
    }
	}
	
	private void handleDispatch(String gameIdStr, Map<String, String[]> params, PrintWriter response) throws StatusException {
	  DatastoreService datastore =
        DatastoreServiceFactory.getDatastoreService();
	  String action = getAction(params);
	  
	  Key gameId = KeyFactory.createKey("Game", gameIdStr);
	  GameConfiguration config = null;
    
    System.out.println("Handling action " + action);
    
	  // *********************  Handle gets ************************
	  switch(action) {
	  case "getconfig":
	    config = new Game(datastore, gameId).getConfig();
	    response.println(config.toString());
	    return;
	    
	  case "getround" :
	    config = new Game(datastore, gameId).getConfig();
	    RoundManager round = new Game(datastore, gameId).getRound();
	    
	    response.println(round.getRoundStatus(config.getPlayers()));
	    return;
	  
	  case "getscores" :
	    config = new Game(datastore, gameId).getConfig();
	    round = new Game(datastore, gameId).getRound();
      
      response.println(round.scoresToJSon(config.getPlayers()).toString());
      return;
      
	  case "getchannel" :
	    config = new Game(datastore, gameId).getConfig();
	    String pid = getString(params, "playerid", false);
	    response.println(
	        new JSONObject().put("channel" , config.getPlayers().get(pid).channel).toString());
	    return;
	    
	  case "gethand" :
	    pid = getString(params, "playerid", false);
      
	    round = new Game(datastore, gameId).getRound();
      
      HashSet<Integer> hand = round.getHandForPlayer(pid);
      
      response.println(round.handToJson(hand));
      return;
      
	  case "getvote" :
	    pid = getString(params, "playerid", false);
      
      round = new Game(datastore, gameId).getRound();
      
      if (round.getPhase() != RoundPhase.VOTING)
        throw new StatusException(StatusException.StatusType.ILLEGAL, "Vote selection request outside of voting phase");      
      
      Map<Integer, int[]> voteSelection = round.getVoteSelectionForPlayer(pid);
      response.println(round.voteSelectionToJson(voteSelection));
      return;
     
	  case "gethistory" :
	    config = new Game(datastore, gameId).getConfig();
      Integer index = getInt(params, "round");
      
      HistoricRound historic = new Game(datastore, gameId).getHistoricRound(index);
      
      response.println(historic.toJSON(config.getPlayers()).toString());
      return;
      
	  default:
	    System.out.println("action " + action + " is not a get request");
	    // do nothing
	  }
	  
	  // *********************  Handle puts ***************************
	  
	  // move Post
	  // vote Post
	  // end/leave game? Post	  
	  
	  int retries = 3;
	  while (true) {
      Transaction txn = datastore.beginTransaction();
      try {
        handlePuts(gameIdStr, params, datastore, action, response);
        // end transaction
        txn.commit();
        System.out.println("Transaction successful!");
        config = new Game(datastore, gameId).getConfig();
        
        notify(config, action);
        return;
      } catch (ConcurrentModificationException e) {
        System.err.println("failed, " + retries + " left");
        if (retries == 0) {
          throw e;
        }
        // Allow retry to occur
        --retries;
      } catch (StatusException e) {
        throw e;
      } catch (Exception e) {
        throw new StatusException(StatusException.StatusType.DATASTORE_ERROR, e.getMessage());
      } finally {
        if (txn.isActive()) {
          txn.rollback();
        }
      }  // finally - end of try
    }  // end of while - all retries
	}  // end of handleDispatch
	
	@Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
	  resp.setContentType("text/plain");
	  resp.setHeader("Cache-control", "no-cache, no-store");
    resp.setHeader("Pragma", "no-cache");
    resp.setHeader("Expires", "-1");    
    
    resp.setHeader("Access-Control-Allow-Origin", "*");
    resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
    resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    resp.setHeader("Access-Control-Max-Age", "86400");
    
	  HttpResponseOutput respOutput = new HttpResponseOutput();
    StatusException error = null;
	  
	  @SuppressWarnings("unchecked")
    final Map<String, String[]> params = req.getParameterMap();
	  String gameIdStr;
    if (!params.containsKey("gameid")) {
      gameIdStr = "G" + randomKey();
    } else {
      gameIdStr = req.getParameter("gameid");
      if (gameIdStr == null) {
        error = new StatusException(StatusException.StatusType.BAD_INPUT, "No game ID provided");
      }
    }
    if (error == null) {
      try {
        handleDispatch(gameIdStr, params, respOutput.out);
      } catch (StatusException e) {
        error = e;
      }
    }
    if (error != null) 
      respOutput.err.println(error.toString());
    
    respOutput.setResponse(resp);
  }
}
