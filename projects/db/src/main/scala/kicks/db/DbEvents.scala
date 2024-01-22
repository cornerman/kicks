package kicks.db

import org.sqlite.jni.capi.CApi.{sqlite3_commit_hook, sqlite3_open, sqlite3_update_hook}
import org.sqlite.jni.capi.{CommitHookCallback, UpdateHookCallback, sqlite3}

object DbEvents {
  def setup(jdbcUrl: String): Unit = {
    println("db events")
    println(jdbcUrl.stripPrefix("jdbc:sqlite:"))
    val sqlite = sqlite3_open("/home/cornerman/projects/kicks/projects/httpServer/kicks.db")
    println("sqlite " + sqlite)
    val commitHook = new CommitHookCallback {
      override def call(): Int = {
        println("\nCOMMIT")
        0
      }
    }
    val updateHook = new UpdateHookCallback {
      override def call(i: Int, s: String, s1: String, l: Long): Unit = {
        println(s"\nEVENT\ni=${i}, s=${s}, s1=${s1}, l=${l}")
      }
    }
    sqlite3_commit_hook(sqlite, commitHook): Unit
    sqlite3_update_hook(sqlite, updateHook): Unit
  }
}
