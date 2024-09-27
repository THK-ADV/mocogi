logLevel := Level.Warn

resolvers += "Typesafe repository".at("https://repo.typesafe.com/typesafe/releases/")

addSbtPlugin("org.playframework" % "sbt-plugin"   % "3.0.5")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix" % "0.12.1")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.5.2")
