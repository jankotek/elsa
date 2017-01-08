POJO Serialization
===================

In POJO (Plain Old Java Object) Serialization the object structure is broken down to fields and content 
of the fields is recursively serialized as subelements. 
Elsa uses this method if it does not know type of object (well known class or singleton). 
This method is usually the slowest and least space efficient, but Elsa has some tricks to improve that. 

This type of serialization is used in other frameworks. 
In Kryo it is called [Field Serializer](https://github.com/EsotericSoftware/kryo/blob/master/src/com/esotericsoftware/kryo/serializers/FieldSerializer.java).
Java Serialization operates in this mode by default. 

Class Catalog
-----------------

Classes and fields have names and other meta-info, 
 which consume extra space.
Class names are used to instantiate correct classes on deserialization.
Field names are used to correctly restore object references on deserialization.
This information also has to be included  into  every 
serialized object graph. 

Elsa optimizes space usage by using name dictionary.
Rather than including text info into every binary stream, it only writes small packed IDs. 
This dictionary is usually included at beginning of each binary stream. 

Dictionary (Catalog) can also be completely externalized from binary data and managed by user. 
In that case Elsa space serialization becomes very efficient for repeated information, such as rows in database.

Class registration
----------------------

Class Catalog is filled with relevant classes on the fly. 
If unknown class pops up at graph traversal, it is included 
into dictionary. 

It is better to prepopulate Class Catalog with relevant classes.
It will speedup serialization, because the Class Catalog 
does not have to be created on every serialization. 

There are more ways to prepopulate Class Catalog but easiest is 
to register relevant classes manually before serialization:

<!---#file#doc/pojo_register_class.java--->
```java
ElsaSerializer ser = new ElsaMaker()
        .registerClasses(SomePojo.class)
        .make();
```
Another option is to train Class Catalog from object graph. 
In this case Elsa traverses object graph, similar way as serialization does. 
But instead of serializing object graph into  binary data, it collect unknown classes  into Class Catalog. 

This training can be repeated multiple times with different types of objects. 
You can even use entire collection (such as Map) of objects.

**Note:** Order of classes in array might change between JVM restarts. You must make sure that classes are registered at the same order on deserialization.

<!---#file#doc/pojo_register_class2.java--->
```java
List somePojos = new ArrayList();
somePojos.add(new SomePojo());

// traverse Object Graph and return all unknown classes
Class[] classes = ElsaUtil.findUnknownClasses(somePojos);

// register unknown classes with serializer
ElsaSerializer ser = new ElsaMaker()
        .registerClasses(classes)
        .make();
```

Rename class
--------------
Over time source code gets refactored and classes renamed. 
Elsa can handle such case.

In serialized data class is identified by its number. 
Class Catalog is than used to resolve number to actual class.
Class name is stored on single place and can be changed.

###Registered Classes

Easiest case is when you registered your class with `ElsaMaker`  before serialization. 
In that case Class Catalog (and your class name) is not stored in binary data,
but calculated by `ElsaMaker`. 
If class name changes, the Class Catalog will be the same, but with different class name. 

So `ElsaMaker.registerClasses(class..)` handles renamed classes automatically.

###Not registered classes
 
Not implemented in Elsa 3.0, see Issue: https://github.com/jankotek/elsa/issues/6
 
Rename field
--------------

Not implemented in Elsa 3.0, see Issue: https://github.com/jankotek/elsa/issues/7

