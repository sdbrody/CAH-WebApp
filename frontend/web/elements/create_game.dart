import 'dart:async';
import 'dart:convert';

import 'package:polymer/polymer.dart';

@CustomTag('create-game')
class CreateGameElement extends PolymerElement {
  @observable String gameId;

  CreateGameElement.created() : super.created();

  onCreated(e) => gameId = e.detail['response']['gameid'];
}
