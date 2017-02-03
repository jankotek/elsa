<img src="https://raw.githubusercontent.com/jankotek/elsa/master/misc/logo.png" width=90 height=80 align="left"/>

Elsa Java Serialization
=======================

[![Build Status](https://travis-ci.org/jankotek/elsa.svg?branch=master)](https://travis-ci.org/jankotek/elsa)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.mapdb/elsa/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.mapdb%22%20AND%20a%3Aelsa)
[![Join the chat at https://gitter.im/jankotek/mapdb](https://badges.gitter.im/jankotek/mapdb.svg)](https://gitter.im/jankotek/mapdb?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


Elsa is object graph serialization framework for Java.
It has good compatibility with Java Serialization,
but is faster and more space efficient.
Elsa is great for storing objects on disk, network transfer, deep cloning etc..

Elsa handles cyclic references and Java Serialization features such as `Externalizable` or
`writeReplace()`.

Elsa was originally part of [MapDB](http://www.mapdb.org) database engine, 
but was moved into separate library.

Documentation
-------------------

Manual is [hosted on gitbooks](https://jankotek.gitbooks.io/elsa/content/). 

*TODO once it is finished, make `readme.md` shorter.*

Install and use
------------------

Elsa is available in Maven repository. Jar files can be  [downloaded here](http://mvnrepository.com/artifact/org.mapdb/elsa), currently Elsa has no dependencies and requires Java6. Maven snipped is bellow, latest VERSION is [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.mapdb/elsa/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.mapdb%22%20AND%20a%3Aelsa)

```xml
<dependency>
    <groupId>org.mapdb</groupId>
    <artifactId>elsa</artifactId>
    <version>VERSION</version>
</dependency>
```

Code examples are on [github](https://github.com/jankotek/elsa/tree/master/src/test/java/examples).

Hello world
------------

Here is simple [Hello World example](https://github.com/jankotek/elsa/blob/master/src/test/java/examples/Hello_World.java):


<!---#file#doc/intro_hello_world.java--->
```java
// import org.mapdb.elsa.*;

// data to be serialized
String data = "Hello World";

// Construct Elsa Serializer
// Elsa uses Maker Pattern to configure extra features
ElsaSerializer serializer = new ElsaMaker().make();

// Elsa Serializer takes DataOutput and DataInput.
// Use streams to create it.
ByteArrayOutputStream out = new ByteArrayOutputStream();
DataOutputStream out2 = new DataOutputStream(out);

// write data into OutputStream
serializer.serialize(out2, data);

// Construct DataInput
DataInputStream in = new DataInputStream(
        new ByteArrayInputStream(out.toByteArray()));

// now deserialize data using DataInput
String data2 = serializer.deserialize(in);
```

Support
------------

Bug reports go to [Issue tracker](https://github.com/jankotek/elsa/issues).

For questions and suggestions use [MapDB support channels](http://www.mapdb.org/support/) (chat, mailing list, subreddit). We also provide professional [support and consulting](http://kotek.net/consulting/).

Documentation is provided in form of
[examples](https://github.com/jankotek/elsa/blob/master/src/test/java/examples/References.java).
TODO javadoc on web.

Serializers
-----------------------

To speedup serialization Elsa comes with serializers for well known `java.lang` and `java.util` classes.
Serializers are recursive and will continue graph traversal,
for example `Map` serializer will continue graph traversal over keys and values.

Users can also install their own serializers.

For objects with no serializer Elsa will use slower field traversal to dive into Object Graph.

### Default serializers
By default Elsa has serializers for following classes:

- All primitive types and their arrays: `double`, `long`, `int`, `byte[]`...

- All primitive wrappers: `Double`, `Long`, `Integer`...

- Generic array `Object[]`

- Collections: `ArrayList`, `LinkedList`, `HashSet`, `LinkedHashSet` and `TreeSet`

- Maps: `HashMap`, `LinkedHashMap`, `TreeMap` and `Properties`

- `BigDecimal`, `BigInteger`, `UUID` and `Date`

- `java.lang.Class`


### Custom serializers

It is possible to [register custom serializers](https://github.com/jankotek/elsa/blob/master/src/test/java/examples/Custom_Serializers.java).
Those are part of graph traversal, and are applied on objects inside graph (collections entries and field values).

TODO better documentation for custom serializers

References
--------------------
Consider following example:

```java
List list = ArrayList();
list.add(list);
Object a = "some huge object";
list.add(a);
list.add(a);
```
That is Cyclic Reference and could send graph traversal into infinitive loop.
Object `a` is in graph twice and could cause space overhead if serialized twice.
To prevent that Elsa on serialization tracks already visited objects in `IdentityHashMap`.
Secondary visit will only write number as reference.
On deserialization references are restored and identity is preserved.

Reference tracking also works for user defined serializers, and for
collection serializers.

Maintaining `IdentityHashMap` has some overhead.
So there is an option to disable this feature completely. Use `ElsaMaker.referenceDisable()` to disable reference tracking

Or `IdentityHashMap` can be replaced with simple `Object[]` where for-loop with identity `==` check on each item.
That is faster on very small graphs with only a few items. Use `ElsaMaker.referenceArrayEnable()`
to enable identity array checks.

Finally there is an option to *deduplicate* references by replacing `IdentityHashMap`
with regular `HashMap`. In this case two equal objects which are not identical,
will become identical after deserialization. This adds some overhead on serialization for hashing
and equality check, but has no overhead on deserialization.
Use `ElsaMaker.referenceHashMapEnable()` to enable it.

There is a [reference handling example](https://github.com/jankotek/elsa/blob/master/src/test/java/examples/References.java)
with all configuration options.

Java Serialization compatibility
---------------------------------

Elsa tries to be compatible with Java Serialization. We require all classes
to implement `Serializable`. We handle `Externalizable` interfaces correctly.
Elsa also provides hacked `java.io.ObjectInputStream` and
`java.io.ObjectOutputStream`. And finally it handles less known `writeReplace` methods and so on.

In some cases Elsa will fallback into using Java Serialization.

Alternatives
--------------

TODO

Deep Cloning
----------------

Use `serializer.clone(object)`.

TODO

Class Catalog
------------------
Serialization format usually stores class structure
metadata (field names, field order, data types) together
with serialized data. Size of serialized data can be greatly
reduced by externalizing class structure information. In
example bellow it is 5 bytes versus 55 bytes.

Elsa can store class structure information outside of
serialized data. There are more ways. MapDB p Class
Catalog to handle class format versions, renamed fields and so on.

Simpler and more accessible way assumes that class format
never changes. That serialization and deserialization
share classes with exactly the same structure
(no renamed fields etc). In that case we can use
simple class registration:


### Register classes

Simplest way to externalize class structure metadata
is to register classes in `ElsaMaker`.
Each registered class is parsed into structural information
and added into Class Catalog.

An example howto [register classes](https://github.com/jankotek/elsa/blob/master/src/test/java/examples/Class_Catalog.java),
here is shorter version:

```java
ElsaSerializer serializer = new ElsaMaker()
//this registers Bean class into class catalog
.registerClasses(Bean.class, Bean2.class)
.make();
```

In binary format the class is represented by its index in an array.
So its critical to **register classes at the same order every time**. Otherwise
you will be unable to deserialize data.

### Unknown class callback

Elsa has callback to notify user about classes not presented in Class Catalog.
This way you assemble list of all classes used in an object graph.

TODO provide an example. `ElsaMaker unknownClassNotification(ClassCallback callback)`

Singletons
----------
Some special instances can be treated as singletons.
Those do not have to be serializable, Elsa just uses instance supplied by user.
In example bellow we serialize `Thread.currentThread()` in binary format.

Elsa does not try to serialize singleton into binary form,
it just writes singleton ID.  On deserialization
it finds ID and uses Singleton instance.
Singletons have reference equality (`==`) preserved even after binary deserialization.

It is easy to [register singleton](https://github.com/jankotek/elsa/blob/master/src/test/java/examples/Singletons.java) in `ElsaMaker`. Here is shorter example:

```java
ElsaSerializer s = new ElsaMaker()
    // Current thread is singleton
    .singletons(Thread.currentThread())
    .make();
```

In binary format the singleton is represented by its index in an array.
So its critical to **register singletons at the same order every time**. Otherwise
you will be unable to deserialize data.
