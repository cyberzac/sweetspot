package bitbonanza
package sweetspot

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try
import scala.util.matching.Regex
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Host, Referer}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}

trait Service { self: Database =>
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  val targetHost = "www.systembolaget.se"
  val targetPort = 80
  lazy val flow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnection(targetHost, 80)

  def sendRequest(request: HttpRequest): Future[HttpResponse] =
    Source.single(request).via(flow).runWith(Sink.head)
  def mapRequest(request: HttpRequest): HttpRequest =
    request
      .withUri(request.uri.withHost(targetHost).withPort(targetPort))
      .mapHeaders(_.collect {
        case h @ HttpHeader("host", _) => Host(targetHost, targetPort)
        case h @ HttpHeader("referer", v) if !Option(v).forall(_.trim.isEmpty) =>
          Referer(Uri(v).withHost(targetHost).withPort(targetPort))
        case h => h
      })

  def colorize(level: Double): String = {
    // green = 0, 255, 0
    // yellow = 255, 255, 0
    // red = 255, 0, 0
    val min = 3.0
    val mid = 4.0
    val high = 10.0
    val value = math.min(math.max(level - min, 0.0), high)
    val (red, green, blue) =
      if (value <= mid) (((value / mid) * 255).toInt, 255, 0)
      else (255, (255 - ((value - mid) / mid) * 255).toInt, 0)
    s"rgb($red,$green,$blue)"
  }
  def classify(literal: String): (String, String, Boolean) = {
    val cleaned = literal.replace(" g/l", "").takeWhile(_ != ',')
    val lessThan = cleaned.charAt(0) == '<'
    val number = cleaned.replace("<", "")
    val value = number.replace("<", "").takeWhile(_.isDigit).toInt
    val sugarClass =
      if (value <= 3 && lessThan) "sugar-2"
      else if (value < 10) "sugar-" + value
      else if (value < 20) "sugar-10"
      else if (value < 50) "sugar-20"
      else if (value < 75) "sugar-50"
      else if (value < 100) "sugar-75"
      else "sugar-100"
    ("burst-12 " + sugarClass, number, lessThan)
  }

  def mapProductSearchJsonResponse(response: HttpResponse): Future[HttpResponse] =
    response.entity.toStrict(10.seconds).flatMap { entity =>
      Unmarshal(entity).to[String].map { data =>
        val fixed =
          """("ProductNumber": *"(\d+)",.*?)("BeverageDescriptionShort": *"[^"]+)",""".r
            .replaceAllIn(
              data,
              m => {
                val numberLiteral = m.group(1)
                val id = Try(m.group(2).toInt).toOption.getOrElse(0)
                val desc = m.group(3)
                val sugarInfo = sugar(id)
                  .map { sugarLiteral =>
                    val (sugarClass, sugarLevel, sugarLessThan) = classify(sugarLiteral)
                    s""", Sockerhalt: $sugarLiteral", "SugarLevel": "$sugarLevel", "SugarClass": "$sugarClass", "SugarLessThan": "$sugarLessThan", """
                  }
                  .getOrElse("\",")
                val result = s"""$numberLiteral$desc$sugarInfo"""
                result
              }
            )
        response.withEntity(entity.copy(data = ByteString(fixed)))
      }
    }

