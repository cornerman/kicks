package quillcodegen.plugin

import io.getquill.codegen.model.{NameParser, SnakeCaseNames}
import quillcodegen.Codegen
import sbt.{io => _, _}
import sbt.Keys._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object CodegenPlugin extends AutoPlugin {
  override def trigger = noTrigger

  object autoImport {
    val packagePrefix = settingKey[String]("The package prefix for the generated code")
    val nestedTrait = settingKey[Boolean]("Whether to generate nested traits, default is false")
    val generateQuerySchema = settingKey[Boolean]("Whether to generate query schemas, default is true")
    val naming = settingKey[NameParser]("The naming parser to use, default is SnakeCaseNames")
    val schemaFile = settingKey[Option[String]]("An optional schema file for the database")
    val jdbcUrl = settingKey[String]("The jdbc URL for the database")
    val username = settingKey[Option[String]]("Optional database username")
    val password = settingKey[Option[String]]("Optional database password")
    val timeout = settingKey[Duration]("Timeout for the generate task")
  }
  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    nestedTrait := false,
    generateQuerySchema := true,
    naming := SnakeCaseNames,
    schemaFile := None,
    jdbcUrl := "jdbc:...",
    username := None,
    password := None,
    timeout := Duration.Inf,

    // Should be same as in build.sbt for codegen module
    libraryDependencies += "io.getquill" %% "quill-core" % "4.8.1",

    sourceGenerators += Def.task {
      val outDir = (Compile / sourceManaged).value / "scala" / "quillcodegen"

      val generation = Codegen.generate(
        outDir = outDir,
        pkgPrefix = packagePrefix.value,
        jdbcUrl = jdbcUrl.value,
        username = username.value,
        password = password.value,
        naming = naming.value,
        generateQuerySchema = generateQuerySchema.value,
        nestedTrait = nestedTrait.value,
        schemaFile = schemaFile.value,
      )

      val generatedFiles = Await.result(generation, timeout.value)

      generatedFiles.map(_.toFile)
    }.taskValue
  )
}