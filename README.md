# closable-compiler-plugin

This scala compiler plugin is meant for those who are still using system shutdown hook to close resources. Let's consider the following code:

```scala
  def main(args: Array[String]): Unit = {

    val publisher: Publisher = new Publisher("p1")
    val server = new Server("s1")

    server.start()
    publisher.pubslish("test") // case 1: exception can be thrown here

    sys.addShutdownHook {
      publisher.close() // case 2: exception can be thrown here
      server.stop()
    }
  }
```

It looks pretty normal, however there are few problems with the code above, let's consider each possible scenario:
1. `publisher.pubslish("test")` throws an exception, `sys.addShutdownHook` wont be executed and `server` wont be closed
2. `publisher.close()` throws an exception, `server.stop()` wont be executed

There is an approach to avoid those problems which is adding a shutdown hook after each variable definition, see below:

```scala
  def main(args: Array[String]): Unit = {

    val publisher = new Publisher("p1")
    sys.addShutdownHook {
      publisher.close()
    }

    val server = new Server("s1")
    sys.addShutdownHook {
      server.stop()
    }

    server.start()
    publisher.pubslish("test")

  }
```

Now all resources are guaranteed to be closed even if something went wrong.

Code readability can be slightly improved by having a helper function:

```scala
def acquire[T](res: T)(release: T => Unit): T = {
    sys.addShutdownHook {
      Try(release(res)) match {
        case Failure(err) => logger.error(err.getMessage, err)
        case _ => ()
      }
    }
    res
  }

```

and then the code will look as follows : `val publisher = acquire(new Publisher("p1"))(_.close())`
However, the alternative way exists which is using a custom scala compiler plugin that allows to transform source AST and this is where `closable-compiler-plugin` comes into play:

```scala
@closeOnShutdown
val publsiher = new Publisher("p1")
@closeOnShutdown
val server = new Server("s1")
```

the plugin will generate a shutdown hook for each annotated variable:

```scala
val pubslisher = {
      val tmp = new Publisher("p1")
      sys.addShutdownHook {
        try {
          tmp.close()
        } catch {
          case err: Throwable => err.printStackTrace()
        }
      }
      tmp
    }
```

It's possible to use a logger instead of `err.printStackTrace()`, all you need to do is to set `useLogger` to `true` in `@closeOnShutdown("close", true)` and AST will be transformed into:

```scala
val pubslisher = {
      val tmp = new Publisher("p1")
      sys.addShutdownHook {
        try {
          tmp.close()
        } catch {
          case err: Throwable => logger.error(err.getMessage, err)
        }
      }
      tmp
    }
```

Also you can set a logger variable name by using `closeOnShutdown#loggerName`, `logger` is the name by default.

## Usage

in you build.sbt

```scala
val closablePluginVersion = "x.y.z"

lazy val myProject = project.settings(
   autoCompilerPlugins := true,
   libraryDependencies += compilerPlugin("com.github.dmgcodevil" %% "closable-compiler-plugin" % closablePluginVersion)
   // or addCompilerPlugin("com.github.dmgcodevil" %% "closable-compiler-plugin" % closablePluginVersion)
)
```

see the `example` project in this repo for more details.

### Notes

Named args aren't supported in `@closeOnShutdown` so the following code will be interpreted by the plugin as if all args were set to default values: ("close", false, "logger"):

```scala
@closeOnShutdown(value= "close", useLogger = true)
```

instead you should use unmaned args:

```scala
@closeOnShutdown("close", true)
```

There are no technical limitations to support named args it should be feasible to implement, it's just a matter of extending the pattern matching to pattern match on `AssignOrNamedArg(lhs: Tree, rhs: Tree)`


_PS: Good luck and don't use system shutdown hooks to close resources_





