package bitbonanza
package sweetspot

import java.io.{File, FilenameFilter}
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent._
import scala.util.Try
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Framing, Sink}
import akka.util.ByteString


trait Database {
  //implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer
  def load: Map[Int, (String, String)] = {
    // 9509801,"http://www.systembolaget.se/dryck/roda-viner/chateau-la-conseillante-9509801","-"
    val Line =
      """(\d+),"([^"]+)","([^"]{2,})"""".r
    val file = new File("config/sugar.csv")
    //Http().singleRequest(HttpRequest(uri = articlesUrl)).foreach { response =>
    val f = FileIO.
        fromPath(file.toPath).
        via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 4096, allowTruncation = true)).
        map(_.utf8String).
        map(s => Line.findFirstMatchIn(s)).
        collect {
          case Some(m) if m.group(3) != null => (m.group(1).toInt, (m.group(2), m.group(3)))
        }.runWith(Sink.seq).map(_.toMap)
    val result = Await.result(f, Duration(5, TimeUnit.SECONDS))
    result
  }
  lazy val data: Map[Int, (String, String)] = load
  def link(id: Int): Option[String] = data.get(id).map(_._1)
  def sugar(id: Int): Option[String] = data.get(id).map(_._2)
}
/*
trait Entity {
  type Id
  def id: Id
}
object Query {
  sealed trait Condition {
    def not: Condition = Not(Seq(this))
    def and(condition: Condition): And = And(Seq(this, condition))
    def or(condition: Condition): Or = Or(Seq(this, condition))
  }
  case class Not(conditions: Seq[Condition]) extends Condition {
    override def not: Condition = this match {
      case Not(c :: Nil) ⇒ c
      case c ⇒ Not(Seq(c))
    }
  }
  case class And(conditions: Seq[Condition]) extends Condition {
    override def and(condition: Condition): And = this match {
      case And(cs) ⇒ And(cs :+ condition)
      case c ⇒ And(Seq(c, condition))
    }
  }
  case class Or(conditions: Seq[Condition]) extends Condition {
    override def or(condition: Condition): Or = this match {
      case Or(cs) ⇒ Or(cs :+ condition)
      case c ⇒ Or(Seq(c, condition))
    }
  }
  case class Exists(name: String) extends Condition
  case class EqualTo[T](name: String, value: T) extends Condition
  case class LessThan[T: Ordering](name: String, limit: T) extends Condition
  case class LessThanOrEqualTo[T: Ordering](name: String, limit: T) extends Condition
  case class GreaterThan[T: Ordering](name: String, limit: T) extends Condition
  case class GreaterThanOrEqualTo[T: Ordering](name: String, limit: T) extends Condition
  case class Between[T: Ordering](name: String, lower: T, upper: T, inclusive: Boolean = false) extends Condition
  case class Like(name: String, pattern: String) extends Condition
  case class Matches(name: String, literal: String) extends Condition
  case class In[T](name: String, items: Seq[T])
  def not(conditions: Condition*): Not = Not(conditions.toList)
  def and(conditions: Condition*): And = And(conditions.toList)
  def or(conditions: Condition*): Or = Or(conditions.toList)
  def exists(name: String): Exists = Exists(name)
  def equalTo[T](name: String, value: T): EqualTo[T] = EqualTo(name, value)
  def lessThan[T: Ordering](name: String, limit: T): LessThan[T] = LessThan(name, limit)
  def lessThanOrEqualTo[T: Ordering](name: String, limit: T): LessThanOrEqualTo[T] = LessThanOrEqualTo(name, limit)
  def greaterThan[T: Ordering](name: String, limit: T): GreaterThan[T] = GreaterThan(name, limit)
  def greaterThanOrEqualTo[T: Ordering](name: String, limit: T): GreaterThanOrEqualTo[T] = GreaterThanOrEqualTo(name, limit)
  def between[T: Ordering](name: String, lower: T, upper: T, inclusive: Boolean = false): Between[T] = Between(name, lower, upper, inclusive)
  def like(name: String, pattern: String): Like = Like(name, pattern)
  def matches(name: String, literal: String): Matches = Matches(name, literal)
  def in[T](name: String, items: T*): In[T] = In(name, items.toList)
}
case class Query(conditions: Seq[Query.Condition], pageSize: Option[Int] = None, pageIndex: Option[Int] = None)
trait Source[E <: Entity] {
  def fetch(id: E#Id): Future[Option[E]]
  def find(query: Query): Future[Seq[E]]
}
trait Sink[E <: Entity] {
  def persist(e: E): Future[E]
  def persist(es: Traversable[E])(implicit executor: ExecutionContext): Future[Traversable[E]] = Future.traverse(es)(persist)
  def delete(id: E#Id): Future[Int]
}
trait Schema[E <: Entity] {
  def create: Future[Unit]
  def drop: Future[Unit]
}
trait Repository[E <: Entity] extends Source[E] with Sink[E] with Schema[E] {
  override def create: Future[Unit] = sys
      .error(s"Create is not implemented for this repository [${this.getClass.getName}]")
  override def drop: Future[Unit] = sys.error(s"Drop is not implemented for this repository [${this.getClass.getName}]")
}
package sql {
  import java.sql.Connection

  trait SqlRepository[E <: Entity] extends Repository[E] {
    implicit abstract val connection: Connection
  }

  package scaslikejdbc {
    import scalikejdbc._
    //Class.forName("org.h2.Driver")

  }

  /*
  package relate {
    import java.lang.reflect.Type
    import java.sql.Types
    import scala.reflect.ClassTag
    import com.lucidchart.relate._

    abstract class RelateRepository[E <: Entity : ClassTag[E]] extends SqlRepository[E] {
      implicit abstract val parser: (SqlRow) ⇒ E
      def tableName: String = implicitly[ClassTag[E]].runtimeClass.getSimpleName
      def fields: Seq[(String, Type)] = {
        val clazz = implicitly[ClassTag[E]].runtimeClass
        val declaredFields = clazz.getDeclaredFields
        val types = for (field ← declaredFields)
          yield (field.getName, field.getGenericType)
        types
      }
      def columnDefs: Seq[(String, String)] = fields.map(t ⇒ t.copy(_2 = t._2.getTypeName)).map {
        case (name, "java.lang.String") ⇒ (name, "VARCHAR(255)")
        case (name, "java.lang.Integer") ⇒ (name, "INT")
        case (name, "java.lang.Option[java.lang.String]") ⇒ (name, "VARCHAR(255) NULL")
        case (name, t) ⇒ throw new IllegalArgumentException("Unknown field type [table: " + tableName + ", field: " + name + ", type: " + t + "].")
      }
      override def create: Future[Unit] = Future {
        sql"""
          CREATE TABLE $tableName (
            ${for ((columnName, columnDef) ← columnDefs)
              yield "$columnName: $columnDef"},
            PRIMARY KEY (id))
            """
        ()
      }
      override def drop: Future[Unit] = Future {
        sql"""
          DROP TABLE $tableName
        """.execute()
        ()
      }

      override def fetch(id: E#Id): Try[Option[E]] = ???
      override def persist(es: Traversable[E]): Try[E] = ???
      override def delete(id: E#Id): Try[Int] = ???


      override def persist(e: E): Try[E] = Try { ???
        /*
        sql"""
      MERGE INTO $tableName VALUES (
        ${e.id},
        ${e.articleId},
        ${e.productId},
        ${e.name},
        ${e.subName},
        ${e.price},
        ${e.reclaim},
        ${e.volume},
        ${e.pricePerLitre},
        ${e.salesStart},
        ${e.salesEnd},
        ${e.productGroup},
        ${e.packaging},
        ${e.seal},
        ${e.origin},
        ${e.country},
        ${e.producer},
        ${e.supplier},
        ${e.vintage},
        ${e.vintageTested},
        ${e.alcoholPercentage},
        ${e.assortment},
        ${e.ecological},
        ${e.kosher},
        ${e.description},
        ${e.sugarLevel})
        """.executeUpdate()
         */
      }
      override def delete(id: E#Id): Try[Int] = Try { ???
        /*
        sql"""
      DELETE
      FROM $tableName
      WHERE id = $id
        """.executeUpdate()
        */
      }
      override def fetch(id: Int): Try[Option[Article]] = Try {
        sql"""
      SELECT *
      FROM Article
      WHERE id = $id
        """.asSingleOption[Article]
      }
      /*
      private def build(combiner: String, conditions: Seq[Query.Condition]): InterpolatedQuery = {
        import Query._
        builder.append(" and ")
        for (condition ← conditions) {
          condition match {
            case Not(conditions) ⇒
              builder.append("not (")
              format(builder, "and", conditions)
              builder.append(")")
            case And(conditions) =>
              builder.append("and (")
              format(builder, "and", conditions)
              builder.append(")")
            case Or(conditions) =>
              builder.append(" or (")
              format(builder, "and", conditions)
              builder.append(")")
            case Nand(conditions) =>
              builder.append(" nand (")
              format(builder, "and", conditions)
              builder.append(")")
            case Nor(conditions) =>
              builder.append(" nor (")
              format(builder, "and", conditions)
              builder.append(")")
            case Xor(conditions) =>
              builder.append(" xor (")
              format(builder, "and", conditions)
              builder.append(")")
            case Exists(name) =>
              builder.append(" and ").append(name).append(" is not null")
            case EqualTo(name, value) =>
              builder.append(" and ").append(name).append(" = ").append(value)
            case NotEqualTo(value) =>
            case Null =>
            case NotNull =>
            case LessThan(limit) =>
            case LessThanOrEqualTo(limit) =>
            case GreaterThan(limit) =>
            case GreaterThanOrEqualTo(limit) =>
            case Between(lower, upper, inclusive) =>
            case Like(pattern) =>
            case Matches(literal) =>
            case In(items)) ⇒
            case NotIn(items)) ⇒
          }
        }
      }
      */
      override def find(query: Query): Try[Seq[Article]] = Try {
        sql"""
        SELECT *
        FROM Article
        WHERE id = $id
          """.asSingleOption[Article]
      }

    }
  }
  */

}