  val burst12 =
    """
      |.burst-12 {
      |    width: 40px;
      |    height: 40px;
      |    position: absolute;
      |    text-align: center;
      |    z-index: 20;
      |    right: 140px;
      |    top: 27px;
      |}
      |li.is-detailed-view .burst-12 {
      |    top: 27px;
      |}
      |li.is-pictures-view .burst-12 {
      |    top: 308px;
      |    right: 19px;
      |}
      |div.product-header .burst-12 {
      |    top: 68px;
      |}
      |.burst-12:before, .burst-12:after {
      |    content: "";
      |    position: absolute;
      |    top: 0;
      |    left: 0;
      |    height: 40px;
      |    width: 40px;
      |}
      |.burst-12 > div {
      |    position: absolute;
      |    top: 7px;
      |    color: white;
      |    text-align: center;
      |    width: 100%;
      |    z-index: 21;
      |    font-size: 20px;
      |    font-weight: bold;
      |}
      |.burst-12:before {
      |    -webkit-transform: rotate(30deg);
      |       -moz-transform: rotate(30deg);
      |        -ms-transform: rotate(30deg);
      |         -o-transform: rotate(30deg);
      |}
      |.burst-12:after {
      |    -webkit-transform: rotate(60deg);
      |       -moz-transform: rotate(60deg);
      |        -ms-transform: rotate(60deg);
      |         -o-transform: rotate(60deg);
      |}
      |.sugar-2, .sugar-2:before, .sugar-2:after {
      |    background-color: #00b200;
      |}
      |.sugar-3, .sugar-3:before, .sugar-3:after {
      |    background-color: #5fb200;
      |}
      |.sugar-4, .sugar-4:before, .sugar-4:after {
      |    background-color: #7fb200;
      |}
      |.sugar-5, .sugar-5:before, .sugar-5:after {
      |    background-color: #b2b200;
      |}
      |.sugar-6, .sugar-6:before, .sugar-6:after {
      |    background-color: #f8b200;
      |}
      |.sugar-7, .sugar-7:before, .sugar-7:after {
      |    background-color: #ffab00;
      |}
      |.sugar-8, .sugar-8:before, .sugar-8:after {
      |    background-color: #ff7200;
      |}
      |.sugar-9, .sugar-9:before, .sugar-9:after {
      |    background-color: #ff3800;
      |}
      |.sugar-10, .sugar-10:before, .sugar-10:after {
      |    background-color: #ff0000;
      |}
      |.sugar-20, .sugar-20:before, .sugar-20:after {
      |    background-color: #b20000;
      |}
      |.sugar-50, .sugar-50:before, .sugar-50:after {
      |    background-color: #7f0000;
      |}
      |.sugar-75, .sugar-75:before, .sugar-75:after {
      |    background-color: #4c0000;
      |}
      |.sugar-100, .sugar-100:before, .sugar-100:after {
      |    background-color: #250000;
      |}
      """.stripMargin
  def mapProductSearchHtmlResponse(response: HttpResponse): Future[HttpResponse] = {
    val flow = Flow[ByteString]
      .via(Gzip.decoderFlow)
      .map { bytes =>
        val data = bytes.utf8String.replace("</head>", s"<style>$burst12</style>\n</head>")
        val fixed = """(<li class="elm-product-list-item-full".*?)>""".r.replaceAllIn(
          data,
          m => {
            val prefix = m.group(1)
            //val result = s"""$prefix style="{{resultItem.SugarStyle}}"><div data-ng-if="resultItem.SugarSign" class="{{resultItem.SugarClass}}"><div>{{resultItem.SugarSign}}</div></div>"""
            val result =
              s"""$prefix><div data-ng-if="resultItem.SugarLevel" class="{{resultItem.SugarClass}}"><div><span data-ng-if="resultItem.SugarLessThan" style="font-size: 50%;vertical-align: 25%;">&lt;&nbsp;</span>{{resultItem.SugarLevel}}<span data-ng-if="resultItem.SugarLessThan" style="visibility:hidden;font-size: 50%;vertical-align: 25%;">&lt;&nbsp;</span></div></div>"""
            Regex.quoteReplacement(result)
          }
        )
        ByteString(fixed)
      }
      .via(Gzip.encoderFlow)
    val transformedEntity = response.entity.transformDataBytes(flow)
    transformedEntity.toStrict(10.seconds).map(response.withEntity)
  }
  def mapProductHtmlResponse(response: HttpResponse): Future[HttpResponse] = {
    val flow = Flow[ByteString]
      .via(Gzip.decoderFlow)
      .map { bytes =>
        val data = bytes.utf8String.replace("</head>", s"<style>$burst12</style>\n</head>")
        val fixed =
          """(?s)("ProductNumber": *"(\d+)".*?<div class="product-header">)""".r.replaceAllIn(
            data,
            m => {
              val prefix = m.group(1)
              val id = Try(m.group(2).toInt).toOption.getOrElse(0)
              val result = sugar(id)
                .map {
                  sugarLiteral =>
                    val (sugarClass, sugarLevel, sugarLessThan) = classify(sugarLiteral)
                    if (sugarLessThan)
                      s"""$prefix<div class="$sugarClass"><div><span style="font-size: 50%;vertical-align: 25%;">&lt;&nbsp;</span>$sugarLevel<span style="visibility:hidden;font-size: 50%;vertical-align: 25%;">&lt;&nbsp;</span></div></div>"""
                    else s"""$prefix<div class="$sugarClass"><div>$sugarLevel</div></div>"""
                }
                .getOrElse("\",")
              Regex.quoteReplacement(result)
            }
          )
        ByteString(fixed)
      }
      .via(Gzip.encoderFlow)
    val transformedEntity = response.entity.transformDataBytes(flow)
    transformedEntity.toStrict(10.seconds).map(response.withEntity)
  }

  lazy val handler: HttpRequest => Future[HttpResponse] = {
    case request @ HttpRequest(GET, Uri.Path(path), _, _, _)
        if path.startsWith("/api/productsearch/search") =>
      sendRequest(mapRequest(request)).flatMap(mapProductSearchJsonResponse)
    case request @ HttpRequest(GET, Uri.Path(path), _, _, _) if path.startsWith("/sok-dryck") =>
      sendRequest(mapRequest(request)).flatMap(mapProductSearchHtmlResponse)
    case request @ HttpRequest(GET, Uri.Path(path), _, _, _) if path.startsWith("/dryck/") =>
      sendRequest(mapRequest(request)).flatMap(mapProductHtmlResponse)
    case request: HttpRequest =>
      sendRequest(mapRequest(request))
  }
}
object Proxy extends App with Service with Database {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandleAsync(
    handler,
    config.getString("http.interface"),
    config.getInt("http.port"))
}
