/*
  
  Mercator, version 0.1.1. Copyright 2018 Jon Pretty, Propensive Ltd.

  The primary distribution site is: https://propensive.com/

  Licensed under the Apache License, Version 2.0 (the "License"); you may not use
  this file except in compliance with the License. You may obtain a copy of the
  License at
  
      http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations under
  the License.

*/
package mercator

import scala.annotation.compileTimeOnly
import scala.language.higherKinds
import scala.reflect.macros._

import language.experimental.macros

object `package` {
  implicit def monadic[F[_]]: Monadic[F] =
    macro Mercator.instantiate[F[Nothing]]
  
  final implicit class Ops[M[_], A](val value: M[A]) extends AnyVal {
    @inline def flatMap[B](fn: A => M[B])(implicit monadic: Monadic[M]): M[B] =
      monadic.flatMap[A, B](value)(fn)

    @inline def map[B](fn: A => B)(implicit monadic: Monadic[M]): M[B] =
      monadic.map[A, B](value)(fn)
    
    @inline def filter(fn: A => Boolean)(implicit monadic: MonadicFilter[M]): M[A] =
      monadic.filter[A](value)(fn)
  }

  final implicit class Mtl[M[_], N[_], A](val value: M[N[A]]) extends AnyVal {
    @inline def in: Inner[M, N, A] = new Inner[M, N, A](value)
  }
  
  final implicit class Mtl2[M[_], N[_], P[_], A](val value: M[N[P[A]]]) extends AnyVal {
    @inline def in2: Inner2[M, N, P, A] = new Inner2[M, N, P, A](value)
  }
  
  final implicit class Mtl3[M[_], N[_], P[_], Q[_], A](val value: M[N[P[Q[A]]]]) extends AnyVal {
    @inline def in3: Inner3[M, N, P, Q, A] = new Inner3[M, N, P, Q, A](value)
  }
}

class Inner[M[_], N[_], A](val value: M[N[A]]) extends AnyVal { inner =>
  @inline def flatMap[B](fn: A => N[B])(implicit monadicM: Monadic[M], monadicN: Monadic[N]): M[N[B]] =
    monadicM.map[N[A], N[B]](value)(monadicN.flatMap[A, B](_)(fn))

  @inline def map[B](fn: A => B)(implicit monadicM: Monadic[M], monadicN: Monadic[N]): M[N[B]] =
    monadicM.map[N[A], N[B]](value)(monadicN.map[A, B](_)(fn))
}

class Inner2[M[_], N[_], P[_], A](val value: M[N[P[A]]]) extends AnyVal { inner =>
  @inline def flatMap[B](fn: A => P[B])(implicit monadicM: Monadic[M], monadicN: Monadic[N], monadicP: Monadic[P]): M[N[P[B]]] =
    monadicM.map[N[P[A]], N[P[B]]](value) { value2 => monadicN.map[P[A], P[B]](value2)(monadicP.flatMap[A, B](_)(fn)) }

  @inline def map[B](fn: A => B)(implicit monadicM: Monadic[M], monadicN: Monadic[N], monadicP: Monadic[P]): M[N[P[B]]] =
    monadicM.map[N[P[A]], N[P[B]]](value) { value2 => monadicN.map[P[A], P[B]](value2)(monadicP.map[A, B](_)(fn)) }
}

class Inner3[M[_], N[_], P[_], Q[_], A](val value: M[N[P[Q[A]]]]) extends AnyVal { inner =>
  @inline def flatMap[B](fn: A => Q[B])(implicit monadicM: Monadic[M], monadicN: Monadic[N], monadicP: Monadic[P], monadicQ: Monadic[Q]): M[N[P[Q[B]]]] =
    monadicM.map[N[P[Q[A]]], N[P[Q[B]]]](value) { value2 =>
      monadicN.map[P[Q[A]], P[Q[B]]](value2) { value3 => monadicP.map[Q[A], Q[B]](value3)(monadicQ.flatMap[A, B](_)(fn)) }
    }

  @inline def map[B](fn: A => B)(implicit monadicM: Monadic[M], monadicN: Monadic[N], monadicP: Monadic[P], monadicQ: Monadic[Q]): M[N[P[Q[B]]]] =
    monadicM.map[N[P[Q[A]]], N[P[Q[B]]]](value) { value2 =>
      monadicN.map[P[Q[A]], P[Q[B]]](value2) { value3 => monadicP.map[Q[A], Q[B]](value3)(monadicQ.map[A, B](_)(fn)) }
    }
}

object Mercator {
  def instantiate[F: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._
    val typeConstructor = weakTypeOf[F].typeConstructor
    val mockType = appliedType(typeConstructor, typeOf[Mercator.type])
    val companion = typeConstructor.dealias.typeSymbol.companion
    val returnType = scala.util.Try(c.typecheck(q"$companion.apply(_root_.mercator.Mercator)").tpe)
    val pointApplication = if(returnType.map(_ <:< mockType).getOrElse(false)) q"${companion.asModule}(value)" else {
      val subtypes = typeConstructor.typeSymbol.asClass.knownDirectSubclasses.filter { sub =>
        c.typecheck(q"${sub.companion}.apply(_root_.mercator.Mercator)").tpe <:< mockType
      }.map { sub => q"${sub.companion}.apply(value)" }
      if(subtypes.size == 1) subtypes.head
      else c.abort(c.enclosingPosition, s"mercator: unable to derive Monadic instance for type constructor $typeConstructor")
    }

    val filterMethods: List[Tree] = if(mockType.typeSymbol.info.member(TermName("filter")) == NoSymbol) Nil
      else List(q"def filter[A](value: Monad[A])(fn: A => Boolean) = value.filter(fn)")

    val instantiation =
      if(filterMethods.isEmpty) tq"_root_.mercator.Monadic[$typeConstructor]"
      else tq"_root_.mercator.MonadicFilter[$typeConstructor]"

    q"""
      new $instantiation {
        def point[A](value: A): Monad[A] = $pointApplication
        def flatMap[A, B](from: Monad[A])(fn: A => Monad[B]): Monad[B] = from.flatMap(fn)
        def map[A, B](from: Monad[A])(fn: A => B): Monad[B] = from.map(fn)
        ..$filterMethods
      }
    """
  }
}

trait Monadic[F[_]] {
  type Monad[T] = F[T]
  def point[A](value: A): F[A]
  def flatMap[A, B](from: F[A])(fn: A => F[B]): F[B]
  def map[A, B](from: F[A])(fn: A => B): F[B]
}

trait MonadicFilter[F[_]] extends Monadic[F] {
  def filter[A](value: F[A])(fn: A => Boolean): F[A]
}
