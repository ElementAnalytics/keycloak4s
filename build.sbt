import sbt.Keys.{credentials, publishMavenStyle}
import sbt.{Credentials, url}

val baseScalaOpts = Seq(
  "-Ywarn-unused:implicits",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:params",
  "-Ywarn-unused:patvars",
  "-Ywarn-unused:privates",
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard"
)

val scalac213Opts = baseScalaOpts
val scalac212Opts = baseScalaOpts ++ Seq("-Ypartial-unification")

val nexus = "https://ean.jfrog.io"
val elementSnapshotRepo = "snapshots" at nexus + "/ean/sbt-dev-local"
val elementReleaseRepo = "releases" at nexus + "/ean/sbt-release-local"

val artifactory =
  Credentials.toDirect(Credentials(Path.userHome / ".sbt" / ".credentials"))

val publishSettings = Seq(
  Compile / publishArtifact := true,
  Test / publishArtifact := false,
  Test / packageDoc / publishArtifact := false,
  credentials += artifactory,
  publishMavenStyle := true,
  publishTo := {
    if (isSnapshot.value) Some(elementSnapshotRepo)
    else Some(elementReleaseRepo)
  }
)

lazy val global = {
  Seq(
    scalaVersion  := "2.13.5",
    organization  := "com.fullfacing",
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 12 => scalac212Opts
      case _                       => scalac213Opts
    }),

    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full),

    credentials += Credentials("GnuPG Key ID", "gpg", "419C90FB607D11B0A7FE51CFDAF842ABC601C14F", "ignored"),

    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary,

    crossScalaVersions := Seq(scalaVersion.value, "2.12.13"),
  )
}

// ---------------------------------- //
//          Library Versions          //
// ---------------------------------- //
val akkaHttpVersion       = "10.2.4"
val akkaStreamsVersion    = "2.6.14"
val catsEffectVersion     = "2.5.1"
val catsCoreVersion       = "2.6.1"
val enumeratumVersion     = "1.6.0"
val json4sVersion         = "3.6.11"
val logbackVersion        = "1.2.3"
val monixVersion          = "3.4.0"
val monixBioVersion       = "1.1.0"
val nimbusVersion         = "9.9.3"
val scalaTestVersion      = "3.2.9"
val sttpVersion           = "3.2.3"

// -------------------------------------- //
//          Library Dependencies          //
// -------------------------------------- //
val akkaHttp: Seq[ModuleID] = Seq(
  "com.typesafe.akka" %% "akka-stream"  % akkaStreamsVersion,
  "com.typesafe.akka" %% "akka-http"    % akkaHttpVersion
)

val akkaTestKit: Seq[ModuleID] = Seq(
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamsVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion
)

val cats: Seq[ModuleID] = Seq(
  "org.typelevel" %% "cats-core"   % catsCoreVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion
)

val enumeratum: Seq[ModuleID] = Seq(
  "com.beachape" %% "enumeratum-json4s" % enumeratumVersion
)

val json4s: Seq[ModuleID] = Seq(
  "org.json4s" %% "json4s-jackson" % json4sVersion
)

val logback: Seq[ModuleID] = Seq(
  "ch.qos.logback" % "logback-core"    % logbackVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion
)

val monix: Seq[ModuleID] = Seq(
  "io.monix" %% "monix" % monixVersion
)

val `monix-bio`: Seq[ModuleID] = Seq(
  "io.monix" %% "monix-bio" % monixBioVersion,
  "io.monix" %% "monix-reactive" % monixVersion
)

val nimbus: Seq[ModuleID] = Seq(
  "com.nimbusds" % "nimbus-jose-jwt" % nimbusVersion,
  "net.minidev" % "json-smart" % "2.4.7"
)

val scalaTest: Seq[ModuleID] = Seq(
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test
)

val sttp: Seq[ModuleID] = Seq(
  "com.softwaremill.sttp.client3" %% "core"   % sttpVersion,
  "com.softwaremill.sttp.client3" %% "json4s" % sttpVersion
)

val sttpAkka: Seq[ModuleID] = Seq(
  "com.softwaremill.sttp.client3" %% "akka-http-backend" % sttpVersion
)

val sttpAkkaMonix: Seq[ModuleID] = Seq(
  "com.fullfacing" %% "sttp-akka-monix-task" % "1.6.0"
)

