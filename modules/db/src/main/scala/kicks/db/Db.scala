package kicks.db

import cats.implicits.*
import doobie.ConnectionIO
import doobie.implicits.*
import io.getquill.*
import kicks.db.quill.schema.*

sealed trait WithId[EntityId, Entity] {
  def getId(entity: Entity): EntityId
  def setId(entity: Entity, entityId: EntityId): Entity
}
object WithId {
  def apply[EntityId, Entity](implicit withId: WithId[EntityId, Entity]) = withId
}

object Db {
  private val ctx = doobie.DoobieContext.SQLite(Literal)
  import ctx.*

  def fun(person: Foo): ConnectionIO[Unit] = {
    val queryRun: ConnectionIO[Unit]  = run(Foo.query).map(println(_))
    val insertRun: ConnectionIO[Unit] = run(Foo.query.insertValue(lift(person))).map(println(_))
    queryRun *> insertRun *> queryRun
  }

  class DbEntityRepository[EntityId, Entity: WithId[EntityId, *]](query: EntityQuery[Entity]) {
    inline def all: ConnectionIO[List[Entity]] = {
      run(query)
    }

    inline def insert(inline entity: Entity): ConnectionIO[EntityId] = {
      run(query.insertValue(lift(entity)).onConflictIgnore).as(???)
    }

    inline def update(entity: Entity): ConnectionIO[Unit] = {
      run(query.updateValue(entity)).void
    }

    inline def upsert(entity: Entity): ConnectionIO[Unit] = {
//      run(query.insertValue(entity).onConflictUpdate(_.id))
      ???
    }

    inline def delete(entityId: Entity): ConnectionIO[Unit] = {
      //      run(query.insertValue(entity).onConflictUpdate(_.id))
      ???
    }
  }

//  implicit val PersonWithId: WithId[Int, Person] = new WithId[Int, Person] {
//    override def getId(entity: Person): Int                   = entity.id
//    override def setId(entity: Person, entityId: Int): Person = entity.copy(id = entityId)
//  }
//  val repo = new DbEntityRepository[Int, Person](PersonDao.query)

//  repo.update(Person(-1, "heinz", None, 5))
//  repo.insert(lift(Person(-1, "heinz", None, 5)))
//  repo.all
}
