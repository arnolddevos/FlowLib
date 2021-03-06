package flowlib

import scala.language.higherKinds
import ProcessUtil._

object Flow extends Flow

/**
 * A simplified process/gate wiring DSL which does not use Labels, take N.
 */
trait Flow {

  implicit class FlowNode[N]( node: N) {
    def ->:[S, M](input: S)(implicit connect: FlowIn[S, N, M]): M = connect(input, node)
    def :->[S, M](output: S)(implicit connect: FlowOut[S, N, M]): M = connect(output, node)
    def :<-[S, M](input: S)(implicit connect: FlowIn[S, N, M]): M = connect(input, node)
  }

  trait FlowIn[-S, -N, +M] extends ((S, N) => M)
  trait FlowOut[-S, -N, +M] extends ((S, N) => M)

  def flowIn[S, N, M](f: (S, N) => M) = new FlowIn[S, N, M] {
    def apply(s: S, n: N): M = f(s, n)
  }

  def flowOut[S, N, M](f: (S, N) => M) = new FlowOut[S, N, M] {
    def apply(s: S, n: N): M = f(s, n)
  }

  implicit def connectSink[A, M]: FlowOut[Sink[A], Sink[A] => M, M] = flowOut((s, n) => n(s))
  implicit def connectSource[A, M]: FlowIn[Source[A], Source[A] => M, M] = flowIn((s, n) => n(s))
  implicit def connectGateOut[A, M]: FlowOut[Gate[A, Any], Sink[A] => M, M] = flowOut((s, n) => n(sendTo(s)))
  implicit def connectGateIn[A, M]: FlowIn[Gate[Nothing, A], Source[A] => M, M] = flowIn((s, n) => n(takeFrom(s)))

  def tee[A]: Sink[A] => Sink[A] => Sink[A] = s1 => s2 => fanout(List(s1, s2))

  implicit def connectSinks[S, N, M](implicit c: FlowOut[S, N, M]): FlowOut[S, List[N], List[M]] =
    flowOut((s, ns) => ns map (n => c(s, n)))

  implicit def connectSources[S, N, M](implicit c: FlowIn[S, N, M]): FlowIn[S, List[N], List[M]] =
    flowIn((s, ns) => ns map (n => c(s, n)))
}
