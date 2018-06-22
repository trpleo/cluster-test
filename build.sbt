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
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

      // cluster
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,

      // log
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "com.lightbend.akka.management" %% "akka-management"              % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,

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
