package kicks.codegen

import io.getquill.codegen.jdbc.{ComposeableTraitsJdbcCodegen, SimpleJdbcCodegen}
import io.getquill.codegen.model
import io.getquill.codegen.model.{CustomNames, NameParser, PackagingStrategy, SnakeCaseNames}
import org.postgresql.ds.PGSimpleDataSource
import org.sqlite.SQLiteDataSource

object Main {
  def main(args: Array[String]): Unit = {
    val outDir = args.headOption.fold(".")(_.stripSuffix("/"))

    val dataSource = new SQLiteDataSource()
    dataSource.setUrl("jdbc:sqlite:kicks.db")
//    val dataSource = new PGSimpleDataSource()
//    dataSource.setUser("postgres")
//    dataSource.setPassword("test")
//    dataSource.setURL("jdbc:postgresql://localhost/postgres?ssl=false")

    val gen = new ComposeableTraitsJdbcCodegen(dataSource, packagePrefix = "kicks.db", nestedTrait = false) {

      override def nameParser: NameParser = sanitizedNameParser(SnakeCaseNames)

      override def packagingStrategy: PackagingStrategy = PackagingStrategy.ByPackageHeader.TablePerSchema(packagePrefix)

    }

    gen.writeFiles(s"$outDir/${gen.packagePrefix.replace(".", "/")}")
  }

  private lazy val scalaKeywords = {
    val st = scala.reflect.runtime.universe.asInstanceOf[scala.reflect.internal.SymbolTable]
    st.nme.keywords.map(_.toString)
  }

  private def sanitizeScalaName(rawName: String): String = {
    val name = rawName.trim.replaceAll("(^[^a-zA-Z_]|[^a-zA-Z0-9_])", "_")
    if (scalaKeywords(name)) name + "_" else name
  }

  private def sanitizedNameParser(naming: NameParser): NameParser = CustomNames(
    columnParser = cm => sanitizeScalaName(naming.parseColumn(cm)),
    tableParser = tm => sanitizeScalaName(naming.parseTable(tm)),
  )
}
