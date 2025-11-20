val playSlickVersion = "6.1.1"
val guiceVersion     = "5.1.0"
val scalaTestVersion = "3.2.19"
val keycloakVersion  = "24.0.3"
val monocle          = "3.3.0"
val playMailVersion  = "10.0.0"

lazy val `mocogi` = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name              := "mocogi",
    maintainer        := "Alexander Dobrynin <alexander.dobrynin@th-koeln.de>",
    version           := "1.0",
    scalaVersion      := "3.4.2",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions += "-Wunused:imports",
    resolvers += "Akka Snapshot Repository".at("https://repo.akka.io/snapshots/"),
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    libraryDependencies ++= play,
    libraryDependencies ++= guiceDeps,
    libraryDependencies ++= test,
    libraryDependencies ++= database,
    libraryDependencies += parser,
    libraryDependencies += kafka,
    libraryDependencies ++= keycloak,
    libraryDependencies ++= optics,
    libraryDependencies += parallelCollections,
    libraryDependencies += circle,
    libraryDependencies ++= mail,
    libraryDependencies += scalaScraper,
    externalResolvers ++= Seq(
      "GitHub <THK-ADV> Apache Maven Packages".at("https://maven.pkg.github.com/THK-ADV/nebulak")
    ),
    credentials += Credentials(
      "GitHub Package Registry",
      "maven.pkg.github.com",
      "THK-ADV",
      sys.env.getOrElse("GITHUB_TOKEN", "")
    ),
    (Universal / javaOptions) ++= Seq(
      "-Dpidfile.path=/dev/null"
    )
  )

lazy val play = Seq(
  specs2 % Test,
  ws,
  ehcache,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.2" // jackson-databind 2.15.2
)

lazy val test = Seq(
  "org.scalactic"          %% "scalactic"          % scalaTestVersion,
  "org.scalatest"          %% "scalatest"          % scalaTestVersion % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1"          % "test"
)

lazy val parser = "de.th-koeln.inf.adv" %% "nebulak" % "0.13"

lazy val database = Seq(
  "org.playframework"   %% "play-slick"            % playSlickVersion,
  "org.playframework"   %% "play-slick-evolutions" % playSlickVersion,
  "org.postgresql"       % "postgresql"            % "42.7.3",
  "com.github.tminglei" %% "slick-pg"              % "0.22.2",
  "com.github.tminglei" %% "slick-pg_play-json"    % "0.22.2",
)

lazy val kafka = "org.apache.kafka" % "kafka-clients" % "3.8.0"

lazy val circle = "io.circe" %% "circe-yaml" % "0.16.0"

lazy val parallelCollections =
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4"

lazy val guiceDeps = Seq(
  guice,
  "com.google.inject"            % "guice"                % guiceVersion,
  "com.google.inject.extensions" % "guice-assistedinject" % guiceVersion
)

val keycloak = Seq(
  "org.keycloak"      % "keycloak-core"         % keycloakVersion,
  "org.keycloak"      % "keycloak-adapter-core" % keycloakVersion,
  "org.jboss.logging" % "jboss-logging"         % "3.5.3.Final"
)

val optics = Seq(
  "dev.optics" %% "monocle-core"  % monocle,
  "dev.optics" %% "monocle-macro" % monocle
)

val mail = Seq(
  "org.playframework" %% "play-mailer"       % playMailVersion,
  "org.playframework" %% "play-mailer-guice" % playMailVersion
)

val scalaScraper = "net.ruippeixotog" %% "scala-scraper" % "3.2.0"
