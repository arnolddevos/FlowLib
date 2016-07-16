package flowlib

import Process._
import ProcessUtil._

object Generators {

  import Series._

  type Generator[+A] = Process[Series[A]]

  implicit class GeneratorOps[A](g: Generator[A]) {
    def concat[B >: A](gb: Generator[B]): Generator[B] = Generator.concat(g, gb)
    def fold[S](z: S)(f: (S, A) => S): Process[S] = Generator.fold(g)(z)(f)
    def sink: Sink[A] => Process[Unit] = Generator.sink(g)
    def +:[B >: A](b: B): Generator[B] = Generator(b, g)
  }

  object Generator {
    def apply(): Generator[Nothing] = stop(Series())
    def apply[A](a: A): Generator[A] = stop(Series(a))
    def apply[A](a: A, g: Generator[A]): Generator[A] = stop(Series(a, g))
    def fromList[A](as: List[A]): Generator[A] = stop(Series.fromList(as))

    def map[A, B](g: Generator[A])(f: A => B): Generator[B] = {
      g map {
        case NonEmpty(a, g1) => NonEmpty(f(a), map(g1)(f))
        case Empty => Empty
      }
    }

    def bind[A, B](g: Generator[A])(f: A => Generator[B]): Generator[B] = {
      g >>= {
        case NonEmpty(a, g1) => concat(f(a), bind(g1)(f))
        case Empty => stop(Empty)
      }
    }

    def concat[A](ga: Generator[A], gb: Generator[A]): Generator[A] = {
      ga >>= {
        case NonEmpty(a, ga1) => stop(NonEmpty(a, concat(ga1, gb)))
        case Empty => gb
      }
    }

    def fold[A, S](g: Generator[A])(z: S)(f: (S, A) => S): Process[S] = {
      g >>= {
        case NonEmpty(a, g1) => fold(g1)(f(z, a))(f)
        case Empty => stop(z)
      }
    }

    def sink[A](g: Generator[A]): Sink[A] => Process[Unit] = {
      output =>
        def loop(g: Generator[A]): Process[Unit] = {
          g >>= {
            case NonEmpty(a, g1) => output(a) >> loop(g1)
            case Empty => stop(())
          }
        }
        loop(g)
    }
  }

  sealed trait Series[+A] {
    def concat[B >: A](s: Series[B]): Series[B] = this match {
      case NonEmpty(a, p) => NonEmpty(a, p map (_ concat s))
      case Empty => s
    }

    def map[B](f: A => B): Series[B] = this match {
      case NonEmpty(a, p) => NonEmpty(f(a), p map (_ map f))
      case Empty => Empty
    }

    def +:[B >: A](b: B): Series[B] = NonEmpty(b, stop(this))
  }

  object Series {

    case class NonEmpty[+A](head: A, tail: Generator[A]) extends Series[A]
    case object Empty extends Series[Nothing]

    def apply(): Series[Nothing] = Empty
    def apply[A](a: A): Series[A] = NonEmpty(a, stop(Empty))
    def apply[A](a: A, g: Generator[A]): Series[A] = NonEmpty(a, g)
    def fromList[A](as: List[A]): Series[A] = as match {
      case a :: as1 => NonEmpty(a, continue(stop(fromList(as1))))
      case Nil => Empty
    }
  }
}
