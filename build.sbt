import sbt._
import Keys._
import NativePackagerHelper._

name := "sweetspot"
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

Project.settings

libraryDependencies ++= Seq(
  Dependencies.Akka.Actor,
  Dependencies.Akka.Slf4j,
  Dependencies.Akka.Stream,
  Dependencies.Akka.HttpCore,
  Dependencies.Akka.HttpSprayJson,
  Dependencies.Akka.HttpXml,
  Dependencies.ScalikeJdbc,
  Dependencies.Config,
  Dependencies.ScalaScraper,
  Dependencies.Logback
)

mainClass in Compile := Some("bitbonanza.sweetspot.App")
maintainer in Docker := "Odd Möller <odd.moller@gmail.com>"
dockerBaseImage := "iron/java:1.8-dev"
bashScriptConfigLocation := Some("${app_home}/../resources/application.conf")
mappings in Universal ++= {
  directory("scripts") ++
    contentOf("src/main/resources").toMap.mapValues("config/" + _)
}
scriptClasspath := Seq("../config/") ++ scriptClasspath.value
