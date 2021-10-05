val projectName = "transactional-data-engineer-test"

name := projectName
version := "0.1"

scalaVersion := "2.12.15"

idePackagePrefix := Some("it.zevektor.datatest")

inThisBuild(Seq(
  scalaVersion := "2.12.15",
  organization := "it.zevektor"
))

val sparkVersion = "3.0.2"

lazy val sparkDeps = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
  "org.apache.spark" %% "spark-sql" % sparkVersion % Provided
)

lazy val appDeps = Seq(
  "com.lihaoyi" %% "os-lib" % "0.7.8",
  "com.lihaoyi" %% "upickle" % "0.7.1",
  "org.postgresql" % "postgresql" % "42.2.24"
)

lazy val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "org.mockito" % "mockito-core" % "2.21.0" % Test
)


val allDeps = sparkDeps ++ appDeps ++ testDeps

lazy val assemblySettings = Seq(
  assembly / assemblyOutputPath := file("./output/de-test.jar"),
  assembly / assemblyMergeStrategy := {
    case PathList("module-info.class") => MergeStrategy.discard
    case x if x.endsWith("/module-info.class") => MergeStrategy.discard
    case x if x.contains("scala/annotation/nowarn.class")  => MergeStrategy.first
    case x if x.contains("scala/annotation/nowarn$.class") => MergeStrategy.first
    case PathList("org", "aopalliance", xs@_*) => MergeStrategy.last
    case PathList("javax", "inject", xs@_*) => MergeStrategy.last
    case PathList("javax", "servlet", xs@_*) => MergeStrategy.last
    case PathList("javax", "activation", xs@_*) => MergeStrategy.last
    case PathList("org", "apache", xs@_*) => MergeStrategy.last
    case "git.properties" => MergeStrategy.concat
    case "log4j.properties" => MergeStrategy.concat
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val defaultSettings = Defaults.coreDefaultSettings ++ Seq(
  libraryDependencies ++= allDeps,
  Test / fork := true,
  Test / parallelExecution := false,
  javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")
)

lazy val root = Project(id = projectName, base = file("."))
  .enablePlugins(AssemblyPlugin)
  .settings(defaultSettings, assemblySettings)
