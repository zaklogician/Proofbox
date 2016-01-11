package proofbox

import java.lang.reflect._

import language.experimental.macros
import reflect.macros.whitebox.Context
import scala.language.existentials

trait Module { }

object Proofbox {
	
    def get[Target, From]: Target = macro getImpl[Target, From]
    def getImpl[Target: c.WeakTypeTag, From: c.WeakTypeTag](c: Context): c.Tree = {
      import c.universe._
      
      // 1. Find the member functions of the given trait via the compiler
      val members: List[c.universe.MethodSymbol] = 
        weakTypeTag[From].tpe.members.
        filter(m => !ignore.contains(m.name.toString) ).
        map(_.asMethod).toList
      
      
      // 2. Create the initial sequent for the proof search
      //    (we need two casts because the type checker is not smart enough)
      object PS extends ProofSearchInContext(c)
      val lhs = members.map { m => PS.Formula(m.asInstanceOf[PS.c.universe.MethodSymbol],Nil) }
      val rhs = weakTypeTag[Target].tpe.asInstanceOf[PS.c.universe.Type]
      val sequent = PS.Sequent(Nil,lhs,rhs)
      
      // 3. Find a proof of the sequent using proof search
      val result = sequent.proofSearch
      
      // 4. Emit code based on result proof search
      result match {
		
		// sequent false, instantiation not possible, signal error
		case Failure() => { 
		  c.abort( c.enclosingPosition, 
		  "Cannot instantiate " + weakTypeTag[Target].tpe + " in module " + weakTypeTag[From].tpe )
		}
		
		// sequent true, emit code for object construction
		case Success(solution, more) => c.parse {
		  val qualifier: String = c.freshName("proofbox$")
		  "val "+qualifier+" = new "+weakTypeTag[From].tpe+"{};"+
		  solution.toQualifiedString(qualifier)
		}
		
      }
      
      // 5. Profit
	}
	
	/** Ignore these built-in member functions */
	val ignore: List[String] = List(
      "$init$",
      "hashCode",
      "toString",
      "equals",
      "$asInstanceOf",
      "$isInstanceOf",
      "synchronized",
      "$hash$hash",
      "$bang$eq",
      "$eq$eq",
      "ne",
      "eq",
      "finalize",
      "wait",
      "notifyAll",
      "notify",
      "clone",
      "getClass",
      "asInstanceOf",
      "isInstanceOf",
      "equals"
	)

}

protected[proofbox] case class ProofSearchInContext(c: Context) {
  
    case class Sequent( past: List[Formula], future: List[Formula], target: c.universe.Type) {
	  
	/** Searches for a proof of the given sequent in the LJT calculus, 
	  * returning an evaluated result and the possibility to ask for more results.
	  *  
	  * Performs the proof search in the (left implicational) fragment of LJT,
	  * generating method calls directly, without explicitly storing the proof tree.
	  * Fails if the sequent has no LJT proof.
	  */
	def proofSearch: Backtrack[Formula] = {
	  axiom                                  orElse 
	  implicationLeft.flatMap(_.proofSearch) orElse
	  exchange.flatMap(_.proofSearch)
	}
	  
	/** Performs the exchange rule if possible, fails otherwise. */
	def exchange: Backtrack[Sequent] = future match {
	  case Nil => Failure()
	  case (currentFormula :: future) => 
	    Sequent( past :+ currentFormula, future, target ).fullExchange
	}
	  
	/** Performs the axiom rule if possible, fails otherwise. */
	def axiom: Backtrack[Formula] = future match {
	  case Nil => Failure()
	  case (formula :: future) => 
	    if( formula.isConstant && (target <:< formula.returnType) )
	    Backtrack.unit(formula) else Failure()
	}
	  
	/** Performs the implication left rule if possible, fails otherwise. */
	def implicationLeft: Backtrack[Sequent] = future match {
	  case Nil => Failure()
	  case (formula :: future) => 
	      if( formula.isConstant ) Failure() else {
	        val constants = formulas.filter(_.isConstant)
		    val arguments = product {
		    formula.argumentTypes.map( a => constants.filter(c=> a <:< c.returnType) )
		  }
	      Backtrack fromList arguments.map(assignArguments(_))
	    }
	}
	  
	/** List of all the formulas on the left hand side of the sequent. */
	val formulas: List[Formula] = past ++ future
	  
	//--------------------------------------------------------------------------------------------//
	
	/** Calculates the Cartesian product of the given list of lists.
	  * E.g. product [[1,2],[3]] == [[1,3],[2,3]]
	  */
	private def product[T](xs: List[List[T]]): List[List[T]] = xs match {
	  case Nil       => List(Nil)
	  case (x :: xs) => for(xp <- x; xsp <- product(xs)) yield (xp :: xsp)
	}

	/** Performs a full exchange rule: 
	  * returns all possible formulas on which the next rule can be applied.
	  */
	private def fullExchange: Backtrack[Sequent] = future match {
	  case Nil                         => Backtrack.unit(this)
	  case (currentFormula :: future)  => 
	    Backtrack.unit(this) interleave 
	    Sequent( past :+ currentFormula, future, target ).fullExchange
	}
	  
	/** Applies the current formula to the given list of arguments. */
	private def assignArguments(args: List[Formula]): Sequent = future match {
	  case Nil => Sequent(Nil,past,target)
	  case (formula :: future) => 
	    Sequent(Nil,past ++ (formula.withArguments(args):: future), target)
	}
  
  }
  
  
  case class Formula(method: c.universe.MethodSymbol, arguments: List[Formula]) {

	val returnType: c.universe.Type = method.returnType
	
	val argumentTypes: List[c.universe.Type] = method.paramLists match {
	  case Nil       => Nil
	  case (m :: ms) => m.map(_.typeSignature)
    }
    
    val isConstant: Boolean = arguments.length == argumentTypes.length
    
    def withArguments(arguments: List[Formula]) = Formula(method, arguments)
    
    def toQualifiedString(moduleName: String): String = {
	  if(arguments.length == 0) (moduleName+"."+method.name) else {
		val args = arguments.map(_.toQualifiedString(moduleName))
		moduleName+"."+method.name+"("+(args mkString ", ")+")"
	  }
	}
	
  }
  
}
