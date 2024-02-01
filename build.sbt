import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.jsenv.{Input, JSEnv, RunConfig}
import org.scalajs.linker.interface.ModuleSplitStyle

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "kicks"
ThisBuild / scalaVersion := "3.3.1"

val versions = new {
  val outwatch      = "1.0.0+4-ea3b233c-SNAPSHOT"
  val colibri       = "0.8.3"
  val scribe        = "3.13.0"
  val http4s        = "0.23.24"
  val smithy4s      = "0.18.5"
  val quill         = "4.8.1"
  val dottyCpsAsync = "0.9.19"
  val sttpOAuth2    = "0.17.0"
  val pac4j         = "5.7.2"
  val jsoniter      = "2.28.0"
}

ThisBuild / libraryDependencySchemes += "org.tpolecat" %% "doobie-core" % "always"

// Uncomment, if you want to use snapshot dependencies from sonatype
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

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
  libraryDependencies += ("org.portable-scala" %%% "portable-scala-reflect"      % "1.1.2").cross(CrossVersion.for3Use2_13),
  libraryDependencies += ("org.scala-js"       %%% "scalajs-java-securerandom"   % "1.0.0").cross(CrossVersion.for3Use2_13),
  libraryDependencies += "org.scala-js"        %%% "scala-js-macrotask-executor" % "1.1.1",
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
    // quillcodegenSetupTask := {
    //  val dbFile  = quillcodegenJdbcUrl.value.stripPrefix("jdbc:sqlite:")
    //  val command = s"rm -f ${dbFile} && sqlite3 ${dbFile} < ./schema.sql"
    //  require(sys.process.Process(Seq("sh", "-c", command)).! == 0, "Schema setup failed")
    // },
    quillcodegenSetupTask := Def.taskDyn {
      IO.delete(file(quillcodegenJdbcUrl.value.stripPrefix("jdbc:sqlite:")))
      executeSqlFile(file("./schema.sql"))
    },
    libraryDependencies ++= Seq(
      "org.xerial"    % "sqlite-jdbc"  % "3.44.1.0",
      "io.getquill"  %% "quill-doobie" % versions.quill,
      "org.tpolecat" %% "doobie-core"  % "1.0.0-RC5",
      "org.flywaydb"  % "flyway-core"  % "10.6.0",
    ),
  )

lazy val httpServer = project
  .in(file("projects/httpServer"))
  .enablePlugins(GraalVMNativeImagePlugin)
  .dependsOn(api, rpc.jvm, db)
  .settings(commonSettings)
  .settings(
    Compile / run / fork := true,
    assembly / assemblyMergeStrategy := {
      // https://stackoverflow.com/questions/73727791/sbt-assembly-logback-does-not-work-with-%C3%BCber-jar
      case PathList("META-INF", "services", _*)           => MergeStrategy.filterDistinctLines
      case PathList("META-INF", _*) | "module-info.class" => MergeStrategy.discard
      case x                                              => MergeStrategy.last
    },
    libraryDependencies ++= Seq(
      "com.outr"                              %% "scribe-slf4j2"           % versions.scribe,
      "com.disneystreaming.smithy4s"          %% "smithy4s-http4s-swagger" % versions.smithy4s,
      "org.http4s"                            %% "http4s-ember-client"     % versions.http4s,
      "org.http4s"                            %% "http4s-ember-server"     % versions.http4s,
      "org.http4s"                            %% "http4s-dsl"              % versions.http4s,
      "com.outr"                              %% "scalapass"               % "1.2.8",
      "org.pac4j"                              % "pac4j-core"              % versions.pac4j,
      "org.pac4j"                              % "pac4j-cas"               % versions.pac4j,
      "org.pac4j"                              % "pac4j-http"              % versions.pac4j,
      "org.pac4j"                              % "pac4j-jwt"               % versions.pac4j,
      "org.pac4j"                              % "pac4j-oauth"             % versions.pac4j,
      "org.pac4j"                             %% "http4s-pac4j"            % "4.3.0-SNAPSHOT",
      "com.auth0"                              % "java-jwt"                % "4.4.0",
      "com.auth0"                              % "jwks-rsa"                % "0.22.1",
      "com.github.cornerman"                  %% "http4s-jsoniter"         % "0.1.1",
      "com.github.cornerman"                  %% "keratin-authn-backend"   % "0.0.0+5-129e5a16-SNAPSHOT",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"     % versions.jsoniter,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros"   % versions.jsoniter % "compile-internal",
    ),
  )

lazy val webapp = project
  .in(file("projects/webapp"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .dependsOn(rpc.js)
  .settings(commonSettings, scalaJsSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.outwatch"   %%% "outwatch"         % versions.outwatch,
      "com.github.cornerman" %%% "colibri-router"   % versions.colibri,
      "com.github.cornerman" %%% "colibri-reactive" % versions.colibri,
      "org.scalatest"        %%% "scalatest"        % "3.2.17" % Test,
    ),

    // https://www.scala-js.org/doc/tutorial/scalajs-vite.html
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule).withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("kicks")))
    },

    // scalablytyped
    externalNpm := baseDirectory.value,
    stIgnore ++= List(
      "snabbdom"
    ),
  )

addCommandAlias("dev", "webapp/fastLinkJS; httpServer/reStart")
