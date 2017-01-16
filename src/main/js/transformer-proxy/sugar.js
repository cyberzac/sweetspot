'use strict';

var fs = require('fs'),
    http = require('http'),
    https = require('https'),
    connect = require('connect'),
    httpProxy = require('http-proxy'),
    util = require('util'),
    trumpet = require('trumpet'),
    transformerProxy = require('transformer-proxy'),
    request = require('request');

var clean = function (str) {
  var start = 0;
  var end = str.length;
  if (str.charAt(0) == '"') start = start + 1;
  if (str.charAt(str.length - 1) == '"') end = end - 1;
  return str.substring(start, end);
}
var db = {};
//var fd = fs.openSync('sugar.csv', 'a+');
var contents = fs.readFileSync('sugar.csv', {encoding: 'utf8'});
var lines = contents.split('\n');
console.log('lines[0]: ' + lines[0]);
for (var i = 0; i < lines.length; i++) {
  var line = lines[i];
  //console.log('line: ' + line);
  var columns = line.split(',');
  if (columns.length == 3) {
    //console.log('columns: ' + columns);
    var id = columns[0];
    var url = clean(columns[1]);
    var sugar = clean(columns[2]);
    db[id] = sugar
    db[url] = sugar
  }
}

var app = connect();
var proxy = httpProxy.createProxyServer({
  target: 'http://www.systembolaget.se',
  agent: http.globalAgent,
  headers: { host: 'www.systembolaget.se' }
});


app.use(transformerProxy(transformerFunction, {match : /search\?/}));
app.use(function (req, res) {
  proxy.web(req, res);
});


//http://localhost:8003/api/productsearch/search?searchquery=caberne&sortdirection=Ascending&site=all&fullassortment=0&page=1&nofilters=1
/*
var sugarFilterAdder = function (match) {
  var sugarFilter = '{"Name":"sugarlevel","DisplayName":"Sockerhaltsnivå","Priority":2,"IsCategoryCard":true,"ModifierCount":7,"Children":[],"SearchModifiers":[{"Attribute":"sugarlevel","DisplayName":"<3 g/l","Value":"^<3$","Description":"Lägsta sockerhaltsnivå som finns angiven, denna sockerhalt påverkar inte kroppens blodsockernivå alls.","Count":916,"Category":null},{"Attribute":"sugarlevel","DisplayName":"3-5 g/l","Value":"^3-5$","Description":"Mycket låg sockerhalt, påverkan på kroppens blodsocker är minimal.","Count":0,"Category":null},{"Attribute":"sugarlevel","DisplayName":"6-9 g/l","Value":"^6-9$","Description":"Låg sockernivå, påverkan på kroppens blodsocker är marginell.","Count":355,"Category":null},{"Attribute":"sugarlevel","DisplayName":"10-15 g/l","Value":"^10-15$","Description":"Medelsockernivå, kan påverka kroppens blodsockernivå.","Count":338,"Category":null},{"Attribute":"sugarlevel","DisplayName":"16-30 g/l","Value":"^16-30$","Description":"Förhöjd sockernivå, påverkar blodsockernivå.","Count":186,"Category":null},{"Attribute":"sugarlevel","DisplayName":"31-80 g/l","Value":"^31-80$","Description":"Mycket hög sockernivå, påverkar blodsockernivån mycket lätt.","Count":186,"Category":null},{"Attribute":"sugarlevel","DisplayName":">81 g/l","Value":"^>81$","Description":"Extremt hög sockernivå, påverkar blodsockernivån på samma sätt som läsk.","Count":186,"Category":null}],"IsActive":false,"IsHidden":false,"IsMultipleChoice":true}'
  return '},' + sugarFilter + ',{"Name":"tastesymbolsnavigator"';
}
*/
var transformerFunction = function (data, req, res) {
  var sugarInfoAdder = function (match, prefix, id, postfix) {
    var sugarLevel = db[id];
    if (sugarLevel && sugarLevel != '-') {
      var match = /^(<?)(\d+) g\/l$/.exec(sugarLevel);
      var sugarLevelAmount = 0.0 + parseInt(match[2]) - ((match[1].length > 0) ? 0.5 : 0); // convert e.g. <3 g/l to 2.5
      var sugarText = (sugarLevel && sugarLevel != '-') ? ', Sockerhalt: ' + sugarLevel : '';
      var result = prefix + id + postfix + sugarText + '","SugarLevel":' + sugarLevelAmount;
      return result;
    } else return prefix + id + postfix + '"';
  }
  var d = ('' + data)
    .replace(/("ProductUrl":"\/.*?-)(\d+)(",.*?"ProductInfo2":".[^"]+)"/g, sugarInfoAdder);
    //.replace(/\},\{"Name":"tastesymbolsnavigator"/g, sugarFilterAdder);
  var json = JSON.parse(d);
  var results = json.ProductSearchResults;
  var count = results.length;
  for (var i = results.length; i--; ) { // Weirdly beautiful; when i-- reaches 0 it will halt the loop since 0 is interpreted as false :)
    if (results[i].ProductInfo2 && !results[i].SugarLevel) {
      results.splice(i, 1);
    }
  }
  if (results.length < count) {
    var pageIncrementer = function (match, page) {
      console.log(match)
      return 'page=' + (parseInt(page) + 1);
    }
    var nextPageUrl = req.url.replace(/page=(\d+)/, pageIncrementer)
    if (nextPageUrl.indexOf('page=') == -1) nextPageUrl = req.url + "&page=1";


    //req.pipe(request(url)).pipe(res)
    var nextPageRequest = Object.assign({}, req);
    req.url = nextPageUrl
    app(req, )
    console.log(req.url);
    console.log(nextPageUrl);
  }
  return JSON.stringify(json);
};


var proxyPort = 8003;
http.createServer(app).listen(proxyPort);
console.log('The proxy server listens on', proxyPort);


// Den här webbsajten är ett utökning av http://www.systembolaget.se. Den visar samma sidor och fungerar likadant, men lägger till information om sockerhalt i alla produktlistor. Dessa kan även filtreras så att endast produkter med en sockerhalt under en viss nivå visas.
