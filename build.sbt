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
  //Dependencies.ScalikeJdbc,
  Dependencies.Config,
  Dependencies.ScalaScraper,
  Dependencies.Logback
)

mainClass in Compile := Some("bitbonanza.sweetspot.Proxy")
maintainer in Docker := "Odd Möller <odd.moller@gmail.com>"
dockerBaseImage := "anapsix/alpine-java"
dockerRepository := Some("oddo")
dockerUpdateLatest := true
daemonUser in Docker := "root"
bashScriptConfigLocation := Some("${app_home}/../resources/application.conf")
mappings in Universal ++= {
  directory("scripts") ++
    contentOf("src/main/resources").toMap.mapValues("config/" + _)
}
scriptClasspath := Seq("../config/") ++ scriptClasspath.value
/*
stage in Docker ~= { result =>
  val src = file("./src/main/resources").getAbsoluteFile
  val dst = file("./target/docker/stage/src/main/resources")
  IO.copyDirectory(src, dst)
  println(s"Copied $src (${src.exists}) to $dst (${dst.exists})")
  if (!dst.exists()) {
    try {
      println(s"Linking $src (${src.exists}) to $dst (${dst.exists})")
      dst.getParentFile.mkdirs()
      java.nio.file.Files.createSymbolicLink(dst.toPath, src.toPath)
      println(s"Linked $src (${src.exists}) into $dst (${dst.exists})")
    } catch {
      case e: Exception =>
        println("### Could not link: " + e.getMessage)
        e.printStackTrace()
    }
  }
  result
}
*/