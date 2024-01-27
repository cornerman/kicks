package kicks.rpc

case class EntityId(i: Long)
//trait EntityRepository[Entity] {
//  def insert(entity: Entity)
//}

sealed trait RpcCommand
//object RpcCommand {
//  case class Insert[Entity](entity: Entity) extends RpcCommand
//  case class Upsert[Entity](entity: Entity) extends RpcCommand
//  case class Update[Entity](entity: Entity) extends RpcCommand
//  case class Delete[Entity](entityId: EntityId) extends RpcCommand
//}

sealed trait RpcEvent
object RpcEvent {}
