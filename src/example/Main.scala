package example

import proofbox._;

object Main {
  
  // random classes with dependencies
  case class DatabaseAccess(url: String)
  case class SecurityFilter()
  case class UserFinder(databaseAccess: DatabaseAccess, securityFilter: SecurityFilter)
  case class UserStatusReader(userFinder: UserFinder)
  
  // in this module databaseAccess can't be invoked because there is no String provider
  trait Base extends proofbox.Module {
    def databaseAccess(url: String): DatabaseAccess = new DatabaseAccess(url)
    def securityFilter: SecurityFilter = new SecurityFilter()
    def userFinder(a: DatabaseAccess, s: SecurityFilter) = new UserFinder(a,s)
    def userStatusReader(u: UserFinder) = new UserStatusReader(u)
  }
  
  // this module has two DatabaseAccess providers, one of which can't be invoked
  trait Testing extends Base {
    def testDB: DatabaseAccess = new DatabaseAccess("testDB")
  }
  
  // this module can invoke the databaseAccess provider
  trait Release extends Base {
    def releaseDB: String = "releaseDB"
  }
  
  def main(args: Array[String]): Unit = {
    //val aBase = Proofbox.get[DatabaseAccess,Base] // can't instantiate, compile error
    val aTesting = Proofbox.get[DatabaseAccess,Testing]
    val aRelease = Proofbox.get[DatabaseAccess,Release]
    println("DatabaseAccess")
    println("  Testing: "+aTesting)
    println("  Release: "+aRelease)
    println()
    
    val bBase    = Proofbox.get[SecurityFilter,Base]
    val bTesting = Proofbox.get[SecurityFilter,Testing]
    val bRelease = Proofbox.get[SecurityFilter,Release]
    println("SecurityFilter")
    println("  Base:    "+bBase)
    println("  Testing: "+bTesting)
    println("  Release: "+bRelease)
    println()
    
    //val cBase  = Proofbox.get[UserFinder,Base]
    val cTesting = Proofbox.get[UserFinder,Testing]
    val cRelease = Proofbox.get[UserFinder,Release]
    println("UserFinder")
    println("  Testing: "+cTesting)
    println("  Release: "+cRelease)
    println()
    
    
    //val dBase  = Proofbox.get[UserStatusReader,Base] // can't instantiate, compile error
    val dTesting = Proofbox.get[UserStatusReader,Testing]
    val dRelease = Proofbox.get[UserStatusReader,Release]
    println("UserStatusReader")
    println("  Testing: "+dTesting)
    println("  Release: "+dRelease)
    println()
  }
  
}
