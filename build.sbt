import ProtobufUtil._

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

      // rabbitmq
      "com.newmotion" %% "akka-rabbitmq" % "5.0.0",

      // testing
      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test
    ),
    envVars in Test := Map(
      "SEEDNODES" -> "akka.tcp://TestSTOCluster@127.0.0.1:6651",
      "HTTPSERVICEPORT" -> "9000",
      "CMHTTPSERVICEPORT" -> "8500"
      ),
    scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "evicted", "-target:jvm-1.8", "-encoding", "utf8")
  )
  .settings(scalapbSettings(".")) // since this is a lib, at this point not the project name, but the current folder must be set.