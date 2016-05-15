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

Elsa was originally part of [MapDB jar](http://www.mapdb.org), 
but was moved into separate library.

Support
------------

Bug reports should go to Github Issue tracker.

For questions and other suggestions use MapDB support channels (chat, mailing list, subreddit).

We also offer professional support and consulting.
More [details](http://www.mapdb.org/support/).

Hello world
------------

Maven snippet, VERSION is [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.mapdb/elsa/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.mapdb%22%20AND%20a%3Aelsa)

```xml
    <dependency>
        <groupId>org.mapdb</groupId>
        <artifactId>mapdb</artifactId>
        <version>VERSION</version>
    </dependency>
```

Code examples are on [github](https://github.com/jankotek/elsa/tree/master/src/test/java/examples)

Class Catalog
------------------
Serialization format usually stores class structure information (field names, field order, data types) together with serialized data. Size of serialized data can be greatly reduced by externalizing class structure information. In example bellow it is 5 bytes versus 55 bytes.

Elsa can store class structure information outside of serialized data, in Class Catalog. It is an array of classes. In binary format the class itself, is just replaced by index in Class Catalog.

Class Catalog is pretty flexible, but here we will only cover basic cases.

Register classes
----------------------

Best way to create class catalog is to register classes in `ElsaMaker`.
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

In binary format the class is represended by its index in an array.
So its critical to **register classes at the same order every time**. Otherwise
you will be unable to deserialize data.

Unknown class callback
-------------------------

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

In binary format the singleton is represended by its index in an array.
So its critical to **register singletons at the same order every time**. Otherwise
you will be unable to deserialize data.

Cyclic reference handling
------------------------------

By default Elsa handles cyclic references. On second apparence of an object
in graph, only reference to previous instance is written.
On deserialization reference equality is preserved.
Elsa also handles cyclic references etc..

Tracking references has overhead. So it can be disabled with `ElsaMaker.referenceDisable()`.

Elsa has two ways to track duplicate references:
* Already serialized objects are placed into `IdentityHashMap<Object,Integer>`. To find duplicates we check the map content.
* Already serialized objects are placed into `Object[]`. To find duplicates we need to traverse entire array and do reference equality check `==`.
 This can be enabled with `ElsaMaker.referenceArrayEnable()`

`IdentityHashMap` works quite well for large graphs and is enabled by defualt.
Array works better well for small object graphs (upto 60 objects). But fails missarably on large object graphs.

TODO benchmarks

Custom serializers
-----------------------

It is possible to [register custom serializers](https://github.com/jankotek/elsa/blob/master/src/test/java/examples/Custom_Serializers.java). Those are applied even for objects inside graph (field values).

TODO better documentation

Java compatibility
--------------------

Elsa tries to be compatible with Java Serialization. We require all classes
to implement `Serializable`. We handle `Externalizable` interfaces correctly.
Elsa also provides hacked `java.io.ObjectInputStream` and
`java.io.ObjectOutputStream`. And finally it handles less known `writeReplace` methods and so on.

In some cases Elsa will fallback into using Java Serialization.
