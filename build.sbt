Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"

Global / excludeLintKeys += webpackDevServerPort // TODO:

val versions = new {
  val outwatch = "1.0.0+4-ea3b233c-SNAPSHOT"
  val colibri  = "0.8.2"
  val scribe = "3.13.0"
  val http4s = "0.23.24"
  val smithy4s = "0.18.5"
  val quill = "4.8.1"
}

// Uncomment, if you want to use snapshot dependencies from sonatype or jitpack
// ThisBuild / resolvers ++= Seq(
//   "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
//   "Sonatype OSS Snapshots S01" at "https://s01.oss.sonatype.org/content/repositories/snapshots", // https://central.sonatype.org/news/20210223_new-users-on-s01/
//   "Jitpack" at "https://jitpack.io",
// )

val isCI = sys.env.get("CI").flatMap(value => scala.util.Try(value.toBoolean).toOption).getOrElse(false)

lazy val commonSettings = Seq(
  addCompilerPlugin("org.typelevel" % "kind-projector"     % "0.13.2" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"   %% "better-monadic-for" % "0.3.1"),

  // overwrite scalacOptions "-Xfatal-warnings" from https://github.com/DavidGregory084/sbt-tpolecat
  if (isCI) scalacOptions += "-Xfatal-warnings" else scalacOptions -= "-Xfatal-warnings",
//  scalacOptions ++= Seq("-Ymacro-annotations", "-Vimplicits", "-Vtype-diffs", "-Xasync"),
  scalacOptions --= Seq("-Xcheckinit"), // produces check-and-throw code on every val access

  //  libraryDependencies += "org.typelevel" %% "cats-effect-cps" % "0.5-99e8dbf-20240118T213220Z-SNAPSHOT",
)

lazy val scalaJsSettings = Seq(
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
  libraryDependencies += "org.portable-scala" %%% "portable-scala-reflect" % "1.1.2",

  // https://github.com/scala-js/scala-js-macrotask-executor
  libraryDependencies += "org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.1",
  // https://www.scala-js.org/news/2022/04/04/announcing-scalajs-1.10.0
  libraryDependencies += "org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0",

  // scalajs-bundler with webpack
  webpack / version               := "5.75.0",
  webpackCliVersion               := "5.0.0",
  startWebpackDevServer / version := "4.11.1",
  useYarn                         := true,
)

def readJsDependencies(baseDirectory: File, field: String): Seq[(String, String)] = {
  // val packageJson = ujson.read(IO.read(new File(s"$baseDirectory/package.json")))
  // packageJson(field).obj.mapValues(_.str.toString).toSeq
  Seq.empty
}

// shared project which contains api definitions.
// these definitions are used for type safe implementations
// of client and server
lazy val api = crossProject(JSPlatform, JVMPlatform)
  .in(file("projects/api"))
  .enablePlugins(smithy4s.codegen.Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
    ),
  )

lazy val db = project
  .in(file("projects/db"))
  .enablePlugins(quillcodegen.plugin.CodegenPlugin)
  .settings(commonSettings)
  .settings(
    quillcodegenPackagePrefix := "kicks.db",
    quillcodegenJdbcUrl := "jdbc:sqlite:/tmp/kicks-quillcodegen.db",

    //quillcodegenSetupTask := Def.taskDyn {
    //  IO.delete(file(quillcodegenJdbcUrl.value.stripPrefix("jdbc:sqlite:")))
    //  executeSqlFile(file("./schema.sql"))
    //},
    quillcodegenSetupTask := {
      val dbFile = quillcodegenJdbcUrl.value.stripPrefix("jdbc:sqlite:")
      val command = s"rm -f ${dbFile} && sqlite3 ${dbFile} < ./schema.sql"
      require(sys.process.Process(Seq("sh", "-c", command)).! == 0, "Schema setup failed")
    },

    libraryDependencies ++= Seq(
      "io.getquill"   %% "quill-doobie"       % versions.quill,
      "org.flywaydb" % "flyway-core" % "10.6.0",
      "org.xerial"       % "sqlite-jdbc"          % "3.44.1.0",
    )
  )

lazy val httpServer = project
  .in(file("projects/httpServer"))
  .dependsOn(api.jvm, db)
  .settings(commonSettings)
  .settings(
    assembly / assemblyMergeStrategy := {
      //https://stackoverflow.com/questions/73727791/sbt-assembly-logback-does-not-work-with-%C3%BCber-jar
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", _*) => MergeStrategy.discard
      case "module-info.class" => MergeStrategy.discard
      case x => (assembly / assemblyMergeStrategy).value(x)
    },
    libraryDependencies ++= Seq(
      "com.outr"                     %% "scribe-slf4j2"           % versions.scribe,
      "com.outr"                     %% "scribe"                  % versions.scribe,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s"         % versions.smithy4s,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % versions.smithy4s,
      "org.http4s" %% "http4s-ember-server" % versions.http4s,
      "org.http4s" %% "http4s-dsl"          % versions.http4s,
    )
  )

lazy val webapp = project
  .in(file("projects/webapp"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin, ScalablyTypedConverterPlugin)
  .dependsOn(api.js)
  .settings(commonSettings, scalaJsSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.outwatch"   %%% "outwatch"             % versions.outwatch,
      "com.github.cornerman" %%% "colibri-router"       % versions.colibri,
      "com.github.cornerman" %%% "colibri-reactive"     % versions.colibri,
    ),
    Compile / npmDependencies ++= readJsDependencies(baseDirectory.value, "dependencies") ++ Seq(
      "snabbdom"               -> "github:outwatch/snabbdom.git#semver:0.7.5", // for outwatch, workaround for: https://github.com/ScalablyTyped/Converter/issues/293
    ),
    stIgnore ++= List(
      "snabbdom",
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
