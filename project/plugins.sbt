logLevel := Level.Warn

// Allow coverage to run, and automatically upload to codacy
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0")

addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "3.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % "1.2.0")
