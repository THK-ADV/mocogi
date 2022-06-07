lazy val `mocogi` = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "mocogi",
    maintainer := "Alexander Dobrynin <alexander.dobrynin@th-koeln.de>",
    version := "1.0",
    scalaVersion := "2.13.8",
    resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/",
    libraryDependencies ++= play,
    libraryDependencies ++= test,
    libraryDependencies += parser,
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
  guice,
  ws,
  ehcache,
  "com.typesafe.play" %% "play-json" % "2.9.2"
)

lazy val test = Seq(
  "org.scalactic" %% "scalactic" % "3.2.12",
  "org.scalatest" %% "scalatest" % "3.2.12" % "test",
)

lazy val parser = "de.th-koeln.inf.adv" %% "nebulak" % "0.1"