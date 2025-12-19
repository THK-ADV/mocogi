lazy val `mocogi` = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name              := "mocogi",
    maintainer        := "Alexander Dobrynin <alexander.dobrynin@th-koeln.de>",
    version           := "1.0",
    scalaVersion      := "3.4.2",
    semanticdbEnabled := true,
    scalacOptions += "-Wunused:imports",
    resolvers += "Akka Snapshot Repository".at("https://repo.akka.io/snapshots/"),
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    libraryDependencies ++= play,
    libraryDependencies ++= guiceDeps,
    libraryDependencies ++= test,
    libraryDependencies ++= database,
    libraryDependencies += parser,
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
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.2"
)

lazy val test = Seq(
  "org.scalactic"          %% "scalactic"          % "3.2.19",
  "org.scalatest"          %% "scalatest"          % "3.2.19" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2"  % "test"
)

lazy val parser = "de.th-koeln.inf.adv" %% "nebulak" % "0.13"

lazy val database = Seq(
  "org.playframework"   %% "play-slick"            % "6.2.0",
  "org.playframework"   %% "play-slick-evolutions" % "6.2.0",
  "org.postgresql"       % "postgresql"            % "42.7.8",
  "com.github.tminglei" %% "slick-pg"              % "0.23.1",
  "com.github.tminglei" %% "slick-pg_play-json"    % "0.23.1",
)

lazy val circle = "io.circe" %% "circe-yaml" % "0.16.0"

lazy val parallelCollections =
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0"

lazy val guiceDeps = Seq(
  guice,
  "com.google.inject"            % "guice"                % "6.0.0",
  "com.google.inject.extensions" % "guice-assistedinject" % "6.0.0"
)

val keycloak = Seq(
  "org.keycloak"      % "keycloak-core"         % "25.0.3",
  "org.keycloak"      % "keycloak-adapter-core" % "25.0.3",
  "org.jboss.logging" % "jboss-logging"         % "3.5.3.Final"
)

val optics = Seq(
  "dev.optics" %% "monocle-core"  % "3.3.0",
  "dev.optics" %% "monocle-macro" % "3.3.0"
)

val mail = Seq(
  "org.playframework" %% "play-mailer"       % "10.1.0",
  "org.playframework" %% "play-mailer-guice" % "10.1.0"
)

val scalaScraper = "net.ruippeixotog" %% "scala-scraper" % "3.2.0"
