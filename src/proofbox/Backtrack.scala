package proofbox;

/** The backtracking monad */
sealed trait Backtrack[A] {
  def interleave( ys: => Backtrack[A] ): Backtrack[A]
  def flatMap[B]( f: A => Backtrack[B] ): Backtrack[B]
  def map[B]( f: A => B ): Backtrack[B] = flatMap(Backtrack unit f(_))
  def orElse( attempt: => Backtrack[A] ): Backtrack[A]
  def andThen( attempt: => Backtrack[A] ): Backtrack[A]
  val isEmpty: Boolean
}

case class Failure[A]() extends Backtrack[A] {
  override def interleave( ys: => Backtrack[A] ): Backtrack[A] = ys
  override def flatMap[B]( f: A => Backtrack[B] ): Backtrack[B] = Failure()
  override def orElse( attempt: => Backtrack[A] ): Backtrack[A] = attempt
  override def andThen( attempt: => Backtrack[A] ): Backtrack[A] = attempt
  override val isEmpty: Boolean = true
}

case class Success[A]( head: A, more: () => Backtrack[A] ) extends Backtrack[A] {
  override def interleave( ys: => Backtrack[A] ): Backtrack[A] = 
    Success( head, () => ys interleave more() )
  override def flatMap[B]( f: A => Backtrack[B] ): Backtrack[B] = 
    f(head) interleave ( (more()) flatMap f )
  override def orElse( attempt: => Backtrack[A] ): Backtrack[A] = this
  override def andThen( attempt: => Backtrack[A] ): Backtrack[A] = 
    Success( head, () => more() andThen attempt )
  override val isEmpty: Boolean = false
}

object Backtrack {
  def unit[A]( a: A ): Backtrack[A] = Success( a, () => Failure() )
  def fromList[A]( xs: List[A] ): Backtrack[A] = xs match {
    case Nil => Failure()
    case (x :: xs) => Success( x, () => fromList(xs) )
  }
  def oneOf[A](xs: A*): Backtrack[A] = fromList(xs.toList)
}
