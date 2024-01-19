package kicks.codegen

import schemacrawler.tools.databaseconnector.{DatabaseConnectorRegistry, DatabaseUrlConnectionOptions}
import freemarker.template._
import kicks.codegen.Codegen.toCamelCase
import schemacrawler.crawl.SchemaCrawlerExt
import schemacrawler.schema._
import schemacrawler.schemacrawler._
import schemacrawler.tools.utility.SchemaCrawlerUtility
import us.fatehi.utility.datasource.{DatabaseConnectionSource, MultiUseUserCredentials}

import java.io.FileWriter
import java.sql.{JDBCType, SQLType, Types}
import scala.jdk.CollectionConverters._
import scala.util.Try

object Codegen {
  case class DataColumn(
    getName: String,
    getScalaType: String,
  ) {
    def getScalaName = sanitizeScalaName(toCamelCase(getName))
  }
  case class DataRow(
    getName: String,
    getAlias: Option[String],
    getColumns: java.util.Collection[DataColumn],
  ) {
    def getScalaName = sanitizeScalaName(getAlias.getOrElse(toPascalCase(getName)))
  }
  case class DataTable(
    getName: String,
    getRowBase: DataRow,
    getRowVariations: java.util.Collection[DataRow],
    isView: Boolean,
  )
  case class DataEnumValue(
    getValue: String,
  ) {
    def getScalaName = sanitizeScalaName(toPascalCase(getValue))
  }
  case class DataEnum(
    getName: String,
    getValues: java.util.Collection[DataEnumValue],
  ) {
    def getScalaName = sanitizeScalaName(toPascalCase(getName))
  }
  case class DataSchema(
    getName: String,
    getTables: java.util.Collection[DataTable],
    getEnums: java.util.Collection[DataEnum],
  ) {
    def getScalaName = sanitizeScalaName(toCamelCase(getName))
  }

  def run(args: Array[String]): Unit = {
    val outDir = args.headOption.map(_.stripSuffix("/")).getOrElse(".")
    println("RUNNING ON " + outDir)

    // schema crawler options

    // val jdbcUrl     = "jdbc:sqlite:kicks.db"
    // val credentials = new MultiUseUserCredentials()
    val jdbcUrl     = "jdbc:postgresql://localhost/postgres"
    val credentials = new MultiUseUserCredentials("postgres", "test")
    val connection =
      DatabaseConnectorRegistry
        .getDatabaseConnectorRegistry()
        .findDatabaseConnectorFromUrl(jdbcUrl)
        .newDatabaseConnectionSource(new DatabaseUrlConnectionOptions(jdbcUrl), credentials)

    val schemaCrawlerOptions = SchemaCrawlerOptionsBuilder
      .newSchemaCrawlerOptions()
      .withLoadOptions(LoadOptionsBuilder.builder().withInfoLevel(InfoLevel.maximum).toOptions)
      .withLimitOptions(LimitOptionsBuilder.builder().toOptions)

    val catalog = SchemaCrawlerUtility.getCatalog(connection, schemaCrawlerOptions)

    // freemarker template configuration

    val templateConfig = new Configuration(Configuration.VERSION_2_3_32)
//    templateConfig.setClassForTemplateLoading(this.getClass, "")
    templateConfig.setDirectoryForTemplateLoading(new java.io.File("codegen/src/main/resources"))
    templateConfig.setDefaultEncoding("UTF-8")
    templateConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
    templateConfig.setLogTemplateExceptions(false)
    templateConfig.setWrapUncheckedExceptions(true)

    // run templates

    val template = templateConfig.getTemplate("table_case_classes.ftl")

    catalog.getSchemas.asScala.foreach { schema =>
      val schemaName = Option(schema.getName).getOrElse("")

      val dataSchema = convertToDataSchema(schema, connection, catalog.getTables(schema))

      if (!dataSchema.getTables.isEmpty) {
        val data = new java.util.HashMap[String, Object]()
        data.put("schema", dataSchema)

        template.process(data, new FileWriter(s"$outDir/TableCaseClasses${schemaName.capitalize}.scala"))
      }
    }
  }

