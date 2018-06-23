
lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion    = "2.5.13"
lazy val logbackVersion = "1.2.3"
lazy val akkaManagementVersion = "0.14.0"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "org.trp",
      scalaVersion    := "2.12.6"
    )),
    name := "cluster-test",
    libraryDependencies ++= Seq(
      // akka-http
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

      // akka-cluster
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,

      // log
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "com.lightbend.akka.management" %% "akka-management"              % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,

      // protobuf
      "com.thesamet.scalapb" %% "scalapb-json4s" % "0.7.1",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",

      // testing
      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test
    ),
    envVars in Test := Map(
      "IPSHOST" -> "127.0.0.1",
      "IPSPORT" -> "9090"
      ),
    scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "evicted", "-target:jvm-1.8", "-encoding", "utf8")
  )
  .settings(scalapbSettings(".")) // since this is a lib, at this point not the project name, but the current folder must be set.

/**
  * This method sets up where are the .proto files can be found for the projects and the
  * related params (like what would be the language to apply)
  *
  * Usable example is in the project:
  *   https://github.com/scalapb/ScalaPB/tree/master/examples
  * Documentation:
  *   https://trueaccord.github.io/ScalaPB/sbt-settings.html
  *
  * @param projectFolder
  * @param forJava
  * @param forServer
  * @return
  */
def scalapbSettings(projectFolder: String, forJava: Boolean = false, forServer: Boolean = false) = {

  val f = file(s"$projectFolder/src/main/protobuf")

  require(f.exists(), s"The specified folder dir is not exists! [$projectFolder]")
  require(f.isDirectory, s"The specified path is not a folder! [$projectFolder]")

  val protoSources = PB.protoSources in Compile := Seq(file(s"$projectFolder/src/main/protobuf"))
  val pVersion = PB.protocVersion := "-v300"

  val pbgen = forJava match {
    case true =>
      PB.targets in Compile := {
        Seq(
          scalapb.gen(javaConversions = true, grpc = forServer, singleLineToProtoString = true) -> (sourceManaged in Compile).value,
          PB.gens.java("3.3.1") -> (sourceManaged in Compile).value
        )
      }
    case false =>
      PB.targets in Compile := {
        Seq( scalapb.gen(javaConversions = false, grpc = forServer, singleLineToProtoString = true) -> (sourceManaged in Compile).value )
      }
  }

  Seq(pVersion,protoSources).:+(pbgen)
}