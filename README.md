<img src="https://raw.githubusercontent.com/jankotek/elsa/master/misc/logo.png" width=90 height=80 align="left"/>

Elsa Java Serialization
=======================

[![Build Status](https://travis-ci.org/jankotek/elsa.svg?branch=master)](https://travis-ci.org/jankotek/elsa)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.mapdb/elsa/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.mapdb%22%20AND%20a%3Aelsa)
[![Join the chat at https://gitter.im/jankotek/mapdb](https://badges.gitter.im/jankotek/mapdb.svg)](https://gitter.im/jankotek/mapdb?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


Java serialization alternative. 
It is faster and more space efficient alternative to `ObjectInputStream` and `ObjectOutputStream`.
Elsa tries to be compatible with Java Serialization. 
It supports `Externalizable` and `Serializable` interfaces, `writeReplace` methods and so on.

Elsa was originally part of [MapDB jar](http://www.mapdb.org), 
but was moved into separate library.

Hello world
------------

Maven snippet, VERSION is [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.mapdb/elsa/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.mapdb%22%20AND%20a%3Aelsa)

    <dependency>
        <groupId>org.mapdb</groupId>
        <artifactId>mapdb</artifactId>
        <version>VERSION</version>
    </dependency>

Code examples are on [github](https://github.com/jankotek/elsa/tree/master/src/test/java/examples)

Registered classes
------------------
Java Serialization stores class structure (class and field names) together with serialized data.
Elsa can externalize the class structure and make data significantly smaller. 
For that Elsa needs to examine classes before data are serialized. 

Singletons
----------
Some special instances can be treated as singletons. Those are registered with Elsa before serialization. 
Singletons are serialized using only two bytes.
Singletons are deserialized using original instance, so reference equality `==` is preserved after deserialization.


Support
------------

Elsa Serialization is part of MapDB project, so support is provided via our mailing list and Gitter chat etc.. 
We also offer professional support and consulting. 
More [details](http://www.mapdb.org/support/).