  private def convertToDataSchema(
    schema: Schema,
    connection: DatabaseConnectionSource,
    tables: java.util.Collection[Table],
  ) = {
    val (dataEnums, dataTables) = tables.asScala.map { table =>
      val usableColumns = table.getColumns.asScala.filter(column => !column.isHidden)
      val writeColumns = usableColumns.collect {
        case column if !column.isGenerated => column.getName
      }.toSet
      val autoColumns = usableColumns.collect {
        case column if column.hasDefaultValue || column.isAutoIncremented => column.getName
      }.toSet
      val indexColumns = table.getIndexes.asScala.map { index =>
        (Some(index), index.getColumns.asScala.map(_.getName).toSet)
      } ++ Option(table.getPrimaryKey).map(pk => (None, pk.getConstrainedColumns.asScala.map(_.getName).toSet))

      val (readEnum, readTable) = {
        val (dataEnums, dataColumns) = usableColumns.collect { case column =>
          val (scalaType, dataEnum) = columnToScalaType(schema, connection, column)
          (dataEnum, DataColumn(column.getName, scalaType))
        }.unzip

        (dataEnums.flatten, DataRow(table.getName, None, dataColumns.asJava))
      }

      val isView = table.isInstanceOf[View]

      val (insertEnums, insertTables) =
        if (isView) (Seq.empty, Seq.empty)
        else
          autoColumns
            .subsets()
            .toSeq
            .map { subAutoColumns =>
              val (dataEnums, dataColumns) = usableColumns.collect {
                case column if !subAutoColumns(column.getName) && writeColumns(column.getName) =>
                  val (scalaType, dataEnum) = columnToScalaType(schema, connection, column)
                  (dataEnum, DataColumn(column.getName, scalaType))
              }.unzip

              val tableClassAlias = {
                if (subAutoColumns.isEmpty) "Insert"
                else
                  s"InsertAuto_${usableColumns.collect {
                    case column if subAutoColumns(column.getName) => toCamelCase(column.getName)
                  }.mkString("_")}"
              }

              (dataEnums.flatten, DataRow(table.getName, Some(tableClassAlias), dataColumns.asJava))
            }
            .unzip

