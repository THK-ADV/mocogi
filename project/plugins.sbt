logLevel := Level.Warn

resolvers += "Typesafe repository".at("https://repo.typesafe.com/typesafe/releases/")

addSbtPlugin("org.playframework" % "sbt-plugin"   % "3.0.8")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix" % "0.14.5")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.5.5")
