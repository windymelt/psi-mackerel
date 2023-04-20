val scala3Version = "3.2.1"

enablePlugins(ScalaNativePlugin)

val https4sVersion = "0.23.16"
val circeVersion = "0.14.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "psi-mackerel",
    version := "1.0.2",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
//      "org.typelevel" %% "cats-core" % "2.9.0",
      "org.typelevel" %%% "cats-effect" % "3.4.2",
      "org.http4s" %%% "http4s-dsl" % https4sVersion,
      "org.http4s" %%% "http4s-curl" % "0.1.1",
      "org.http4s" %%% "http4s-circe" % https4sVersion,
      "com.monovore" %%% "decline" % "2.4.0",
      "com.monovore" %%% "decline-effect" % "2.4.0",
      "com.lihaoyi" %%% "fansi" % "0.4.0",
      "org.scalameta" %% "munit" % "0.7.29" % Test,
    ),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
//      "io.circe" %% "circe-parser"
    ),
    nativeMode := sys.env
      .get("NATIVE_MODE") getOrElse "debug", // use "release-fast" when release
    nativeLTO := sys.env
      .get("NATIVE_LTO") getOrElse "none", // use "thin" when release
  )