      val (idEnums, idTables) = indexColumns.map { case (index, indexColumns) =>
        val (dataEnums, dataColumns) = usableColumns.collect {
          case column if indexColumns(column.getName) =>
            val (scalaType, dataEnum) = columnToScalaType(schema, connection, column)
            (dataEnum, DataColumn(column.getName, scalaType))
        }.unzip

        val tableClassAlias = index match {
          case Some(_) =>
            s"ID_${usableColumns.collect {
              case column if indexColumns(column.getName) => toCamelCase(column.getName)
            }.mkString("_")}"
          case None => "ID"
        }

        (dataEnums.flatten, DataRow(table.getName, Some(tableClassAlias), dataColumns.asJava))
      }.unzip

      (
        (Vector(readEnum) ++ idEnums ++ insertEnums).flatten,
        DataTable(
          table.getName,
          readTable,
          (idTables ++ insertTables).toSeq.distinct.asJava,
          isView = isView,
        ),
      )
    }.unzip

    DataSchema(
      Option(schema.getName).getOrElse(""),
      dataTables.toSeq.distinct.asJava,
      dataEnums.flatten.toSeq.distinct.asJava,
    )
  }

  private def columnToScalaType(
    schema: Schema,
    connection: DatabaseConnectionSource,
    column: Column,
  ): (String, Option[DataEnum]) = {
    val tpe = column.getColumnDataType

    val (enumValues, arrayElementType) = (tpe.getJavaSqlType.getVendorTypeNumber.intValue(), tpe.getName) match {
      // TODO: specific to Postgres
      case (Types.ARRAY, s"_${elementType}") =>
        val columnDataType         = SchemaCrawlerExt.newColumnDataType(schema, elementType, DataTypeType.system)
        val conn                   = connection.get()
        val schemaRetrievalOptions = SchemaCrawlerUtility.matchSchemaRetrievalOptions(connection)
        connection.releaseConnection(conn)
        val enumType = schemaRetrievalOptions.getEnumDataTypeHelper.getEnumDataTypeInfo(column, columnDataType, conn)
        (enumType.getEnumValues, Some(elementType))
      case (_, _) =>
        (tpe.getEnumValues, None)
    }

    val (baseScalaType, dataEnum) = enumValues match {
      case enumValues if enumValues.isEmpty =>
        val targetType = arrayElementType
          .flatMap(localTypeNameToSqlType)
          .orElse(localTypeNameToSqlType(tpe.getName))
          .getOrElse(tpe.getJavaSqlType)
        (sqlToScalaType(targetType), None)
      case enumValues =>
        val targetTypeName = arrayElementType.getOrElse(tpe.getName)
        (targetTypeName, Some(DataEnum(targetTypeName, enumValues.asScala.map(DataEnumValue(_)).asJava)))
    }

    val scalaTypeWithArray = if (arrayElementType.isDefined) s"Vector[${baseScalaType}]" else baseScalaType

    val scalaType = if (column.isNullable) s"Option[${scalaTypeWithArray}]" else scalaTypeWithArray

    (scalaType, dataEnum)
  }

  private def sqlToScalaType(tpe: SQLType): String = tpe.getVendorTypeNumber.intValue() match {
    case Types.OTHER | Types.VARCHAR | Types.CHAR | Types.LONGVARCHAR | Types.NVARCHAR | Types.LONGNVARCHAR => "String"
    case Types.INTEGER                                                                                      => "Int"
    case Types.TINYINT                                                                                      => "Byte"
    case Types.SMALLINT                                                                                     => "Short"
    case Types.BIGINT                                                                                       => "Long"
    case Types.DECIMAL | Types.NUMERIC                                                                      => "java.math.BigDecimal"
    case Types.REAL | Types.FLOAT                                                                           => "Float"
    case Types.DOUBLE                                                                                       => "Double"
    case Types.BOOLEAN | Types.BIT                                                                          => "Boolean"
    case Types.TIME                                                                                         => "java.time.LocalTime"
    case Types.DATE                                                                                         => "java.time.LocalDate"
    case Types.TIMESTAMP                                                                                    => "java.time.LocalDatetime"
    case Types.TIME_WITH_TIMEZONE                                                                           => "java.time.OffsetTime"
    case Types.TIMESTAMP_WITH_TIMEZONE                                                                      => "java.time.OffsetDatetime"
    case Types.VARBINARY | Types.LONGVARBINARY | Types.BINARY                                               => "Array[Byte]"
    case Types.BLOB                                                                                         => "java.sql.Blob"
    case Types.CLOB                                                                                         => "java.sql.Clob"
    case Types.NCLOB                                                                                        => "java.sql.NClob"
    case Types.ROWID                                                                                        => "java.sql.RowId"
    case tpeNum                                                                                             => throw new IllegalArgumentException(s"Unexpected sql type: ${tpe.getName} ($tpeNum)")
  }

  private def localTypeNameToSqlType(localTypeName: String): Option[SQLType] = localTypeName.toUpperCase match {
    // TODO: specific to Postgres
    case "TEXT" | "UUID"  => Some(JDBCType.LONGVARCHAR)
    case "JSON" | "JSONB" => Some(JDBCType.OTHER)
    case "INT2"           => Some(JDBCType.SMALLINT)
    case "INT" | "INT4"   => Some(JDBCType.INTEGER)
    case "INT8"           => Some(JDBCType.BIGINT)
    case "FLOAT4"         => Some(JDBCType.FLOAT)
    case "FLOAT8"         => Some(JDBCType.DOUBLE)
    case "MONEY"          => Some(JDBCType.DECIMAL)
    case other            => Try(JDBCType.valueOf(other)).toOption
  }

  private val scalaKeywords = {
    val st = scala.reflect.runtime.universe.asInstanceOf[scala.reflect.internal.SymbolTable]
    st.nme.keywords.map(_.toString)
  }

  private def sanitizeScalaName(rawName: String): String = {
    val name              = rawName.trim
    def isValidIdentifier = name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")
    if (name.isEmpty || (!scalaKeywords(name) && isValidIdentifier)) name else s"`$name`"
  }

//  private def toPascalCase(str: String): String = {
//    val x = regex""
//    str.trim.split("(-|_| |[A-Z])").map(_.toLowerCase.capitalize).mkString.capitalize
//  }

  private def toPascalCase(str: String): String = {
    val sb               = new StringBuilder()
    var shouldCapitalize = true
    var wasCapitalized   = false
    str.foreach {
      case '-' | '_' | ' ' =>
        wasCapitalized = false
        shouldCapitalize = true
      case char if wasCapitalized =>
        wasCapitalized = char.isUpper
        shouldCapitalize = false
        sb.append(char.toLower)
      case char if shouldCapitalize =>
        wasCapitalized = char.isUpper
        shouldCapitalize = false
        sb.append(char.toUpper)
      case char if char.isUpper =>
        wasCapitalized = char.isUpper
        shouldCapitalize = false
        sb.append(char)
      case char if char.isUpper =>
        wasCapitalized = true
        shouldCapitalize = false
        sb.append(char.isLower)
      case char =>
        wasCapitalized = false
        shouldCapitalize = false
        sb.append(char)
    }

    sb.result()
  }

  private def toCamelCase(str: String): String = {
    val pascalCase = toPascalCase(str)
    pascalCase.take(1).toLowerCase ++ pascalCase.drop(1)
  }
}
