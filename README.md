# Proofbox


Proofbox is an experiment in writing a Dependency Injection container for Scala using proof search.

The capabilities of current DI libraries are inadequate for functional languages with advanced type
systems. When you use Dependency Injection in e.g. Scala, you are forced to write your program in a 
dumbed-down dialect that the DI container can cope with.

Our goal is to create a container that uses logic programming / proof search to 

* auto-wire dependencies
* manage the lifetime of objects
* provide plug-in support with declarative configuration

Proofbox is the first step towards this objective: it demonstrates the thesis that
Dependency Injection works like a form of proof search, and that proof search can be used to 
*implement* a Dependency Injection container.

## Installation

Use the shell build script by invoking `./build` (it should work on POSIX systems).

## Usage

Run the example by executing `scala example.Main`.


## Development Ideas

* Instead of target type, allow Prolog queries 

This could express: *substitute any `x <: CardCharger` that has a `CanCharge[x,Visa]` instance*.

* replace module declarations with Linear Logic formulas (in some fragment)

This would allow us to handle
[DI lifetime issues](http://nblumhardt.com/2011/01/an-autofac-lifetime-primer/) provably optimally,
without programmer intervention.


## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request.
