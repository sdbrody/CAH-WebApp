/// Root element. Handles parsing URL, routing to subpages,

import 'package:polymer/polymer.dart';

import 'package:route/client.dart';

final allUrls = [homeUrl, articleUrl];

class Page {
  final String name;
  final UrlPattern pattern;

  /// Any subgroups in the pattern will be inserted into [params] using these
  /// names.
  final List<String> paramNames;

  Page(this.name, patternRE, this.paramNames)
    : pattern = new UrlPattern(patternRE);
}

final index = new Page('INDEX', r'/#/', []);
final newGame = new Page('NEW_GAME', r'/#/create', []);
final joinGame = new Page('JOIN_GAME', r'/#/g/(.+)/join', ['gameId']);
final table = new Page('TABLE', r'/#/g/(.+)/play/(.+)', ['gameId', 'playerId']);

final pages = [index, newGame, joinGame, table];
final title = 'Inhumane Cards';

@CustomTag('cah-app')
class CahAppElement extends PolymerElement {
  final Router router = new Router(useFragment: true);

  @observable String pageName;
  @observable Map<String, String> params = {};

  CahAppElement.created()
    : super.created() {
    for (var p in pages) {
      router.addHandler(p.pattern, (path) {
        pageName = p.name;
        params = {};
        var parsed = p.pattern.parse(path);
        for (int i = 0; i < p.paramNames.length; i++) {
          params[p.paramNames[i]] = parsed[i];
        }
      });
    }

    router
      ..addHandler(new UrlPattern('/'), (p) => router.gotoPath('/#/', TITLE))
      ..listen();
  }

  void gameStarted(event) {
    var d = event.detail;
    router.gotoUrl(TABLE.pattern, [d['gameId'], d['playerId']], TITLE);
  }
}
