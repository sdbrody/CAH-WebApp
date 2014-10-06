/// NOTE(hjfreyer): It may seem like this element can simply be noscripted, if
/// you do that the initialization order gets messed up. Shrug.

import 'dart:async';
import 'dart:convert';

import 'package:polymer/polymer.dart';

@CustomTag('join-game')
class JoinGameElement extends PolymerElement {
  @observable String gameId;

  JoinGameElement.created() : super.created();
}
