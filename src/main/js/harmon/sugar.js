var fs = require('fs'),
    http = require('http'),
    https = require('https'),
    connect = require('connect'),
    httpProxy = require('http-proxy'),
    util = require('util'),
    trumpet = require('trumpet');

function clean(str) {
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
//console.log('map: ' + util.inspect(map));
//fs.closeSync(fd);

/*
var insertAfter = function (node, data) {
  //Create a read/write stream wit the inner option
  //so we get the contents of the tag and we can append to it
  var stm = node.createStream();
  //variable to hold all the info from the data events
  var existingData = '';
  //collect all the data in the stream
  stm.on('data', function(d) {
     existingData += d;
  });
  //When the read side of the stream has ended..
  stm.on('end', function() {
    //Now on the write side of the stream write some data using .end()
    //N.B. if end isn't called it will just hang.
    stm.end(existingData + data);
  });
}
*/
var insertAfter = function (stm, data) {
  //variable to hold all the info from the data events
  var existingData = '';
  //collect all the data in the stream
  stm.on('data', function(d) {
     existingData += d;
  });
  //When the read side of the stream has ended..
  stm.on('end', function() {
    //Now on the write side of the stream write some data using .end()
    //N.B. if end isn't called it will just hang.
    stm.end(existingData + data);
  });
}

var selects = [];
var aSelect = {};
aSelect.query = 'li.elm-product-list-item-full span.product-info-2';
aSelect.func = function (span) {
    //node.setAttribute('style', 'background-color: blue;');
    //node.createWriteStream().end('<span>Sockerhalt: &lt; 3 g/l</span>');
    insertAfter(span.createStream(), '<span class="sugarInfo">{{resultItem.SugarInfo}}</span>');
}


/*
sugarSelect.query = 'li.elm-product-list-item-full span.product-info-2';
sugarSelect.func = function (node) {
    //node.setAttribute('style', 'background-color: blue;');
    //node.createWriteStream().end('<span>Sockerhalt: &lt; 3 g/l</span>');
    node.getAttribute();
    insertAfter(node, ', <span>Sockerhalt: &lt;3 g/l</span>');
}
*/

selects.push(aSelect);

//
// Basic Connect App
//
var app = connect();

var proxy = httpProxy.createProxyServer({
  //target: 'http://localhost:9000'
  target: 'http://www.systembolaget.se',
  agent: http.globalAgent,
  headers: { host: 'www.systembolaget.se' }
})


app.use(require('../')([], selects, true));

app.use(
  function (req, res) {
    proxy.web(req, res);
  }
);

http.createServer(app).listen(8000);

/*
http.createServer(function (req, res) {
  res.writeHead(200, { 'Content-Type': 'text/html' });
  res.write('<html><head></head><body><div class="a">Nodejitsu Http Proxy</div><div class="b">&amp; Frames</div></body></html>');
  res.end();
}).listen(9000);
*/
