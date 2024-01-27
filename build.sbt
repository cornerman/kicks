import scala.collection.immutable.Seq

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.1"

Global / excludeLintKeys += webpackDevServerPort // TODO:

ThisBuild / libraryDependencySchemes += "org.tpolecat" %% "doobie-core" % "always"

val versions = new {
  val outwatch      = "1.0.0+4-ea3b233c-SNAPSHOT"
  val colibri       = "0.8.2"
  val scribe        = "3.13.0"
  val http4s        = "0.23.24"
  val smithy4s      = "0.18.5"
  val quill         = "4.8.1"
  val dottyCpsAsync = "0.9.19"
}

// Uncomment, if you want to use snapshot dependencies from sonatype or jitpack
ThisBuild / resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype OSS Snapshots S01" at "https://s01.oss.sonatype.org/content/repositories/snapshots", // https://central.sonatype.org/news/20210223_new-users-on-s01/
//   "Jitpack" at "https://jitpack.io",
)

val isCI = sys.env.get("CI").flatMap(value => scala.util.Try(value.toBoolean).toOption).getOrElse(false)

lazy val commonSettings = Seq(
  // Default scalacOptions set by: https://github.com/DavidGregory084/sbt-tpolecat
  if (isCI) scalacOptions += "-Xfatal-warnings" else scalacOptions -= "-Xfatal-warnings",
  scalacOptions --= Seq("-Xcheckinit"), // produces check-and-throw code on every val access
  scalacOptions ++= Seq(
    // TODO: https://github.com/zio/zio-quill/issues/2639
    "-Wconf:msg=Questionable row-class found:s"
  ),
  libraryDependencies ++= Seq(
    "com.github.rssh"  %% "dotty-cps-async"               % versions.dottyCpsAsync,
    "com.github.rssh" %%% "cps-async-connect-cats-effect" % versions.dottyCpsAsync,
    "com.github.rssh" %%% "cps-async-connect-fs2"         % versions.dottyCpsAsync,
    "com.outr"         %% "scribe"                        % versions.scribe,
  ),
)

lazy val scalaJsSettings = Seq(
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
  libraryDependencies += ("org.portable-scala" %%% "portable-scala-reflect"      % "1.1.2").cross(CrossVersion.for3Use2_13),
  libraryDependencies += "org.scala-js"        %%% "scala-js-macrotask-executor" % "1.1.1",
  libraryDependencies += ("org.scala-js"       %%% "scalajs-java-securerandom"   % "1.0.0").cross(CrossVersion.for3Use2_13),

  // scalajs-bundler with webpack
  webpack / version               := "5.75.0",
  webpackCliVersion               := "5.0.0",
  startWebpackDevServer / version := "4.11.1",
  useYarn                         := true,
)

lazy val rpc = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("projects/rpc"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.cornerman" %%% "sloth" % "0.7.1+15-90fae7c7-SNAPSHOT"
    )
  )

lazy val api = project
  .enablePlugins(smithy4s.codegen.Smithy4sCodegenPlugin)
  .in(file("projects/api"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % versions.smithy4s
    )
  )

lazy val db = project
  .in(file("projects/db"))
  .enablePlugins(quillcodegen.plugin.CodegenPlugin)
  .settings(commonSettings)
  .settings(
    quillcodegenPackagePrefix := "kicks.db",
    quillcodegenJdbcUrl       := "jdbc:sqlite:/tmp/kicks-quillcodegen.db",
    quillcodegenSetupTask := {
      val dbFile  = quillcodegenJdbcUrl.value.stripPrefix("jdbc:sqlite:")
      val command = s"rm -f ${dbFile} && sqlite3 ${dbFile} < ./schema.sql"
      require(sys.process.Process(Seq("sh", "-c", command)).! == 0, "Schema setup failed")
    },
//    quillcodegenSetupTask := Def.taskDyn {
//      IO.delete(file(quillcodegenJdbcUrl.value.stripPrefix("jdbc:sqlite:")))
//      executeSqlFile(file("./schema.sql"))
//    },

    libraryDependencies ++= Seq(
      "org.xerial"            % "sqlite-jdbc"  % "3.44.1.0",
      "io.getquill"          %% "quill-doobie" % versions.quill,
      "org.tpolecat"         %% "doobie-core"  % "1.0.0-RC5",
      "org.flywaydb"          % "flyway-core"  % "10.6.0",
      "com.github.jsqlparser" % "jsqlparser"   % "4.8",
    ),
  )

lazy val httpServer = project
  .in(file("projects/httpServer"))
  .dependsOn(api, rpc.jvm, db)
  .settings(commonSettings)
  .settings(
    reStart / javaOptions       := Seq("-Djava.library.path=/home/cornerman/projects/kicks/projects/db/lib-system"),
    Compile / run / fork        := true,
    Compile / run / javaOptions := (reStart / javaOptions).value,
    assembly / assemblyMergeStrategy := {
      // https://stackoverflow.com/questions/73727791/sbt-assembly-logback-does-not-work-with-%C3%BCber-jar
      case PathList("META-INF", "services", _*)           => MergeStrategy.filterDistinctLines
      case PathList("META-INF", _*) | "module-info.class" => MergeStrategy.discard
      case x                                              => (assembly / assemblyMergeStrategy).value(x)
    },
    libraryDependencies ++= Seq(
      "com.outr"                     %% "scribe-slf4j2"           % versions.scribe,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % versions.smithy4s,
      "org.http4s"                   %% "http4s-ember-client"     % versions.http4s,
      "org.http4s"                   %% "http4s-ember-server"     % versions.http4s,
      "org.http4s"                   %% "http4s-dsl"              % versions.http4s,
    ),
  )

lazy val webapp = project
  .in(file("projects/webapp"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(rpc.js)
  .settings(commonSettings, scalaJsSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.outwatch"   %%% "outwatch"         % versions.outwatch,
      "com.github.cornerman" %%% "colibri-router"   % versions.colibri,
      "com.github.cornerman" %%% "colibri-reactive" % versions.colibri,
    ),
    Compile / npmDependencies ++= Seq(
      "snabbdom" -> "github:outwatch/snabbdom.git#semver:0.7.5" // for outwatch, workaround for: https://github.com/ScalablyTyped/Converter/issues/293
    ),
    Compile / npmDevDependencies ++= Seq(
      "@fun-stack/fun-pack" -> "^0.3.5",
      "autoprefixer"        -> "^10.4.12",
      "daisyui"             -> "^3.0.3",
      "postcss"             -> "^8.4.16",
      "postcss-loader"      -> "^7.0.1",
      "tailwindcss"         -> "^3.1.8",
    ),
//    stIgnore ++= List(
//      "snabbdom",
//    ),
    scalaJSUseMainModuleInitializer   := true,
    webpackDevServerPort              := sys.env.get("FRONTEND_PORT").flatMap(port => scala.util.Try(port.toInt).toOption).getOrElse(12345),
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
