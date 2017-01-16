import sbt._

object Dependencies {
  val resolvers = Seq(
    "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/",
    "OJO Snapshots" at "https://oss.jfrog.org/oss-snapshot-local"
  )
  object Akka {
    val Version = "2.4.+"
    val HttpVersion = "10.0.+"
    def module(name: String, v: String = null) = "com.typesafe.akka" %% s"akka-$name" % Option(v)
        .getOrElse(Version) withSources()
    def httpModule(name: String, v: String = null) = "com.typesafe.akka" %% s"akka-$name" % Option(v)
        .getOrElse(HttpVersion) withSources()
    lazy val Actor = module("actor")
    lazy val Persistence = module("persistence")
    lazy val PersistenceQuery = module("persistence-query-experimental")
    lazy val Camel = module("camel")
    lazy val Remote = module("remote")
    lazy val Cluster = module("cluster")
    lazy val ClusterTools = module("cluster-tools")
    lazy val ClusterSharding = module("cluster-sharding")
    lazy val DistributedData = module("distributed-data-experimental")
    lazy val Stream = module("stream")
    lazy val HttpCore = httpModule("http-core")
    lazy val Http = httpModule("http")
    lazy val HttpSprayJson = httpModule("http-spray-json")
    lazy val HttpJackson = httpModule("http-jackson")
    lazy val HttpXml = httpModule("http-xml")
    lazy val HttpTestkit = httpModule("http-testkit")
    lazy val Contrib = module("contrib") intransitive()
    lazy val Slf4j = module("slf4j")
    lazy val Testkit = module("testkit")
  }
  val ScalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.3" withSources()
  val SprayJson = "io.spray" %% "spray-json" % "1.3.2" withSources()
  val Eventuate = "com.rbmhtechnology" %% "eventuate" % "0.5-SNAPSHOT" withSources()
  val Logback = "ch.qos.logback" % "logback-classic" % "1.1.+"
  val Config = "com.typesafe" % "config" % "1.3.+" withSources()
  val LevelDb = "org.iq80.leveldb" % "leveldb" % "0.7"
  val LevelDbJni = "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
  //val relate = "com.lucidchart" %% "relate" % "2.0.0-SNAPSHOT" withSources()
  val ScalaScraper = "net.ruippeixotog" %% "scala-scraper" % "1.2.+" withSources()
  //val ScalikeJdbc = "org.scalikejdbc" %% "scalikejdbc" % "3.0.0-M2"
  val H2 = "com.h2database" % "h2" % "1.4.+"
  val ScalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % Test
  val ScalaCheck = "org.scalacheck" %% "scalacheck" % "1.12.4" % Test
}
