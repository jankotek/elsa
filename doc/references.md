Reference Tracking
======================

Elsa needs to handle backward and circular references. 
Lets take following object graph:

<!---#file#doc/ref_1.java--->
```java
Long a = new Long(100);
// create object graph with duplicate references
Object[] array1 = new Object[]{a, a};

//object graph with circular reference, array2 references itself
Object[] array2 = new Object[]{a, null};
array2[1] = array2;
```
In first case we have 'duplicate reference'. When Elsa traverses object graph it will visit single object twice.

Second case is 'circular reference'. An array element points to array itself. 
Naive object graph traversal would end in infinite loop and eventually with `StackOverflowException`.

To prevent both cases Elsa implements Reference Tracking protection.
While it traverses object graph, it puts visited objects into  `IdentityHashMap<Object, Index>`. 
When it visits new object, it first checks if object exists in Map of already visited objects. 
If yes, it will not serialize object, but will write reference to previous instance of this object. 


Plugable Reference Tracking
---------------------------

Reference Tracking has multiple implementations. By default `IdentityHashMap` is used. 
Here is an example howto change reference tracking implementation.

There are following alternative implementations:

<!---#file#doc/ref_2.java--->
```java
ElsaSerializer serializevr =
        new ElsaMaker()
        .referenceHashMapEnable()
        .make();
```

### Reference Tracking Disabled
Reference Trackign is disabled with `ElsaMaker.referenceDisable()` option. 
Elsa will not track visited objects. 
This will speedup serialization of  object graphs and serialization will use less memory.
However duplicate objects will be serialized multiple times.
Circular references will send serializer into infinitive loop, 
and serialization will fail with `StackOverflowException`. 
 
### Array based Reference Tracking 
Is activated with `ElsaMaker.referenceArrayEnable()` option. 
In this case Elsa will not use `IdentityHashMap` but `IdentityArrayList` type of collection.

So rather than using hash table to lookup objects (with hash `System.identityHashCode()`), 
it will use an array with linear scan (traverse all entries)

In this case the lookup performance degrades exponentially with size of object graph.
Some time could be saved, array is faster for inserts than hash table.
 
I would not recommend this, except very small object graph (2 items).
 
### Deduplication Reference Tracking

By default Elsa uses `IdentityHashMap` with identity equality check (`==`) and identity hash code (`System.identityHashCode()`). 
If we use normal `HashMap` for reference tracking, Elsa will perform deduplication.

Hash code is calculated for all visited objects. 
If two objects in object graph are equal with `Object.equals()`, 
only first instance of object will be serialized. 

After deserialization second (and third..) instance will be replaced by first instance. 
This way Elsa saves memory and reduces space usage, if your object graph has many 
different instances of the same object. 

Deduplication has some overhead on serialization (calculate hash code for all objects).
It has zero overhead on deserialization. 