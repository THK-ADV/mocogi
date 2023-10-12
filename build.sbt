val playSlickVersion = "5.1.0"
val guiceVersion = "5.1.0"
val scalaTestVersion = "3.2.15"
val keycloakVersion = "22.0.1"

lazy val `mocogi` = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "mocogi",
    maintainer := "Alexander Dobrynin <alexander.dobrynin@th-koeln.de>",
    version := "1.0",
    scalaVersion := "2.13.10",
    resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/",
    libraryDependencies ++= play,
    libraryDependencies ++= guiceDeps,
    libraryDependencies ++= playJson,
    libraryDependencies ++= test,
    libraryDependencies ++= database,
    libraryDependencies += parser,
    libraryDependencies += kafka,
    libraryDependencies ++= keycloak,
    libraryDependencies ++= optics,
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
  ehcache
)

lazy val test = Seq(
  "org.scalactic" %% "scalactic" % scalaTestVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test",
  "org.mockito" % "mockito-core" % "5.5.0" % Test
)

lazy val parser = "de.th-koeln.inf.adv" %% "nebulak" % "0.10"

lazy val database = Seq(
  "com.typesafe.play" %% "play-slick" % playSlickVersion,
  "com.typesafe.play" %% "play-slick-evolutions" % playSlickVersion,
  "org.postgresql" % "postgresql" % "42.5.4"
)

lazy val kafka = "de.th-koeln.inf.adv" %% "kafka-pubsub" % "0.3"

lazy val playJson = Seq(
  "com.typesafe.play" %% "play-json" % "2.10.0-RC9",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.2", // jackson-databind 2.15.2
)

lazy val guiceDeps = Seq(
  guice,
  "com.google.inject" % "guice" % guiceVersion,
  "com.google.inject.extensions" % "guice-assistedinject" % guiceVersion
)

val keycloak = Seq(
  "org.keycloak" % "keycloak-core" % keycloakVersion,
  "org.keycloak" % "keycloak-adapter-core" % keycloakVersion,
  "org.jboss.logging" % "jboss-logging" % "3.5.1.Final"
)

val optics = Seq(
  "dev.optics" %% "monocle-core" % "3.2.0",
  "dev.optics" %% "monocle-macro" % "3.2.0",
)