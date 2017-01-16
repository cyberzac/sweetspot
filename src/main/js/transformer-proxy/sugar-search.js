'use strict';

var http = require('http'),
  connect = require('connect'),
  httpProxy = require('http-proxy'),
  transformerProxy = require('../');

var transformerFunction = function (data, req, res) {
  return data;
};


//var proxiedPort = 3000;
var proxyPort = 8001;

var app = connect();
//var proxy = httpProxy.createProxyServer({target: 'http://localhost:' + proxiedPort});
var proxy = httpProxy.createProxyServer({
  //target: 'http://localhost:9000'
  target: 'http://www.systembolaget.se',
  agent: http.globalAgent,
  headers: { host: 'www.systembolaget.se' }
})

app.use(transformerProxy(transformerFunction));

app.use(function (req, res) {
  proxy.web(req, res);
});

http.createServer(app).listen(proxyPort);

/*
//
// A simple server which will be proxied.
//

http.createServer(function (req, res) {
  res.writeHead(200, {'Content-Type': 'text/html'});
  res.write('<html><head></head><body>A simple HTML file</body></html>');
  res.end();
}).listen(proxiedPort);


console.log('The proxied server listens on', proxiedPort);
console.log('The proxy server listens on', proxyPort);
*/
