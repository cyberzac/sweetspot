package bitbonanza
package sweetspot

import java.io._
import java.nio.file.{Path, Paths}
import java.time.{LocalDate, Year}
import java.util.concurrent.TimeUnit
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult, Materializer}
import akka.stream.scaladsl.{FileIO, Sink, _}
import akka.util.ByteString
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.matching.Regex
import scala.util.{Success, Try}

object Import extends App {
  val sugarFile = new File("./src/main/resources/sugar.csv")
  val table = {
    var m: Map[String, String] = Map.empty
    if (sugarFile.exists()) {
      val reader = new BufferedReader(new FileReader(sugarFile))
      try {
        var line = reader.readLine()
        while (line != null) {
          val i = line.indexOf(',')
          val j = line.lastIndexOf(',')
          m += line.substring(0, i) → line.substring(j + 1)
          line = reader.readLine()
        }
      } finally reader.close()
    }
    m
  }
  println(s"Loaded ${table.size} articles")
  val re =
    """(?ms).*?<artikel>.*?<nr>(.+?)</nr>.*?<Namn>(.+?)</Namn>.*?<Varugrupp>(.+?)</Varugrupp>.*?<Sortiment>(.+?)</Sortiment>.*?"""
        .r
  val pattern = re.pattern
  val productsFile = Products.file.get
  def normalize(str: String): String =
    java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
        .toLowerCase
        .replaceAll("\\p{InCombiningDiacriticalMarks}+|[^a-z0-9 -]", "")
        .replaceAll("\\s+", "-")
  def scanner = new java.util.Scanner(productsFile).useDelimiter("\\s*</artikel>\\s*")
  val browser = JsoupBrowser()
  def sugar(url: String): Option[String] = Try {
    println(url)
    val doc = browser.get(url)
    import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
    import net.ruippeixotog.scalascraper.dsl.DSL._
    doc >> text("li:contains(Sockerhalt)>p")
  }.toOption
  def group(s: String): Option[String] = {
    if (s.startsWith("Rött vin")) Some("roda-viner")
    else if (s.startsWith("Röda")) Some("roda-viner")
    else if (s.startsWith("Vitt vin")) Some("vita-viner")
    else if (s.startsWith("Vita")) Some("vita-viner")
    else if (s.startsWith("Mousserande vin")) Some("mousserande-viner")
    else if (s.startsWith("Rosé")) Some("roseviner")
    else if (s.startsWith("Glögg")) Some("aperitif-dessert")
    else if (s.startsWith("Fruktvin")) Some("aperitif-dessert")
    else if (s.startsWith("Aperitif")) Some("aperitif-dessert")
    else if (s.startsWith("Portvin")) Some("aperitif-dessert")
    else if (s.startsWith("Sherry")) Some("aperitif-dessert")
    else if (s.startsWith("Sake")) Some("aperitif-dessert")
    else if (s.startsWith("Madeira")) Some("aperitif-dessert")
    else if (s.startsWith("Montilla")) Some("aperitif-dessert")
    else if (s.startsWith("Vermouth")) Some("aperitif-dessert")
    else if (s.startsWith("Smaksatt vin")) Some("aperitif-dessert")
    else if (s.startsWith("Vin av flera typer")) Some("aperitif-dessert")
    else if (s.startsWith("Övrigt starkvin")) Some("aperitif-dessert")
    else if (s.startsWith("Cider")) Some("cider-och-blanddrycker")
    else if (s.startsWith("Blanddrycker")) Some("cider-och-blanddrycker")
    else if (s.startsWith("Alkoholfritt")) Some("alkoholfritt")
    else if (s.startsWith("Gin")) Some("sprit")
    else if (s.startsWith("Rom")) Some("sprit")
    else if (s.startsWith("Cognac")) Some("sprit")
    else if (s.startsWith("Calvados")) Some("sprit")
    else if (s.startsWith("Armagnac")) Some("sprit")
    else if (s.startsWith("Bitter")) Some("sprit")
    else if (s.startsWith("Genever")) Some("sprit")
    else if (s.startsWith("Grappa")) Some("sprit")
    else if (s.startsWith("Punsch")) Some("sprit")
    else if (s.startsWith("Tequila")) Some("sprit")
    else if (s.startsWith("Whisky")) Some("sprit")
    else if (s.startsWith("Drinkar och Cocktails")) Some("sprit")
    else if (s.startsWith("Brandy")) Some("sprit")
    else if (s.startsWith("Okryddad sprit")) Some("sprit")
    else if (s.startsWith("Kryddad sprit")) Some("sprit")
    else if (s.startsWith("Aniskryddad sprit")) Some("sprit")
    else if (s.startsWith("Smaksatt sprit")) Some("sprit")
    else if (s.startsWith("Sprit av frukt")) Some("sprit")
    else if (s.startsWith("Likör")) Some("sprit")
    else if (s.startsWith("Övrig sprit")) Some("sprit")
    else None
  }
  val writer = new PrintWriter(new FileWriter(sugarFile, true))
  var count = 0
  var reloadedCount = 0
  val i = new ScannerIterator(scanner, re)
  i.filter { xs ⇒
    val nr = xs(0)
    /*
    val day = (nr(1) - '0') * 10 + (nr(2) - '0')
    val reload = LocalDate.now().getDayOfMonth == (day % 31 + 1)
    if (reload) {
      println("reload: " + xs.zipWithIndex.mkString(" "))
      reloadedCount += 1
    }
    (!table.contains(xs(0)) || reload) && !xs(3).startsWith("BS") && group(xs(2)).nonEmpty
    */
    !table.contains(xs(0)) && !xs(3).startsWith("BS") && group(xs(2)).nonEmpty
  }.take(90000).map { xs ⇒
    val id = xs(0)
    val name = normalize(xs(1))
    val category = group(xs(2)).get
    id → s"http://www.systembolaget.se/dryck/$category/$name-$id"
  }.map { t ⇒
    (t._1, t._2, sugar(t._2).getOrElse("-"))
  }.foreach {
    case (id, url, sugar) ⇒
      writer.println(s"""$id,"$url","$sugar"""")
      if (count % 100 == 0) writer.flush()
      count += 1
  }
  writer.close()
  //println("Fetched " + reloadedCount + " existing articles")
  println("Fetched " + count + " new articles")
}
class ScannerIterator(scanner: java.util.Scanner, re: Regex) extends Iterator[List[String]] {
  override def hasNext: Boolean = scanner.hasNext(re.pattern)
  override def next(): List[String] = re.unapplySeq(scanner.next(re.pattern)).get
}


