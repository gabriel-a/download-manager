organization := "com.typesafe.akka.samples"
name := "download-manager"

scalaVersion := "2.12.6"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.14",
  "com.lightbend.akka" %% "akka-stream-alpakka-ftp" % "0.20",
  "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.20",
  "com.typesafe.akka" %% "akka-http" % "10.1.3",
  "org.jsoup" % "jsoup" % "1.11.3",
  "commons-validator" % "commons-validator" % "1.6",
  "com.lihaoyi" %% "requests" % "0.1.3",
  "commons-io" % "commons-io" % "2.6"
)

//Test
libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.5" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.14" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % "10.1.3" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.14" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.5.13" % Test
libraryDependencies += "org.mockito" % "mockito-core" % "2.21.0" % Test
libraryDependencies += "org.mockftpserver" % "MockFtpServer" % "2.7.1" % Test
libraryDependencies += "com.jcraft" % "jsch" % "0.1.54" % Test
libraryDependencies += "software.sham" % "sham-ssh" % "0.2.0" % Test
libraryDependencies += "org.awaitility" % "awaitility-scala" % "3.1.2" % Test
libraryDependencies += "com.github.tomakehurst" % "wiremock" % "2.18.0" % Test

licenses := Seq(("MIT", url("https://github.com/gabriel-a/download-manager/license.md")))