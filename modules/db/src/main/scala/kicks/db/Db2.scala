package kicks.db

import com.augustnagro.magnum.*
import kicks.db.magnum.schema.*
import kicks.db.magnum.schema.PersonRepo.findAll

import javax.sql.DataSource

object Db2 {
  def run(ds: DataSource): Unit = transact(ds) {

    val personCreator = Person.Creator(None, "hans", Some(29))
    val person        = PersonRepo.insertReturning(personCreator)
    println(s"Created person: $person")
//    PersonRepo.insert(personCreator)

    // val persons = PersonRepo.findAll
    val persons = PersonRepo.findByIndexPersonFoo(None)
    val spec    = Spec[Person].where(sql"foo is null")
    // val persons = PersonRepo.findAll(spec).headOption
    println(s"All persons: ${persons.mkString("\n", "\n", "\n")}")
  }
}
