/// Lobby element which users use to give their name, see who has joined, and
/// start the game (if owner).

import 'dart:async';
import 'dart:html';
import 'package:polymer/polymer.dart';

@CustomTag('cah-lobby')
class LobbyElement extends PolymerElement {
  // TODO(hjfreyer): Abstract polling into a subelement.
  Timer pollTimer;

  @published String gameId;
  @published bool owner;

  @observable String protocol;
  @observable String host;

  @observable String name;
  @observable String playerId;

  @observable var configResponse;
  @observable var roundResponse;

  LobbyElement.created(): super.created() {
    pollTimer = new Timer.periodic(new Duration(seconds: 1), poll);
    protocol = window.location.protocol;
    host = window.location.host;
  }

  void detached() {
    pollTimer.cancel();
  }

  void joinGame(e) {
    $['joinRpc'].go();
    e.preventDefault();
  }

  void joined(e) {
    playerId = e.detail['response']['playerid'];
  }

  void poll(timer) {
    $['configRpc'].go();
    $['roundRpc'].go();
    print('poll');
  }

  void start() {
    $['startRpc'].go();
  }

  void roundResponseChanged() {
    if (roundResponse['blackCard'] != -1 && playerId != null) {
      print('started');
      fire('game-started', detail: {
        'gameId': gameId,
        'playerId': playerId,
      });
    }
  }
}
