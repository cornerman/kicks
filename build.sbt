Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"

Global / excludeLintKeys += webpackDevServerPort // TODO:

val versions = new {
  val outwatch = "1.0.0+4-ea3b233c-SNAPSHOT"
  val colibri  = "0.8.2"
  val funStack = "0.9.19"
  val tapir    = "1.9.0"
  val pprint   = "0.8.1"
}

// Uncomment, if you want to use snapshot dependencies from sonatype or jitpack
// ThisBuild / resolvers ++= Seq(
//   "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
//   "Sonatype OSS Snapshots S01" at "https://s01.oss.sonatype.org/content/repositories/snapshots", // https://central.sonatype.org/news/20210223_new-users-on-s01/
//   "Jitpack" at "https://jitpack.io",
// )

val enableFatalWarnings =
  sys.env.get("ENABLE_FATAL_WARNINGS").flatMap(value => scala.util.Try(value.toBoolean).toOption).getOrElse(false)

lazy val commonSettings = Seq(
  addCompilerPlugin("org.typelevel" % "kind-projector"     % "0.13.2" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"   %% "better-monadic-for" % "0.3.1"),

  // overwrite scalacOptions "-Xfatal-warnings" from https://github.com/DavidGregory084/sbt-tpolecat
  if (enableFatalWarnings) scalacOptions += "-Xfatal-warnings" else scalacOptions -= "-Xfatal-warnings",
  scalacOptions ++= Seq("-Ymacro-annotations", "-Vimplicits", "-Vtype-diffs", "-Xasync"),
  scalacOptions --= Seq("-Xcheckinit"), // produces check-and-throw code on every val access

  libraryDependencies += "org.typelevel" %% "cats-effect-cps" % "0.5-99e8dbf-20240118T213220Z-SNAPSHOT",
)

lazy val scalaJsSettings = Seq(
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
  libraryDependencies += "org.portable-scala" %%% "portable-scala-reflect" % "1.1.2",
) ++ scalaJsBundlerSettings ++ scalaJsMacrotaskExecutor ++ scalaJsSecureRandom

lazy val scalaJsBundlerSettings = Seq(
  webpack / version               := "5.75.0",
  webpackCliVersion               := "5.0.0",
  startWebpackDevServer / version := "4.11.1",
  useYarn                         := true,
)

lazy val scalaJsMacrotaskExecutor = Seq(
  // https://github.com/scala-js/scala-js-macrotask-executor
  libraryDependencies += "org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.1"
)

lazy val scalaJsSecureRandom = Seq(
  // https://www.scala-js.org/news/2022/04/04/announcing-scalajs-1.10.0
  libraryDependencies += "org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0"
)

def readJsDependencies(baseDirectory: File, field: String): Seq[(String, String)] = {
  // val packageJson = ujson.read(IO.read(new File(s"$baseDirectory/package.json")))
  // packageJson(field).obj.mapValues(_.str.toString).toSeq
  Seq.empty
}

lazy val webapp = project
  .enablePlugins(
    ScalaJSPlugin,
    ScalaJSBundlerPlugin,
    ScalablyTypedConverterPlugin,
  )
  .dependsOn(api)
  .settings(commonSettings, scalaJsSettings)
  .settings(
    Test / test := {}, // skip tests, since we don't have any in this subproject. Remove this line, once there are tests
    libraryDependencies ++= Seq(
      "io.github.outwatch"   %%% "outwatch"             % versions.outwatch,
      "io.github.fun-stack"  %%% "fun-stack-client-web" % versions.funStack,
      "com.github.cornerman" %%% "colibri-router"       % versions.colibri,
      "com.github.cornerman" %%% "colibri-reactive"     % versions.colibri,
    ),
    Compile / npmDependencies ++= readJsDependencies(baseDirectory.value, "dependencies") ++ Seq(
      "snabbdom"               -> "github:outwatch/snabbdom.git#semver:0.7.5", // for outwatch, workaround for: https://github.com/ScalablyTyped/Converter/issues/293
      "reconnecting-websocket" -> "4.1.10",                                    // for fun-stack websockets, workaround for https://github.com/ScalablyTyped/Converter/issues/293 https://github.com/cornerman/mycelium/blob/6f40aa7018276a3281ce11f7047a6a3b9014bff6/build.sbt#74
      "jwt-decode"             -> "3.1.2",                                     // for fun-stack auth, workaround for https://github.com/ScalablyTyped/Converter/issues/293 https://github.com/cornerman/mycelium/blob/6f40aa7018276a3281ce11f7047a6a3b9014bff6/build.sbt#74
    ),
    stIgnore ++= List(
      "reconnecting-websocket",
      "snabbdom",
      "jwt-decode",
    ),
    Compile / npmDevDependencies   ++= readJsDependencies(baseDirectory.value, "devDependencies"),
    scalaJSUseMainModuleInitializer := true,
    webpackDevServerPort := sys.env
      .get("FRONTEND_PORT")
      .flatMap(port => scala.util.Try(port.toInt).toOption)
      .getOrElse(12345),
    webpackDevServerExtraArgs         := Seq("--color"),
    fullOptJS / webpackEmitSourceMaps := true,
    fastOptJS / webpackEmitSourceMaps := true,
    fastOptJS / webpackBundlingMode   := BundlingMode.LibraryOnly(),
    fastOptJS / webpackConfigFile     := Some(baseDirectory.value / "webpack.config.dev.js"),
    fullOptJS / webpackConfigFile     := Some(baseDirectory.value / "webpack.config.prod.js"),
  )

// shared project which contains api definitions.
// these definitions are used for type safe implementations
// of client and server
lazy val api = project
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings)
  .settings(
    Test / test := {}, // skip tests, since we don't have any in this subproject. Remove this line, once there are tests
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %%% "tapir-core"       % versions.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-circe" % versions.tapir,
    ),
  )

