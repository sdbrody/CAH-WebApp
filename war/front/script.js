var app = angular.module('myApp', []);

cah = {};

// **************** Services *******************/
cah.services = {};

cah.services.parseParams = function() {
	var params = unescape(location.search);
	if (params.length > 0) {
		// remove '?'
		params = params.substr(1);
	}
	var parts = params.split('&');
	var paramMap = {};
	for (var i = 0; i < parts.length; ++i) {
		var index = parts[i].indexOf('=');
		if (index >= 0) {
			var key = parts[i].substring(0, index);
			var value = parts[i].substr(index + 1);
			paramMap[key] = value;
		}
	}
	return paramMap;
};

cah.services.HttpService = function(http, errorCallback) {
  this.http = http;
  var params = cah.services.parseParams();
  this.gid = params['gameid'];
  this.pid = params['playerid'];
  this.errorCallback = errorCallback;
};

cah.services.HttpService.prototype.get = function(action, callback) {
	var pidStr = (typeof this.pid == 'undefined' ? '' : '&playerid=' + this.pid);	
	this.http.get('/cahwebapp?' + 'gameid=' + this.gid + pidStr + '&action=' + action)
	.success(function(data) {
		callback(data);
	}).
	error(this.errorCallback);
};

cah.services.HttpService.prototype.newGame = function(callback) {
	this.http.get('/cahwebapp')
	.success(function(data) {
		this.gid = data.gameid;
		callback(data);
	}.bind(this)).
	error(this.errorCallback);
}

// **************** Controllers *******************/

cah.controllers = {};

cah.controllers.Hand = function(httpService) {
  this.httpService = httpService;
  this.selection = [];
  this.data = {};
  this.update();
  this.isVisible = true;
  this.isSubmitted = false;
};

cah.controllers.Hand.prototype.isSelected = function(x) {
  return this.selection.indexOf(x) >= 0;
};

cah.controllers.Hand.prototype.selectionIndex = function(x) {
  var index = this.selection.indexOf(x);
  if (index >= 0 && this.data.numExpected > 1)
	  return (index + 1) + '. ';
  return '';
};

cah.controllers.Hand.prototype.getClass = function(x) {
  if (this.isSelected(x)) return 'selected';
  else return '';
};

cah.controllers.Hand.prototype.select = function(x) {
  var index = this.selection.indexOf(x);
  if (index == -1) {
    // not already selected
    if (this.selection.length == this.data.numExpected)
      this.selection.splice(0, 1)
    this.selection.push(x);

  } else {
    this.selection.splice(index, 1);
  }
};

cah.controllers.Hand.prototype.submit = function() {
  if (this.selection.length != this.data.numExpected) {
	alert('Please select ' + this.data.numExpected);
	return;
  }
  // post/get selection
  this.httpService.get('move&cards=' + this.selection, function(data) {
    this.update();
    this.isVisible = false;
    this.isSubmitted = true;
  }.bind(this));
  // wait for update
};

cah.controllers.Hand.prototype.update = function() {
  this.httpService.get('gethand', function(data) {
    this.data = data;
    this.selection = [];
    console.log('hand updated');
    console.log(data);
  }.bind(this));
};

cah.controllers.Hand.prototype.prepSelect = function() {
	this.update();
	this.isVisible = true;
    this.isSubmitted = false;
}

cah.controllers.Hand.prototype.toggleVisibility = function() {
	this.isVisible = !this.isVisible; 
}

cah.controllers.Hand.prototype.setVisibility = function(vis) {
	this.isVisible = vis;
}


// **************** Round *******************/
cah.controllers.Round = function(httpService, callback) {
  this.httpService = httpService;
  this.callback = callback;
  this.data = { moved: 0, round: 0 };
  this.update();
};

cah.controllers.Round.prototype.update = function() {
  this.httpService.get('getround', function(data) {
    var old = this.data;
    this.data = data;
    if (data.phase != old.phase)
      this.callback(data.phase);
    window.setTimeout(this.update.bind(this), 2000);
  }.bind(this));
};

// **************** History *****************/

cah.controllers.History = function(httpService) {
	this.httpService = httpService;
	this.prev = {};
	this.all = [];
	this.visible = false;
}

cah.controllers.History.prototype.update = function(round) {
	if (round < 2) return;
	this.httpService.get('gethistory&round=' + (round -1), function (data) {
	  this.all.push(this.prev);	
	  this.prev = data;
	}.bind(this));
}

// **************** Table *******************/
cah.controllers.Table = function(httpService) {
  this.httpService = httpService;
  this.data = {};
  this.clear();
};

cah.controllers.Table.prototype.clear = function() {
  this.data.answers = [];
  this.vote = -1;
  this.isVoted = false;
}

cah.controllers.Table.prototype.update = function() {
  this.httpService.get('getvote', function(data) {
    this.data = data;
  }.bind(this));
};

cah.controllers.Table.prototype.isSelected = function(x) {
  return this.vote == x.id;
};

cah.controllers.Table.prototype.getClass = function(x) {
  if (this.isSelected(x)) return 'selected';
  else return '';
};

cah.controllers.Table.prototype.select = function(x) {
  this.vote = x.id;
};

cah.controllers.Table.prototype.submit = function() {
  // post/get selection
  if (this.vote < 0) {
	  alert('Please Select Card');
	  return;
  }
  this.httpService.get('vote&vote=' + this.vote, function(data) {
    this.isVoted = true;
  }.bind(this));
  // wait for update
};

