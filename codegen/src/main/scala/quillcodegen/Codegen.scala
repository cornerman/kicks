package quillcodegen

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.codegen.jdbc.ComposeableTraitsJdbcCodegen
import io.getquill.codegen.model._

import java.io.File
import java.nio.file.Path
import scala.concurrent.Future

object Codegen {
  def generate(
                outDir: File,
                jdbcUrl: String,
                username: Option[String],
                password: Option[String],
                schemaFile: Option[String],
                pkgPrefix: String,
                naming: NameParser,
                nestedTrait: Boolean,
                generateQuerySchema: Boolean,
 ): Future[scala.Seq[Path]] = {
    val config = new HikariConfig()
    config.setJdbcUrl(jdbcUrl)
    username.foreach(config.setUsername)
    password.foreach(config.setPassword)

    val dataSource = new HikariDataSource(config)

    val gen = new ComposeableTraitsJdbcCodegen(dataSource, packagePrefix = pkgPrefix, nestedTrait = nestedTrait) {
      override def nameParser: NameParser = sanitizedNameParser(naming, shouldGenerateQuerySchema = generateQuerySchema)
      override def packagingStrategy: PackagingStrategy = PackagingStrategy.ByPackageHeader.TablePerSchema(pkgPrefix)
    }

    schemaFile.foreach { schemaFile =>
      val connection = dataSource.getConnection
      val statement = connection.createStatement()
      val schema = scala.io.Source.fromFile(schemaFile).mkString
      statement.execute(schema)
    }

    gen.writeAllFiles(s"${outDir.getPath}/${pkgPrefix.replace(".", "/")}")
  }

  private lazy val scalaKeywords = {
    val st = scala.reflect.runtime.universe.asInstanceOf[scala.reflect.internal.SymbolTable]
    st.nme.keywords.map(_.toString)
  }

  private def sanitizeScalaName(rawName: String): String = {
    val name = rawName.trim.replaceAll("(^[^a-zA-Z_]|[^a-zA-Z0-9_])", "_")
    if (scalaKeywords(name)) name + "_" else name
  }

  private def sanitizedNameParser(naming: NameParser, shouldGenerateQuerySchema: Boolean): NameParser = new LiteralNames {
      override def generateQuerySchemas: Boolean = shouldGenerateQuerySchema
      override def parseColumn(cm: JdbcColumnMeta): String = sanitizeScalaName(naming.parseColumn(cm))
      override def parseTable(tm: JdbcTableMeta): String = sanitizeScalaName(naming.parseTable(tm))
    }
}
