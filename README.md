Here’s the content formatted as a `.md` file for your README:

```markdown
# Scopes Programming Language

Scopes is a simple, yet powerful programming language that emphasizes scope management and functional programming paradigms. It introduces concepts such as variable scoping, closures, and object-oriented programming with class inheritance.

## Features

- **Variable Scoping**: Supports both global and block-level scoping.
- **Closures**: Functions can return other functions, allowing for the creation of stateful functions.
- **Classes and Inheritance**: Implements object-oriented principles such as classes, methods, and inheritance.

## Code Examples

### 1. Variable Scoping

Scopes allows variables to be defined at different levels, with block-level scoping within curly braces `{}`.

```scopes
var a = "global a";
var b = "global b";
var c = "global c";

{
  var a = "outer a";
  var b = "outer b";
  {
    var a = "inner a";
    print a;  // inner a
    print b;  // outer b
    print c;  // global c
  }
  print a;  // outer a
  print b;  // outer b
  print c;  // global c
}

print a;  // global a
print b;  // global b
print c;  // global c
```

In this example, variables `a`, `b`, and `c` have different values depending on the scope in which they are accessed. The innermost block uses the most local version of `a`, while the outer blocks use their respective variables.

### 2. Closures

Scopes supports closures, where a function can return another function, preserving the state of its environment.

```scopes
fun makeCounter() {
  var i = 0;
  fun count() {
    i = i + 1;
    print i;
  }

  return count;
}

var counter = makeCounter();
counter(); // "1"
counter(); // "2"
```

In this example, the `makeCounter` function returns a closure (`count`), which maintains its own state (`i`), even when called multiple times.

### 3. Classes and Methods

Scopes allows for defining classes and methods. Classes can have methods, and they can be instantiated and used to manage state.

```scopes
class Cake {
  taste() {
    var adjective = "delicious";
    print "The " + this.flavor + " cake is " + adjective + "!";
  }
}

var cake = Cake();
cake.flavor = "German chocolate";
cake.taste();  // "The German chocolate cake is delicious!"
```

In this example, the `Cake` class has a `taste` method that prints a statement including the `flavor` property of the instance.

### 4. Inheritance

Scopes supports class inheritance, allowing you to extend existing classes and override their methods.

```scopes
class Doughnut {
  cook() {
    print "Fry until golden brown.";
  }
}

class BostonCream < Doughnut {
  cook() {
    super.cook();
    print "Pipe full of custard and coat with chocolate.";
  }
}

BostonCream().cook();
// Output:
// Fry until golden brown.
// Pipe full of custard and coat with chocolate.
```

Here, `BostonCream` is a subclass of `Doughnut` that overrides the `cook` method. It calls the superclass's method using `super.cook()` and adds its own behavior.

## Syntax

- **Variable Declaration**: `var <name> = <value>;`
- **Function Declaration**: `fun <name>() { <body> }`
- **Class Definition**: `class <ClassName> { <methods> }`
- **Method Invocation**: `<instance>.<methodName>();`
- **Inheritance**: `<Subclass> < ParentClass`
- **Printing to Console**: `print <value>;`