lazy val lambda = project
  .enablePlugins(
    ScalaJSPlugin,
    ScalaJSBundlerPlugin,
    ScalablyTypedConverterPlugin,
  )
  .dependsOn(api)
  .settings(commonSettings, scalaJsSettings, scalaJsBundlerSettings)
  .settings(
    Test / test := {}, // skip tests, since we don't have any in this subproject. Remove this line, once there are tests
    libraryDependencies ++= Seq(
      "io.github.fun-stack" %%% "fun-stack-lambda-ws-event-authorizer" % versions.funStack,
      "io.github.fun-stack" %%% "fun-stack-lambda-ws-rpc"              % versions.funStack,
      "io.github.fun-stack" %%% "fun-stack-lambda-http-rpc"            % versions.funStack,
      "io.github.fun-stack" %%% "fun-stack-lambda-http-api-tapir"      % versions.funStack,
      "io.github.fun-stack" %%% "fun-stack-backend"                    % versions.funStack,
      "com.lihaoyi"         %%% "pprint"                               % versions.pprint,
    ),
    Compile / npmDependencies ++= readJsDependencies(baseDirectory.value, "dependencies"),
    stIgnore ++= List(
      "aws-sdk"
    ),
    Compile / npmDevDependencies     ++= readJsDependencies(baseDirectory.value, "devDependencies"),
    fullOptJS / webpackEmitSourceMaps := true,
    fastOptJS / webpackEmitSourceMaps := true,
    fastOptJS / webpackConfigFile     := Some(baseDirectory.value / "webpack.config.dev.js"),
    fullOptJS / webpackConfigFile     := Some(baseDirectory.value / "webpack.config.prod.js"),
  )

import smithy4s.codegen.Smithy4sCodegenPlugin
val scribeVersion = "3.13.0"
val http4sVersion = "0.23.24"
lazy val httpServer = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.outr"                     %% "scribe-slf4j2"           % scribeVersion,
      "com.outr"                     %% "scribe"                  % scribeVersion,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s"         % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      // "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl"          % http4sVersion,
    )
  )
  .dependsOn(db)

lazy val dbCore = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
    )
  )

val quillVersion         = "4.8.0"
val schemaCrawlerVersion = "16.21.1"
lazy val codegen = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect"            % scalaVersion.value,
      "us.fatehi"      % "schemacrawler-tools"      % schemaCrawlerVersion,
      "us.fatehi"      % "schemacrawler-sqlite"     % schemaCrawlerVersion,
      "us.fatehi"      % "schemacrawler-postgresql" % schemaCrawlerVersion,
      "org.freemarker" % "freemarker"               % "2.3.32",
      "org.xerial"     % "sqlite-jdbc"              % "3.44.1.0",
      "org.postgresql" % "postgresql"               % "42.7.1",
      "io.getquill"   %% "quill-codegen-jdbc"       % quillVersion,
    )
  )

lazy val db = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.getquill" %% "quill-core"   % quillVersion,
      "io.getquill" %% "quill-doobie" % quillVersion,
      "org.xerial"   % "sqlite-jdbc"  % "3.44.1.0",
    ),
    Compile / sourceGenerators += Def.taskDyn {
      val outDir = (Compile / sourceManaged).value / "scala" / "codegen"
      Def.task {
        val _ = (codegen / Compile / run).toTask(s" ${outDir.getAbsolutePath}").value
        (outDir ** "*.scala").get
      }
    }.taskValue,
  )

addCommandAlias("prod", "; lambda/fullOptJS/webpack; webapp/fullOptJS/webpack")
addCommandAlias("prodf", "webapp/fullOptJS/webpack")
addCommandAlias("prodb", "lambda/fullOptJS/webpack")
addCommandAlias("dev", "devInitAll; devWatchAll; devDestroyFrontend")
addCommandAlias("devf", "devInitFrontend; devWatchFrontend; devDestroyFrontend") // compile only frontend
addCommandAlias("devb", "devInitBackend; devWatchBackend")                       // compile only backend

// devInitBackend needs to execute {...}/fastOptJS/webpack, to prepare all npm dependencies.
// We want to avoid this expensive preparation in the hot-reload process,
// and therefore only watch {...}/fastOptJS, where dependencies can be resolved from the previously prepared
// node_modules folder.
addCommandAlias("devInitBackend", "lambda/fastOptJS/webpack")
addCommandAlias("devInitFrontend", "webapp/fastOptJS/startWebpackDevServer; webapp/fastOptJS/webpack")
addCommandAlias("devInitAll", "devInitFrontend; devInitBackend")
addCommandAlias("devWatchFrontend", "~; webapp/fastOptJS")
addCommandAlias("devWatchBackend", "~; lambda/fastOptJS")
addCommandAlias("devWatchAll", "~; lambda/fastOptJS; webapp/fastOptJS; compile; Test/compile")
addCommandAlias("devDestroyFrontend", "webapp/fastOptJS/stopWebpackDevServer")
