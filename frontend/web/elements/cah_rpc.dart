/// Wrapper for core-ajax specifically for doing RPCs to the backend.

import 'dart:convert';

import 'package:polymer/polymer.dart';

import "../config.dart";

@CustomTag('cah-rpc')
class RpcElement extends PolymerElement {
  @published bool auto = false;
  @published String action;
  @published Map<String, String> params;
  @published var response;

  RpcElement.created() : super.created();

  void attached() {
    if (auto) {
      go();
    }
  }

  void go() {
    var rpc = $['rpc'];
    rpc.url = RPC_ENDPOINT;

    var internalParams = {};
    if (action != null) {
      internalParams['action'] = action;
    }

    params.forEach((k, v) {
      internalParams[k] = v;
    });

    rpc.params = JSON.encode(internalParams);
    rpc.go();
  }
}
