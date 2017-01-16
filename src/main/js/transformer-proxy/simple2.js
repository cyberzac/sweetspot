'use strict';

var http = require('http'),
  connect = require('connect'),
  httpProxy = require('http-proxy'),
  transformerProxy = require('../');

//
// The transforming function.
//

function replacer(match, prefix, id, postfix) {
  var sugarLevel = map
  return prefix + id + postfix + ', Sockerhalt: <3 g/l"';
}
var transformerFunction = function (data, req, res) {
  //console.log('kalle!')
  return ('' + data).replace(/("ProductUrl":"\/.*?-)(\d+)(",.*?"ProductInfo2":".[^"]+)"/g, replacer);
};


//
// A proxy as a basic connect app.
//

var proxyPort = 8013;

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

http.createServer(app).listen(proxyPort);

console.log('The proxy server listens on', proxyPort);