//************** Scores **************************************** /
cah.controllers.Scores = function(httpService) {
	this.httpService = httpService;
	this.data = []
}

cah.controllers.Scores.prototype.update = function() {
	this.httpService.get('getscores', function(data) {
		this.data = data;
	}.bind(this));
}

//**************  Join/Create Game ***************************** /

cah.controllers.PreGame = function(httpService) {
	this.httpService = httpService;
	this.name = '';
	this.email = '';
	this.gameStarted = false;
}

cah.controllers.PreGame.prototype.register = function(registrationCallback) {
	var nameStr = (this.name == '' ? '' : '&name=' + this.name);
	var emailStr = (this.email == '' ? '' : '&email=' + this.email);
	this.httpService.get('register&type=web&watcher=false' + nameStr + emailStr, function(data) { 
		console.log('player ' + this.name + ' registered');
		this.name = '';
		this.email = '';
		registrationCallback(data);
	}.bind(this));
} 

cah.controllers.PreGame.prototype.newGame = function() {
	this.httpService.newGame(function(gameData) {
		// gid already set
		// wait 3 sec. then register player (owner)
		console.log(this.httpService);
		window.setTimeout(this.register(
				function(playerData) {
					this.httpService.pid = playerData.playerid;
					// wait a sec. then start polling
					window.setTimeout(this.startPolling.bind(this), 1000);
				}.bind(this)),
				3000);  // new player (owner) callback
	}.bind(this));  // new game callback
}

cah.controllers.PreGame.prototype.startPolling = function() {
	this.httpService.get('getconfig', function(configData) {
	    this.players = configData.players;
	    window.setTimeout(this.startPolling.bind(this), 5000);
	}.bind(this));
}

app.controller('JoinCtrl', function($scope, $http, $location) {	
	$scope.handleError = function(data, status, headers, config) {
		$scope.status = status;
		var message = data.match(/<title>(.*)/);
		if (message.length > 1)
			alert(message[1]);
		else
			alert(message);
	}
	
	$scope.httpService = new cah.services.HttpService($http, $scope.handleError);
	
	$scope.newGame = (typeof $scope.httpService.gid == 'undefined');
	$scope.pageType = function() {
		if ($scope.newGame) {
			if (typeof $scope.httpService.gid == 'undefined') {
				return 'create';
			} else {
				return 'add';
			}
		} else {
			return 'join';
		}
	}
	
	$scope.state = new cah.controllers.PreGame($scope.httpService);
	
	if (!$scope.newGame) {
		$scope.state.startPolling();
	}
	
	$scope.joinUrl = function () {
		return unescape(location.href) + '?gameid=' + $scope.httpService.gid;
	}
	
	$scope.createNew = function() {
		$scope.state.newGame();
	}
		
	$scope.add = function() {
		$scope.state.register(function(data){});
	}
	
	$scope.join = function() {
		$scope.state.register(function (data) {
			window.setTimeout(function() { window.location = '/front/main.html?gameid=' + $scope.httpService.gid + '&playerid=' + data.playerid; }, 1000);
		})
	}
	
	$scope.start = function() {
		$scope.httpService.get('start', function(data) {
			window.setTimeout(function() { window.location = '/front/main.html?gameid=' + $scope.httpService.gid + '&playerid=' + $scope.httpService.pid; }, 1000);
		});
	}
});

// **************  Main ***************************** /

app.controller('HandCtrl', function($scope, $http, $location) {
  var params = cah.services.parseParams();
  console.log(params['gameid']);
  console.log(params['playerid']);
  // Load white cards.
  $http({
    method: 'GET',
    url: '../data/white_cards.json'
  }).
  success(function(data, status) {
    $scope.whiteCards = data;
  }).
  error(function(data, status) {
    $scope.status = status;
  });

  // Load black cards.
  $http({
    method: 'GET',
    url: '../data/black_cards.json'
  }).
  success(function(data, status) {
    $scope.blackCards = data;
  }).
  error(function(data, status) {
    $scope.status = status;
  });

  // Create components.
  $scope.http_ = $http;

  $scope.handleError = function(data, status, headers, config) {
    $scope.status = status;
    var message = data.match(/<title>(.*)/);
    if (typeof message != 'undefined' && message.length > 1)
    	alert(message[1]);
    else
    	alert(message);
  }
  $scope.httpService = new cah.services.HttpService($http, $scope.handleError);
  
  $scope.gameStarted = function() {
	  return typeof $scope.hand != 'undefined'; 
  }
  
  // Called when round state changes.
  $scope.notify = function(phase) {
	  console.log('notify ' + phase);
	  if (!$scope.gameStarted() && $scope.round.data.round > 0) {
		  // shift to started
		  $scope.hand = new cah.controllers.Hand($scope.httpService);
		  $scope.table = new cah.controllers.Table($scope.httpService);
		  $scope.scores = new cah.controllers.Scores($scope.httpService);
		  $scope.history = new cah.controllers.History($scope.httpService);
	  }	  
	  
	  if (phase == 'SELECTION') {
		  $scope.table.clear();
		  $scope.hand.prepSelect();
		  $scope.scores.update();
		  $scope.history.update($scope.round.data.round);
	  }

	  if (phase == 'VOTING')
		  $scope.table.update();
  };
  
  $scope.initRound = function() {
	  $scope.round = new cah.controllers.Round(
			  $scope.httpService, $scope.notify);
  };
  
  
  // Load game configuration and start polling rounds
  $scope.httpService.get('getconfig', function(data) {
    $scope.config = data;
    $scope.initRound();
  });
 
});