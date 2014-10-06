/// The "card table". Where card selection and voting happens.

import 'package:polymer/polymer.dart';

@CustomTag('cah-table')
class TableElement extends PolymerElement {
  @published String gameId;
  @published String playerId;

  // TODO(hjfreyer): Cache these.
  @observable List<String> whiteCards;
  @observable List<String> blackCards;

  @observable var handResponse;

  TableElement.created() : super.created();
}
