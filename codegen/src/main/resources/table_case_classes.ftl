package <#if schema.scalaName?has_content>kicks.db.${schema.scalaName}<#else>kicks.db</#if>

import dbdata.core.DatabaseObject

<#list schema.tables as table>
<@row_case_class row=table.rowBase indent=0 />
object ${table.rowBase.scalaName} extends <#if table.view>DatabaseObject.View<#else>DatabaseObject.Table</#if> {
  <#list table.rowVariations as row>
  <@row_case_class row=row indent=1 />
  object ${row.scalaName} extends <#if table.view>DatabaseObject.View<#else>DatabaseObject.Table</#if> {
    val tableName = "${table.name?j_string}"
    val columnNames = Vector[String](
      <#list row.columns as column>"${column.name?j_string}"<#if column?has_next>, </#if></#list>
    )
  }
  </#list>

  val tableName = "${table.name?j_string}"
  val columnNames = Vector[String](
    <#list table.rowBase.columns as column>"${column.name?j_string}"<#if column?has_next>, </#if></#list>
  )
}
</#list>

<#list schema.enums as enum>
sealed abstract class ${enum.scalaName}(val value: String)
object ${enum.scalaName} {
<#list enum.values as enumValue>
  case object ${enumValue.scalaName} extends ${enum.scalaName}("${enumValue.value?j_string}")
</#list>
  val values = Set[${enum.scalaName}](
    <#list enum.values as enumValue>${enumValue.scalaName}<#if enumValue?has_next>, </#if></#list>
  )

  def byName(searchValue: String): Option[${enum.scalaName}] = values.find(_.value == searchValue)
}

</#list>

<#macro row_case_class row indent>
<#local padding = ""?left_pad(2*indent)/>
${padding}case class ${row.scalaName}(
<#list row.columns as column>
${padding}  ${column.scalaName}: ${column.scalaType},
</#list>
${padding})
</#macro>
