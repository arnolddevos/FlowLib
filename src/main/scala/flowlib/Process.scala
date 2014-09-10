package flowlib

sealed trait Process[+U] extends Process.ProcessOps[U]

object Process {

  // a constant process
  case class Complete[U](u: U) extends Process[U]

  // one process after another
  case class Sequential[V, U]( process: Process[V], step: V => Process[U]) extends Process[U]

  // a trampoline
  case class Ready[U]( step: () => Process[U] ) extends Process[U]

  // failed state triggers error handling
  case class Failed(e: Throwable) extends Process[Nothing]

  // states for concurrent processes
  case class Waiting[U]( respond: (U => Unit) => Unit) extends Process[U]
  case class Asynchronous[U]( step: () => Process[U] ) extends Process[U]
  case class WaitingAsync[U]( respond: (U => Unit) => Unit) extends Process[U]
  case class Parallel( p1: Process[Any]) extends Process[Unit]

  // naming  processes
  case class Named[U](name: String, step: Process[U]) extends Process[U] {
    override def toString = s"Process($name)"
  }

  def process[U]( step: => Process[U]): Process[U] = Asynchronous(() => step)

  def waitFor[T]( respond: (T => Unit) => Unit): Process[T] = WaitingAsync(respond)

  def continue[U]( step: => Process[U]): Process[U] = Ready(() => step)

  def stop[U](u: U): Process[U] = Complete(u)

  def fail(message: String, cause: Throwable=null): Process[Nothing] =
    Failed(new RuntimeException(message, cause))

  trait ProcessOps[+U] { p0: Process[U] =>
    def map[V]( f: U => V ): Process[V] = flatMap(u => Complete(f(u)))
    def flatMap[V]( step: U => Process[V]): Process[V] = Sequential(p0, step)
    def >>=[V]( step: U => Process[V]): Process[V] = Sequential(p0, step)
    def >>[V]( step: => Process[V]): Process[V] = Sequential(p0, (_:U) => step)
    def &[V](step: => Process[V]): Process[V] = Sequential(Parallel(p0), (_: Any) => step)
    def !:(name: String): Process[U] = Named(name, p0)
    def run() = (new DefaultSite {}) run p0
  }
}
