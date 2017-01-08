Configuration options and Elsa Maker
=====================================

Elsa has many options, to make it easier to use there is ElsaMaker  *TODO javadoc link*.
It uses [builder pattern](https://en.wikipedia.org/wiki/Builder_pattern#Java) to set various options. 
 
Next chapters describe some configuration options deeper. This chapter is just quick overview of various options.

Class Loader
-------------

On deserialization Elsa knows only class name, and it needs to resolve and load class by its name. 
For that Java uses [ClassLoader](https://docs.oracle.com/javase/7/docs/api/java/lang/ClassLoader.html). 
Single running JVM might use more than one Class Loader. By default Elsa uses system Class Loader obtained by:

```java
Thread.currentThread().getContextClassLoader()
```

You can set your own Class Loader with `classLoader` option:
<!---#file#doc/option_class_loader.java--->
```java
ClassLoader myClassLoader =
        Thread.currentThread().getContextClassLoader();

ElsaSerializer serializer =
        new ElsaMaker()
        .classLoader(myClassLoader)
        .make();
```

Unknown class registration
-------------------------

In binary serialized data Elsa references unknown classes by their name. That creates space overhead, 
package and class names are repeatedly stored in your data. 
To avoid space overhead you can register your classes into dictionary. 
In that case Elsa will only use packed number to reference classes it knows.

It is also possible to register unknown class listener, and get notified when Elsa serializes unknown class.

*TODO link to pojo-serialization chapter*


User class serializers
-------------------------

Elsa allows you to register serializers for your own object types. 
So you can serialize your own data (such as `Person`) faster and more efficiently. 

User serializer will work for top level objects in object graph (`serialize(person)`) but also 
for objects nested deep inside object graph (`serialize(List<Person>)). 
In that case Elsa will call user serializer recursively, as it traverses object graph.

*TODO link to chapter*

User singletons
-------------

It is possible to register user singletons. 
So you can use references such as `ApplicationServer.INSTANCE` in your object graph. 
Singletons are not really serialized (only packed number is written) and after deserialization are preserved
so their reference equality is preserved (no second instance of singleton is created).

*TODO link to chapter*

Reference tracking
-------------------

Elsa handles object duplicates and backward references while it traverses object graph. So single instance of object is serialized only once. 
Even if its referenced more than once in object graph.
References are also correctly restored after deserialization. 

For that Elsa keeps already visited objects in `IdentityHashMap`. That brings some overhead on serialization.

It is possible to completely disable this for faster serialization. 
You can also different implementation. 
For example Elsa can perform deduplication, while it serializes data. 


*TODO link to chapter*
