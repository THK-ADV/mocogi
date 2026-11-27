addCommandAlias("format", "scalafixAll; scalafmtAll")

ThisBuild / scalaVersion      := "3.9.0"
ThisBuild / semanticdbEnabled := true
ThisBuild / scalacOptions += "-Wunused:imports"

lazy val `mocogi` = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name       := "mocogi",
    maintainer := "Alexander Dobrynin <alexander.dobrynin@th-koeln.de>",
    version    := "1.0",
    libraryDependencies ++= play,
    libraryDependencies ++= jackson,
    libraryDependencies ++= guiceDeps,
    libraryDependencies ++= test,
    libraryDependencies ++= database,
    libraryDependencies += parser,
    libraryDependencies ++= authorization,
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
    ),
    Test / unmanagedSources / excludeFilter ~= { excl =>
      excl || new sbt.io.SimpleFileFilter(f =>
        f.getCanonicalPath.replace('\\', '/').contains("/test/database/") &&
          f.getName.endsWith(".scala")
      )
    }
  )

/** Postgres snapshot suites under test/database, run via `it/test` (not part of `test`). */
lazy val it = project
  .dependsOn(mocogi)
  .settings(
    libraryDependencies ++= test,
    Test / scalaSource := (mocogi / baseDirectory).value / "test" / "database",
    publish / skip     := true
  )

lazy val play = Seq(
  specs2 % Test,
  ws,
  ehcache
)

val jacksonVersion = "2.22.4"

lazy val jackson = Seq(
  "com.fasterxml.jackson.module"    %% "jackson-module-scala"           % jacksonVersion,
  "com.fasterxml.jackson.module"     % "jackson-module-parameter-names" % jacksonVersion,
  "com.fasterxml.jackson.datatype"   % "jackson-datatype-jdk8"          % jacksonVersion,
  "com.fasterxml.jackson.datatype"   % "jackson-datatype-jsr310"        % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor"        % jacksonVersion
)

lazy val test = Seq(
  "org.scalactic"          %% "scalactic"          % "3.2.20"                                  % Test,
  "org.scalatest"          %% "scalatest"          % "3.2.20"                                  % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2"                                   % Test,
  "org.apache.pekko"       %% "pekko-testkit"      % _root_.play.core.PlayVersion.pekkoVersion % Test
)

lazy val parser = "de.th-koeln.inf.adv" %% "nebulak" % "0.14"

lazy val database = Seq(
  "org.playframework"   %% "play-slick"            % "6.2.0",
  "org.playframework"   %% "play-slick-evolutions" % "6.2.0",
  "org.postgresql"       % "postgresql"            % "42.7.14",
  "com.github.tminglei" %% "slick-pg"              % "0.23.1",
  "com.github.tminglei" %% "slick-pg_play-json"    % "0.23.1",
)

lazy val circle = "io.circe" %% "circe-yaml" % "0.16.1"

lazy val parallelCollections =
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.3.0"

lazy val guiceDeps = Seq(
  guice,
  "com.google.inject"            % "guice"                % "6.0.0",
  "com.google.inject.extensions" % "guice-assistedinject" % "6.0.0"
)

val authorization = Seq(
  "com.github.jwt-scala" %% "jwt-play-json" % "12.0.0"
)

val optics = Seq(
  "dev.optics" %% "monocle-core"  % "3.3.0",
  "dev.optics" %% "monocle-macro" % "3.3.0"
)

val mail = Seq(
  "org.playframework" %% "play-mailer"       % "10.1.0",
  "org.playframework" %% "play-mailer-guice" % "10.1.0"
)

val scalaScraper = "net.ruippeixotog" %% "scala-scraper" % "3.3.0"
