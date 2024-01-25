package kicks.db

import cats.implicits._
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.util.TablesNamesFinder

import scala.jdk.CollectionConverters._

class SqlInterpreter {
  def interpret(sql: String): Either[String, Unit] = {
    ???
//    Either.catchNonFatal(CCJSqlParserUtil.parse(sql)).map { statement =>
//      val tablesNamesFinder = new TablesNamesFinder {
//        override def visit(tableColumn: Column): Unit = {
//          if (allowColumnProcessing && tableColumn.getTable != null && tableColumn.getTable.getName != null) visit(tableColumn.getTable)
//        }
//      }
//      val tables = tablesNamesFinder.getTables(statement)
//      tables.asScala.head
//    }
  }

}
