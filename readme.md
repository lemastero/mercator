<a href="https://furore.dev/propensive/mercator"><img src="/doc/images/furore.png" style="vertical-align:middle" valign="middle"></a>&nbsp;&nbsp;<a href="https://furore.dev/propensive/mercator">__Develop with Fury__ </a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://riot.im/app/#/room/#propensive.mercator:matrix.org"><img src="/doc/images/riotim.png" style="vertical-arign:middle" valign="middle"></a>&nbsp;&nbsp;<a href="https://riot.im/app/#/room/#propensive.mercator:matrix.org">__Discuss on Riot__</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://search.maven.org/search?q=g:com.propensive%20AND%20a:mercator_2.12"><img src="/doc/images/mavencentral.png" style="vertical-arign:middle" valign="middle"></a>&nbsp;&nbsp;<a href="https://search.maven.org/search?q=g:com.propensive%20AND%20a:mercator_2.12">__Download from Maven Central__</a>

<img src="/doc/images/github.png" valign="middle">

# Mercator

Mercator is a macro for automatically constructing evidence that a known type may be used in a for-comprehension, for abstracting over monad-like types with no impact on performance.  This allows us to write code against generic type constructors which can assume that they are usable in a for-comprehension, without having the evidence that they are until the application point when the type constructor is known, at which point, Mercator will construct it.

## Features

- constructs `Monadic` instances for types with the appropriate `flatMap`, `map` and &ldquo;point&rdquo; methods
- allows abstraction over for-comprehensions
- can generate `Functor` instances for types without `flatMap`
- support for filterable monadic types


## Getting Started

It is not possible to write code such as,
```
// does not compile
def increment[F[_]](xs: F[Int]) = for(x <- xs) yield x + 1
```
because the compiler is not able to enforce the constraint that the type
constructor `F[_]` provides the methods `map` and `flatMap` (with the correct
signatures) which are necessary for the for-comprehension to compile.

With Mercator, it is possible to demand an implicit instance of `Monadic[F]` to
enforce this constraint. Mercator will automatically instantiate such an
instance at the use-site for any type which has the required methods, like so,
```
import mercator._
def increment[F[_]: Monadic](xs: F[Int]) = for(x <- xs) yield x + 1
```

The methods `flatMap` and `map` will be provided to the instance of `F[_]` as
extension methods, using an implicit value class in the `mercator` package.
This incurs no allocations at runtime, and the performance overhead should be
zero or negligible.

## Point

An instance of `Monadic[F]` will generate an implementation of `point`, which
constructs a new instance of the type from a single value. This implementation
assumes the existence of an `apply` method on the type's companion object, and
that applying the value to it will produce a result of the correct type.

If this is not the case, Mercator will try to find a unique subtype of `F[_]`
whose companion object has an apply method taking a single value and returning
the correct type. In the case of `Either` or Scalaz's `\/`, this will do the
right thing.


## Availability

Mercator&rsquo;s source is available on GitHub, and may be built with [Fury](https://github.com/propensive/fury) by
cloning the layer `propensive/mercator`.
```
fury layer clone -i propensive/mercator
```
or imported into an existing layer with,
```
fury layer import -i propensive/mercator
```
A binary will be made available on Maven Central.

## Contributing

Contributors to Mercator are welcome and encouraged. New contributors may like to look for issues marked
<a href="https://github.com/propensive/mercator/labels/good%20first%20issue"><img alt="label: good first issue"
src="https://img.shields.io/badge/-good%20first%20issue-67b6d0.svg" valign="middle"></a>.

We suggest that all contributors read the [Contributing Guide](/contributing.md) to make the process of
contributing to Mercator easier.

Please __do not__ contact project maintainers privately with questions, as other users cannot then benefit from
the answers.

## Author

Mercator was designed and developed by [Jon Pretty](https://twitter.com/propensive), and commercial support and
training is available from [Propensive O&Uuml;](https://propensive.com/).



## License

Mercator is copyright &copy; 2018-20 Jon Pretty & Propensive O&Uuml;, and is made available under the
[Apache 2.0 License](/license.md).
