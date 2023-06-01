val playSlickVersion = "5.1.0"
val guiceVersion = "5.1.0"
val scalaTestVersion = "3.2.15"

lazy val `mocogi` = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "mocogi",
    maintainer := "Alexander Dobrynin <alexander.dobrynin@th-koeln.de>",
    version := "1.0",
    scalaVersion := "2.13.10",
    resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/",
    libraryDependencies ++= play,
    libraryDependencies ++= test,
    libraryDependencies ++= database,
    libraryDependencies += parser,
    libraryDependencies += kafka,
    externalResolvers ++= Seq(
      "GitHub <THK-ADV> Apache Maven Packages" at "https://maven.pkg.github.com/THK-ADV/nebulak"
    ),
    credentials += Credentials(
      "GitHub Package Registry",
      "maven.pkg.github.com",
      "THK-ADV",
      ""
    )
  )

lazy val play = Seq(
  specs2 % Test,
  ws,
  guice,
  ehcache,
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.2",
  "com.google.inject" % "guice" % guiceVersion,
  "com.google.inject.extensions" % "guice-assistedinject" % guiceVersion
)

lazy val test = Seq(
  "org.scalactic" %% "scalactic" % scalaTestVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test"
)

lazy val parser = "de.th-koeln.inf.adv" %% "nebulak" % "0.10"

lazy val database = Seq(
  "com.typesafe.play" %% "play-slick" % playSlickVersion,
  "com.typesafe.play" %% "play-slick-evolutions" % playSlickVersion,
  "org.postgresql" % "postgresql" % "42.5.4"
)

lazy val kafka = "de.th-koeln.inf.adv" %% "kafka-pubsub" % "0.3"