// --------------------------------------------- //
// Project and configuration for keycloak4s-core //
// --------------------------------------------- //
lazy val coreDependencies: Seq[ModuleID] = cats ++ json4s ++ logback ++ enumeratum

lazy val `keycloak4s-core` = (project in file("./keycloak4s-core"))
  .settings(global: _*)
  .settings(publishSettings)
  .settings(libraryDependencies ++= coreDependencies)
  .settings(name := "keycloak4s-core")

// ---------------------------------------------- //
// Project and configuration for keycloak4s-admin //
// ---------------------------------------------- //
lazy val `keycloak4s-admin` = (project in file("./keycloak4s-admin"))
  .settings(global: _*)
  .settings(publishSettings)
  .settings(libraryDependencies ++= sttp)
  .settings(name := "keycloak4s-admin")
  .dependsOn(`keycloak4s-core`)

// ---------------------------------------------------- //
// Project and configuration for keycloak4s-admin-monix //
// ---------------------------------------------------- //
lazy val `keycloak4s-monix` = (project in file("./keycloak4s-admin-monix"))
  .settings(publishSettings)
  .settings(global: _*)
  .settings(libraryDependencies ++= monix)
  .settings(name := "keycloak4s-admin-monix")
  .dependsOn(`keycloak4s-admin`)

// ---------------------------------------------------- //
// Project and configuration for keycloak4s-admin-monix //
// ---------------------------------------------------- //
lazy val `keycloak4s-monix-bio` = (project in file("./keycloak4s-admin-monix-bio"))
  .settings(global: _*)
  .settings(publishSettings)
  .settings(libraryDependencies ++= `monix-bio` ++ sttp)
  .settings(name := "keycloak4s-admin-monix-bio")
  .dependsOn(`keycloak4s-core`)

// ------------------------------------------------------- //
// Project and configuration for keycloak4s-auth-core //
// ------------------------------------------------------- //
lazy val `keycloak4s-auth-core` = (project in file("./keycloak4s-auth/core"))
  .settings(global: _*)
  .settings(publishSettings)
  .settings(libraryDependencies ++= nimbus)
  .settings(name := "keycloak4s-auth-core")
  .dependsOn(`keycloak4s-core`)

// ------------------------------------------------------- //
// Project and configuration for keycloak4s-auth-akka-http //
// ------------------------------------------------------- //
lazy val `keycloak4s-akka-http` = (project in file("./keycloak4s-auth/akka-http"))
  .settings(global: _*)
  .settings(publishSettings)
  .settings(libraryDependencies ++= akkaHttp)
  .settings(name := "keycloak4s-auth-akka-http")
  .dependsOn(`keycloak4s-auth-core`)

// ------------------------------------------------------- //
// Project and configuration for keycloak4s-authz-client //
// ------------------------------------------------------- //
lazy val `keycloak4s-authz` = (project in file("./keycloak4s-authz-client"))
  .settings(global: _*)
  .settings(publishSettings)
  .settings(name := "keycloak4s-authz-client")

// --------------------------------------------------- //
// Project and configuration for keycloak4s-playground //
// --------------------------------------------------- //
lazy val `keycloak4s-playground` = (project in file("./keycloak4s-playground"))
  .settings(scalaVersion  := "2.13.5")
  .settings(publish / skip := true)
  .settings(libraryDependencies ++= sttpAkkaMonix ++ scalaTest ++ akkaTestKit ++ sttpAkka)
  .settings(coverageEnabled := false)
  .settings(Test / parallelExecution := false)
  .settings(scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n <= 12 => scalac212Opts
    case _                       => scalac213Opts
  }))
  .settings(addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"))
  .settings(name := "keycloak4s-playground", publishArtifact := false)
  .dependsOn(`keycloak4s-admin`, `keycloak4s-monix`, `keycloak4s-akka-http`)

// ---------------------------------------------- //
// Project and configuration for the root project //
// ---------------------------------------------- //
lazy val root = (project in file("."))
  .settings(global: _*)
  .settings(publishArtifact := false)
  .settings(publish / skip := true)
  .aggregate(
    `keycloak4s-core`,
    `keycloak4s-admin`,
    `keycloak4s-monix`,
    `keycloak4s-monix-bio`,
    `keycloak4s-auth-core`,
    `keycloak4s-akka-http`,
    `keycloak4s-authz`,
    `keycloak4s-playground`
  )
