lazy val akkaHttpVersion = "10.2.9"
lazy val akkaVersion    = "2.6.19"

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.AssignmentOne",
      scalaVersion    := "2.13.4"
    )),
    name := "Assignment-One",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"% akkaHttpVersion,

      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,

      "org.scalatest"     %% "scalatest"                % "3.2.12"         % Test,

      "ch.qos.logback"    % "logback-classic"           % "1.2.11",
      "com.typesafe.slick" %% "slick" % "3.3.3",
      "mysql" % "mysql-connector-java" % "8.0.29"

    )
  )