trait Articles extends Repository[Article] {
  import java.sql.Connection
  import com.lucidchart.relate._
  Class.forName("org.h2.Driver")
  implicit val connection: Connection = DriverManager.getConnection("jdbc:h2:mem:test")
  override def create: Try[Unit] = Try {
    sql"""
      CREATE TABLE Article (
        id: INT,
        articleId: INT,
        productId: INT,
        name: VARCHAR(255),
        subName: VARCHAR(244) NULL,
        price: INT,
        reclaim: INT NULL,
        volume: INT,
        pricePerLitre: INT,
        salesStart: TIMESTAMP,
        salesEnd: TIMESTAMP NULL,
        productGroup: VARCHAR(255) NULL,
        packaging: VARCHAR(255) NULL,
        seal: VARCHAR(255) NULL,
        origin: VARCHAR(255) NULL,
        country: VARCHAR(255),
        producer: VARCHAR(255),
        supplier: VARCHAR(255),
        vintage: INT NULL,
        vintageTested: INT NULL,
        alcoholPercentage: INT,
        assortment: VARCHAR(255),
        ecological: INT NULL,
        kosher: INT NULL,
        description: VARCHAR(255) NULL,
        sugarLevel: INT NULL,
        PRIMARY KEY (id))
        """.execute()
    ()
  }
  override def drop: Try[Unit] = Try {
    sql"""
      DROP TABLE Article
        """.execute()
    ()
  }
  override def persist(e: Article): Try[Article] = Try {
    sql"""
      MERGE INTO Article VALUES (
        ${e.id},
        ${e.articleId},
        ${e.productId},
        ${e.name},
        ${e.subName},
        ${e.price},
        ${e.reclaim},
        ${e.volume},
        ${e.pricePerLitre},
        ${e.salesStart},
        ${e.salesEnd},
        ${e.productGroup},
        ${e.packaging},
        ${e.seal},
        ${e.origin},
        ${e.country},
        ${e.producer},
        ${e.supplier},
        ${e.vintage},
        ${e.vintageTested},
        ${e.alcoholPercentage},
        ${e.assortment},
        ${e.ecological},
        ${e.kosher},
        ${e.description},
        ${e.sugarLevel})
        """.executeUpdate()
  }
  override def delete(id: Int): Try[Int] = Try {
    sql"""
      DELETE
      FROM Article
      WHERE id = $id
        """.executeUpdate()
  }
  override def fetch(id: Int): Try[Option[Article]] = Try {
    sql"""
      SELECT *
      FROM Article
      WHERE id = $id
        """.asSingleOption[Article]
  }
  /*
  private def build(combiner: String, conditions: Seq[Query.Condition]): InterpolatedQuery = {
    import Query._
    builder.append(" and ")
    for (condition ← conditions) {
      condition match {
        case Not(conditions) ⇒
          builder.append("not (")
          format(builder, "and", conditions)
          builder.append(")")
        case And(conditions) =>
          builder.append("and (")
          format(builder, "and", conditions)
          builder.append(")")
        case Or(conditions) =>
          builder.append(" or (")
          format(builder, "and", conditions)
          builder.append(")")
        case Nand(conditions) =>
          builder.append(" nand (")
          format(builder, "and", conditions)
          builder.append(")")
        case Nor(conditions) =>
          builder.append(" nor (")
          format(builder, "and", conditions)
          builder.append(")")
        case Xor(conditions) =>
          builder.append(" xor (")
          format(builder, "and", conditions)
          builder.append(")")
        case Exists(name) =>
          builder.append(" and ").append(name).append(" is not null")
        case EqualTo(name, value) =>
          builder.append(" and ").append(name).append(" = ").append(value)
        case NotEqualTo(value) =>
        case Null =>
        case NotNull =>
        case LessThan(limit) =>
        case LessThanOrEqualTo(limit) =>
        case GreaterThan(limit) =>
        case GreaterThanOrEqualTo(limit) =>
        case Between(lower, upper, inclusive) =>
        case Like(pattern) =>
        case Matches(literal) =>
        case In(items)) ⇒
        case NotIn(items)) ⇒
      }
    }
  }
  */
  override def find(query: Query): Try[Seq[Article]] = Try {
    sql"""
        SELECT *
        FROM Article
        WHERE id = $id
          """.asSingleOption[Article]
  }
  implicit val parser: (SqlRow) ⇒ Article = {
    row: SqlRow ⇒
      Article(
        id = row.int("id"),
        articleId = row.int("articleId"),
        productId = row.int("productId"),
        name = row.string("name"),
        subName = row.stringOption("subName"),
        price = row.int("price"),
        reclaim = row.intOption("reclaim"),
        volume = row.int("volume"),
        pricePerLitre = row.int("pricePerLitre"),
        salesStart = LocalDate.from(row.date("salesStart").toInstant),
        salesEnd = row.dateOption("salesEnd").map(d ⇒ LocalDate.from(d.toInstant)),
        productGroup = row.stringOption("productGroup"),
        packaging = row.stringOption("packaging"),
        seal = row.stringOption("seal"),
        origin = row.stringOption("origin"),
        country = row.string("country"),
        producer = row.string("producer"),
        supplier = row.string("supplier"),
        vintage = row.intOption("vintage").map(Year.of),
        vintageTested = row.intOption("vintageTested").map(Year.of),
        alcoholPercentage = row.int("alcoholPercentage"),
        assortment = row.string("assortment"),
        ecological = row.boolOption("ecological"),
        kosher = row.boolOption("kosher"),
        description = row.stringOption("description"),
        sugarLevel = row.intOption("sugarLevel"))
  }
}
object Articles extends Articles with App {
  for (file <- Products.file) {
    Articles
  }
}
*/
object Products {
  def file: Option[File] = {
    val dir = new File("./src/main/resources")
    dir.listFiles(
      new FilenameFilter {
        override def accept(dir: File, name: String): Boolean = name.matches("""products-\d\d\d\d-\d\d-\d\d\.xml""")
      }).sortBy(_.getName).lastOption
  }
}
