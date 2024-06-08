import org.scalajs.linker.interface.ModuleSplitStyle

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / organization := "kicks"
ThisBuild / scalaVersion := "3.4.1"

enablePlugins(GitVersioning)
git.useGitDescribe := true

val versions = new {
  val scribe         = "3.13.0"
  val dottyCpsAsync  = "0.9.19"
  val smithy4s       = "0.18.5"
  val jsoniter       = "2.28.0"
  val http4s         = "0.23.24"
  val http4sJsoniter = "0.1.1"
  val authn          = "0.1.2"
  val outwatch       = "1.0.0+12-c2498e95-SNAPSHOT"
  val colibri        = "0.8.4"
  val monocle        = "3.2.0"
}

// Uncomment, if you want to use snapshot dependencies from sonatype
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

val isCI = sys.env.get("CI").contains("true")

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-encoding",
    "utf8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:higherKinds",
    "-Ykind-projector",
    "-Wunused:imports,privates,params,locals,implicits,explicits",
    "-Wnonunit-statement",
    // default imports in every scala file. we use the scala defaults + cps for direct syntax with lift/unlift/!
    "-Yimports:java.lang,scala,scala.Predef,cps.syntax,cps.syntax.monadless,cps.monads.catsEffect",
  ),
  scalacOptions ++= (if (isCI) Seq("-Xfatal-warnings") else Seq.empty),
  libraryDependencies ++= Seq(
    "com.github.rssh" %%% "dotty-cps-async"               % versions.dottyCpsAsync,
    "com.github.rssh" %%% "cps-async-connect-cats-effect" % versions.dottyCpsAsync,
    "com.github.rssh" %%% "cps-async-connect-fs2"         % versions.dottyCpsAsync,
    "com.outr"        %%% "scribe"                        % versions.scribe,
  ),
)

lazy val scalaJsSettings = Seq(
  libraryDependencies += ("org.portable-scala" %%% "portable-scala-reflect"      % "1.1.2").cross(CrossVersion.for3Use2_13),
  libraryDependencies += ("org.scala-js"       %%% "scalajs-java-securerandom"   % "1.0.0").cross(CrossVersion.for3Use2_13),
  libraryDependencies += "org.scala-js"        %%% "scala-js-macrotask-executor" % "1.1.1",
)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .enablePlugins(BuildInfoPlugin)
  .in(file("modules/shared"))
  .settings(commonSettings)
  .settings(
    buildInfoPackage := "sbt",
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core"   % versions.jsoniter,
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-macros" % versions.jsoniter % "compile-internal",
    ),
  )

lazy val rpc = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(shared)
  .in(file("modules/rpc"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.cornerman" %%% "sloth" % "0.7.1+15-90fae7c7-SNAPSHOT"
    )
  )

lazy val api = project
  .enablePlugins(smithy4s.codegen.Smithy4sCodegenPlugin)
  .in(file("modules/api"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % versions.smithy4s
    )
  )

lazy val db = project
  .in(file("modules/db"))
  .dependsOn(shared.jvm)
  .enablePlugins(dbcodegen.plugin.DbCodegenPlugin)
  .settings(commonSettings)
  .settings(
    dbcodegenTemplateFiles := Seq(file("schema.scala.ssp")),
    dbcodegenJdbcUrl := "jdbc:sqlite:file::memory:?cache=shared",
    dbcodegenSetupTask := { db =>
      db.executeSqlFile(file("./schema.sql"))
    },
    libraryDependencies ++= Seq(
      "org.xerial"            % "sqlite-jdbc"        % "3.46.0.0",
      "com.augustnagro"      %% "magnum"             % "1.2.0",
      "com.github.cornerman" %% "magnum-cats-effect" % "0.0.0+4-988d4494-SNAPSHOT",
      "org.flywaydb"          % "flyway-core"        % "10.6.0",
    ),
  )

lazy val httpServer = project
  .in(file("modules/httpServer"))
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
      "com.outr"                     %% "scribe-slf4j2"           % versions.scribe,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % versions.smithy4s,
      "org.http4s"                   %% "http4s-ember-client"     % versions.http4s,
      "org.http4s"                   %% "http4s-ember-server"     % versions.http4s,
      "org.http4s"                   %% "http4s-dsl"              % versions.http4s,
      "com.github.cornerman"         %% "http4s-jsoniter"         % versions.http4sJsoniter,
      "com.github.cornerman"         %% "keratin-authn-backend"   % versions.authn,
      "io.github.arainko"            %% "ducktape"                % "0.2.1",
      "dev.optics"                   %% "monocle-core"            % versions.monocle,
      "dev.optics"                   %% "monocle-macro"           % versions.monocle,
    ),
  )

lazy val webapp = project
  .in(file("modules/webapp"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .enablePlugins(webcodegen.plugin.WebCodegenPlugin)
  .dependsOn(rpc.js, shared.js)
  .settings(commonSettings, scalaJsSettings)
  .settings(
    webcodegenCustomElements := Seq(
      webcodegen
        .CustomElements("shoelace", jsonFile = file("modules/webapp/node_modules/@shoelace-style/shoelace/dist/custom-elements.json")),
      webcodegen.CustomElements("emojipicker", jsonFile = file("modules/webapp/node_modules/emoji-picker-element/custom-elements.json")),
    ),
    webcodegenTemplates := Seq(
      webcodegen.Template.Outwatch
    ),
    libraryDependencies ++= Seq(
      "io.github.outwatch"   %%% "outwatch"               % versions.outwatch,
      "com.github.cornerman" %%% "colibri-router"         % versions.colibri,
      "com.github.cornerman" %%% "colibri-reactive"       % versions.colibri,
      "com.github.cornerman" %%% "keratin-authn-frontend" % versions.authn,
      "org.scalatest"        %%% "scalatest"              % "3.2.17" % Test,
    ),

    // https://www.scala-js.org/doc/tutorial/scalajs-vite.html
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule).withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("kicks")))
    },

    // scalablytyped
    externalNpm := baseDirectory.value,
    // TODO workaround to explicitly enable scalablytyped for specific packages:
    // https://github.com/ScalablyTyped/Converter/pull/632
    stIgnore ++= {
      val deps = ujson.read(IO.read(baseDirectory.value / "package.json"))("dependencies").obj.keys.toList
      deps.diff(
        List(
          // explicitly select which typescript packages need facades:
        )
      )
    },
  )

addCommandAlias("dev", "~; httpServer/reStart; webapp/fastLinkJS")
addCommandAlias("devf", "httpServer/reStart; ~webapp/fastLinkJS")
addCommandAlias("prod", "httpServer/assembly; webapp/fullLinkJS")
