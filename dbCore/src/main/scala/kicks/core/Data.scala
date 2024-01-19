package dbdata.core

sealed trait DatabaseObject {
  def tableName: String
  def columnNames: Vector[String]
}
object DatabaseObject {
  sealed trait Table extends DatabaseObject
  sealed trait View  extends DatabaseObject
}
