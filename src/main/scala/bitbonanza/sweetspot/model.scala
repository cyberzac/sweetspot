package bitbonanza
package sweetspot

import java.io.File
import java.time.{LocalDate, Year}
import scala.util.{Success, Try}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing, Sink}
import akka.util.ByteString

case class Article(
    id: Int, articleId: Int, productId: Int,
    name: String, subName: Option[String] = None,
    price: Int, reclaim: Option[Int] = None,
    volume: Int, pricePerLitre: Int,
    salesStart: LocalDate, salesEnd: Option[LocalDate] = None,
    productGroup: Option[String] = None, packaging: Option[String] = None, seal: Option[String] = None,
    origin: Option[String] = None, country: String,
    producer: String, supplier: String,
    vintage: Option[Year] = None, vintageTested: Option[Year] = None,
    alcoholPercentage: Int, assortment: String,
    ecological: Option[Boolean] = None, kosher: Option[Boolean] = None,
    description: Option[String] = None,
    sugarLevel: Option[Int] = None) //extends Entity { type Id = Int }
object Article {
  def apply(xs: List[String]): Try[Article] = Try {
    Article(
      id = xs(0).toInt,
      articleId = xs(1).toInt,
      productId = xs(2).toInt,
      name = xs(3),
      subName = Option(xs(4)),
      price = xs(5).replace(".", "").toInt,
      reclaim = Option(xs(6)).map(_.replace(".", "").toInt),
      volume = xs(7).replace(".", "").toInt,
      pricePerLitre = xs(8).replace(".", "").toInt,
      salesStart = LocalDate.parse(xs(9)),
      salesEnd = Option(xs(10)).map(s => LocalDate.parse(s)),
      productGroup = Option(xs(11)),
      packaging = Option(xs(12)),
      seal = Option(xs(13)),
      origin = Option(xs(14)),
      country = xs(15),
      producer = xs(16),
      supplier = xs(17),
      vintage = Option(xs(18)).map(s => Year.parse(if (s.length == 2) "20" + s else s)),
      vintageTested = Option(xs(19)).map(s => Year.parse(s)),
      alcoholPercentage = xs(20).replace('%', '0').replace(".", "").toInt,
      assortment = xs(21),
      ecological = Option(xs(22)).map(_ != "0"),
      kosher = Option(xs(23)).map(_ != "0"),
      description = Option(xs(24)),
      sugarLevel = None)
  }
}
object Lister extends App {
  implicit val system = ActorSystem("sweetspot")
  implicit val materializer = ActorMaterializer()
  val articlesUrl = "http://www.systembolaget.se/api/assortment/products/xml"
  def withArticles(f: Article => Unit) = {
  }
  def articles = {
    val ArticleRe =
      """<artikel><nr>(\d+)</nr><Artikelid>(\d+)</Artikelid><Varnummer>(\d+)</Varnummer><Namn>([^<]+)</Namn><Namn2(?:(?: ?/>)|(?:>([^<]+)</Namn2>))<Prisinklmoms>(\d+\.\d{2})</Prisinklmoms>(?:<Pant>([^<]+)</Pant>)?<Volymiml>(\d+\.\d{2})</Volymiml><PrisPerLiter>(\d+\.\d{2})</PrisPerLiter><Saljstart>(\d+-\d\d-\d\d)</Saljstart><Slutlev(?:(?: ?/>)|(?:>(\d+-\d\d-\d\d)</Slutlev>))<Varugrupp>([^<]+)</Varugrupp><Forpackning>([^<]+)</Forpackning><Forslutning(?:(?: ?/>)|(?:>([^<]+)</Forslutning>))<Ursprung(?:(?: ?/>)|(?:>([^<]+)</Ursprung>))<Ursprunglandnamn>([^<]+)</Ursprunglandnamn><Producent>([^<]+)</Producent><Leverantor>([^<]+)</Leverantor><Argang(?:(?: ?/>)|(?:>(\d+)</Argang>))<Provadargang(?:(?: ?/>)|(?:>([^<]+)</Provadargang>))<Alkoholhalt>(\d+\.\d+%)</Alkoholhalt><Sortiment>([^<]+)</Sortiment><Ekologisk>(\d+)</Ekologisk><Koscher>(\d+)</Koscher>(?:<RavarorBeskrivning(?:(?: ?/>)|(?:>([^<]+)</RavarorBeskrivning>)))?</artikel>"""
          .r.unanchored
    //val articlesFile = new File("src/main/resources/products-2016-02-09.xml")
    val articlesFile = new File("config/products-2016-02-09.xml")
    FileIO.
        fromPath(articlesFile.toPath).
        via(Framing.delimiter(ByteString("</artikel>"), maximumFrameLength = 4096, allowTruncation = true)).
        map(_.utf8String).
        map(s => ArticleRe.findFirstMatchIn(s + "</artikel>")).
        collect {
          case Some(m) => Article.apply(m.subgroups)
        }.
        collect {
          case Success(article) => article
        }.to(Sink.ignore)
  }
  withArticles(println)
}
