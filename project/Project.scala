import sbt._
import Keys._

object Project {
  val settings = Defaults.defaultConfigs ++ Seq(
      organization := "org.bitbonanza",
      scalaVersion :=  "2.12.1",
      version := "1.0.1",
      resolvers := Dependencies.resolvers,
      libraryDependencies ++= Seq(
          Dependencies.Config,
          Dependencies.Akka.Testkit % "test",
          Dependencies.ScalaTest),
      scalacOptions ++= Seq(
        "-encoding", "UTF-8",
        "-deprecation",
        "-unchecked",
        "-Ywarn-dead-code",
        "-feature"
      ),
      javacOptions ++= Seq(
        "-Xlint:unchecked",
        "-Xlint:deprecation"
      ),
      javaOptions ++= Seq(
        "-Xmx2G"
      )
  )
}
