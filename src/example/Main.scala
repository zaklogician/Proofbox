package example

import proofbox._;

object Main {
  
  case class DatabaseAccess(url: String)
  case class SecurityFilter()
  case class UserFinder(databaseAccess: DatabaseAccess, securityFilter: SecurityFilter)
  case class UserStatusReader(userFinder: UserFinder)
  
  trait DevelModule extends proofbox.Module {
    def databaseAccess: DatabaseAccess = new DatabaseAccess("testDB")
    def securityFilter: SecurityFilter = new SecurityFilter()
    def userFinder(d: DatabaseAccess, s: SecurityFilter) = new UserFinder(d,s)
    def userStatusReader(u: UserFinder) = new UserStatusReader(u)
  }
  
  trait ReleaseModule extends proofbox.Module {
    def databaseAccess(url: String): DatabaseAccess = new DatabaseAccess(url)
    def securityFilter: SecurityFilter = new SecurityFilter()
    def userFinder(a: DatabaseAccess, s: SecurityFilter) = new UserFinder(a,s)
    def userStatusReader(u: UserFinder) = new UserStatusReader(u)
    def getReleaseDB: String = "releaseDB"
  }
  
  def main(args: Array[String]): Unit = {
    val a = Proofbox.get[DatabaseAccess,ReleaseModule]
    val b = Proofbox.get[SecurityFilter,ReleaseModule]
    val c = Proofbox.get[UserFinder,ReleaseModule]
    val d = Proofbox.get[UserStatusReader,ReleaseModule]
    println(a)
    println(b)
    println(c)
    println(d)
    // Proofbox.get[Int,ReleaseModule] // compile error
  }
  
}
