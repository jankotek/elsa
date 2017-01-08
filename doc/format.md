Binary format and internals
==========================

This chapter provides details how Elsa serializers objects, 
how object graph is traversed and how binary format looks like. 

This is not complete overview, but broad explanation. 
More specific details can be found in [Known classes](known-classes.md) chapter.

Element
------------

Element in this context is single cell in object graph. An element is serialized 
into single entry (has single Header Byte).

Non-recursive objects are serialized into single element. For example String 
could be serialized into two elements: as `String` POJO and `char[]` value of 
one of its fields. But Elsa treats Strings as single `char[]` element,
it does not try to parse String fields and dive deeper into its object graph. 

Header Byte
----------------
Each serialized object starts with **Header Byte**; an value between 0-255 which identifies type of data after it. All possible values are defined in `ElsaSerializerBase.Header` interface. 

Elsa tries to minimize binary data size. Some common singletons such as `Boolean.TRUE`, `0`, `0L` or `""` have their own Header Byte and consume only single byte when serialized.

In most cases the Header Byte is followed by some binary data. For example `STRING_1` is followed by single packed char. Size of string is defined in Header Byte and does not have to be stored. There is also `STRING_2`, `STRING_3` and so on.  Not all header values are reserved for strings, so there is also `STRING` header followed by packed string size and its data.

Space saving with Header Byte goes quite far. It handles most common java classes such as `Integer`, `Long`, `BigDecimal`, primitive arrays, `BigDecimal`, `UUID`, HashMap`, etc..

Some values are used for Elsa internals: singletons, user serializers, class infos... To detect data corruption, header values  zero and 172 (java serialization) are illegal.

Singletons
--------------

Elsa maintains `IdentityHashMap<Object, Int>` which maps singletons into their singleton code. All elements found during object graph traversal are compared with this map. If singleton is found, it is not serialized, but instead of its code is written into binary data. 

On deserialization the singletons are restored from `Map<Int, Singleton>` which
contains instances of all known singletons. 

It is possible to register your own singletons. In this case you must provide ID and singleton instance. ID is written into binary data.

*TODO this is implemented, code example*

Elements already visited during graph traversal are treated similar way as singletons. 
They are tracked in similar map.
Backward reference is written into binary data, instead of serializing object second time.

Array Serialization
-----------------------
To demonstrate how Elsa serializes object graph, lets see how an object array is serialized. 

Simple case is an `{new Long(1), new Long(12)}`. 
- Binary data starts with Header Byte `ARRAY_OBJECT`. *TODO array no refs?*
- Followed by packed array size, this consumes single byte (array size 128> would take 2 bytes, 16>  3 bytes etc) 
- `Long(1)` is known singleton so single Header Byte `LONG_1` is used
- `Long(12)` is known class, but no singleton. Header Byte `LONG_F1` is used to indicate positive long which fits into single byte. 
- Followed by single byte with value `12` from previous long

Lets consider cyclic reference an array which contains itself:
```java
Object[] array = new Object[1];
array[0] = array;  new
```
In this case binary data starts with header `ARRAY_OBJECT` followed by packed array size.
On first element Elsa detects cyclic reference. Serializing array again would cause infinitive loop (cyclic reference), instead 
it writes  header `OBJECT_STACK` followed by packed value `0`. That is reference to list of already serialized objects with index 0.

Custom serializers and deserializers
----------------------------------------

Elsa has some build in serializers and deserializers. So for example String is not treated as an object graph, but as single `char[]` value. 

For serialization Elsa has `Map<Class, Serializer>` which maps known classes
to their serializers. When element is found during graph traversal, 
Elsa tries to assign an serializer to this element by its class.
If no serializer is found, elsa will treat element as POJO and analyze its field structure.

For deserialization Elsa has `Map<Header Byte, Deserializer>` to get deserializer 
from known Header Byte. 

It is possible to register custom Serializer and Deserializer. 
In that case you need to provide not just binary converters, but also exact element class (subclasses are not found) and your own header byte. 

All custom serializers do not actually use Header Byte, but single Header Byte value,
followed by Custom Serializer ID. You provide this ID when Serializer is registered,
this ID will become part of serialized data. When data are deserialized, 
the Deserializer needs to be registered under the same ID.

*TODO this is already implemented, code example*

Custom serializers are tricky to implement for recursive objects. 
If you implement serializer for new collection, you also need to serialize its subelements.
That means calling back Elsa graph traversal. 
You will also need to careful with handling possible cyclic references, 
and reference counters. 

Cyclic references
-------------------
Elsa handles cyclic references. It keeps track of objects it visited during graph traversal in `IdentityHashMap`.
On second visit objects are serialized as back-reference in binary data. 

*TODO it is possible to use `HashMap` and perform deduplication on serialization (using `Object.equals()`), no overhead on deserialization, document this feature.*

Deserialization reverses this process. Elsa traverses binary stream, deserialized objects are kept in indexed List. 

*TODO normalized reference list, document feature*

Class structure
--------------------------
TODO