val scala3Version = "3.2.1"

enablePlugins(ScalaNativePlugin)

lazy val root = project
  .in(file("."))
  .settings(
    name := "psi-mackerel",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
//      "org.typelevel" %% "cats-core" % "2.9.0",
      "org.typelevel" %% "cats-effect" % "3.4.2",
      "org.http4s" %% "http4s-ember-client" % "0.23.16",
      "com.monovore" %% "decline" % "2.4.0",
      "org.scalameta" %% "munit" % "0.7.29" % Test,
    )
  )
