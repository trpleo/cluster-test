import ProtobufUtil._

lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion    = "2.5.13"
lazy val logbackVersion = "1.2.3"
lazy val akkaManagementVersion = "0.14.0"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
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

    scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "evicted", "-target:jvm-1.8", "-encoding", "utf8"),

    dockerBaseImage := "java:latest",
    maintainer in Docker  := "istvan, papp",
    packageName in Docker := name.value,
    version in Docker     := version.value,
    dockerExposedPorts in Docker := Seq(6651, 9000, 8500),
    dockerExposedUdpPorts in Docker := Seq(),
    dockerExposedVolumes in Docker := Seq(),
    dockerLabels in Docker := Map(),
//    dockerEntrypoint := Seq(s"${(defaultLinuxInstallLocation in Docker).value}/bin/${executableScriptName.value}"),
//    dockerEntrypoint in Docker := Seq("sh", "-c", """SEEDNODES="akka.tcp://TestSTOCluster@127.0.0.1:6651"; HTTPSERVICEPORT=9000; CMHTTPSERVICEPORT=8500"""),
    dockerEntrypoint in Docker := Seq("sh", "-c", """HTTPSERVICEPORT=9000"""),
    dockerCmd in Docker := Seq(),
    dockerExecCommand in Docker := Seq("docker"),
    // HACK: it is a hack / workaround.
    // It is really similar to the problem was rescribed in the ticket
    // https://github.com/playframework/playframework/issues/6688
    // https://groups.google.com/forum/#!topic/simple-build-tool/5OPY0pS2fA8
    sources in (Compile, doc) := Seq.empty
  )
  .settings(scalapbSettings(".")) // since this is a lib, at this point not the project name, but the current folder must be set.
  .settings(mainClass in Compile := Some("org.trp.cluster.QuickstartServer"))

addCommandAlias("dockerize", ";reload;docker:publishLocal")