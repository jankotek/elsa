Known classes
===============

Elsa knows howto serialize some classes. For example for `new Long(11)` 
it can write simple number without analyzing `Long` class and it fields.
Here is list of known classes and explanation how much space they consume 
in serialized form. 

Minimal size after serialization is single header byte, which identifies data type. 

Some prominent singletons (`""`, Integer(1)...) have their own header byte, and consume only single byte.

Array structures are typically written as packed size, followed by data of each array entry.

All classes are separated in two categories
- **non recursive** can be written in single element, and do not need recursion to write their content (or subelements). That includes numbers, Strings and primitive arrays.
- **recursive** can nit be written in single element, but have subelement with flexible types. We need recursion to writte sublements. That are collections and non primitive arrays.

Numbers
-----

There is no difference between primitive numbers and their wrappers. Elsa uses
autoboxing for primitive types. So `int` field can be swapped with `Integer` without
affecting serialized data.

### Integer 

`Integer`s  between -9 and 16 (inclusive) and `Integer.MIN_VALUE` and `Integer.MAX_VALUE` 
are treated as singletons. They consume single byte after serialization

`Integer`s which consume less then 3 bytes (left most bites are zero) are written in packed form.

Full integers (no left most zeroes) are written in 5 bytes, header byte plus data.

### Long

Same as `Integer`, but packed size is 7 bytes.

### Boolean

Both values serialize into single byte.

### Character

`Char(0)` and `Char(1)` are singletons and consume single byte.

`Char` with value bellow 255 consumes two bytes.

Other values consume three bytes.

### Short
-1, 0 and 1 are singletons and consume single byte. 

Between -255 and 255 consumes two bytes.

Other consume three bytes.


### Byte
-1, 0 and 1 are singletons and consume single byte.

Otherwise two bytes.

### Double
-1D, 0D, 1D are singletons and consume single byte

Integer values between 0 and 255 consume two bytes

Integer less than 0xFFFF consume three bytes

Integer less than 0xFFFFFFFF (four bytes) consume five bytes

Otherwise 9 bytes are used.


### Float
-1D, 0D, 1D are singletons and consume single byte

Integer values between 0 and 255 consume two bytes

Integer less than 0xFFFF consume three bytes

Otherwise 4 bytes are used.


String
-----------

Empty string `""` is a singleton and consumes single byte

If string length is bellow 11, the len is combined into header byte.
Otherwise  String length is written in packed form after header byte. 

String chars are written in packed form. 7bit ASCII consume single byte, bellow 32K two bytes etc..


Primitive arrays
----------------------

### char[]
Array length is written in packed form after header byte. 
Chars are written in packed form. 7bit ASCII consume single byte, bellow 32K two bytes etc..

### byte[]
Serializer checks if all elements in array are equal, in that case only two bytes + packed size are used. 

Otherwise header byte + packed size + data are written.

### boolean[]
Header byte and packed size are written. All booleans are packed into bites, so 8 booleans consume single byte. 

### short[]
Header byte and packed size. Each array entry consumes two bytes.


### float[]
Header byte and packed size. Each array entry consumes four bytes.


### double[]
Header byte and packed size. Each array entry consumes eight bytes.

### int[]
Header byte and packed size.

Serializer checks for maximal and minimal values in array, and tries to write
all entries with less bytes or in packed form. So in best case each entry consumes single byte, in worst case each entry consumes four bytes.


### long[]
Header byte and packed size.

Serializer checks for maximal and minimal values in array, and tries to write
all entries with less bytes or in packed form. So in best case each entry consumes single byte, in worst case each entry consumes eight bytes.

Other non recursive classes
----------------------

### BigInteger
Is converted to `byte[]` using `.toByteArray()` and saved. 

### BigDecinal
Is converted to `byte[]` using `.toByteArray()` and saved. 
Scale is written in packed integer.

### Class
Converted to Class Name and saved as String.

### Date
Saved as 8 byte timestamp with extra header byte.

### UUID
Saved as two 8 byte longs with extra header byte.

Collections
-------------------------------------------

Collections are recursive classes. Elsa only stores basic information such as collection type and size. Subelements are serialized separately using another step in recursion (graph traversal)

### ArrayList, LinkedList, LinkedHashSet, LinkedHashMap, Properties 

Single header byte plus packed size. Subelements are serialized in next step of graph traversal.
Order of elements is preserved after deserialization.

### HashSet and HashMap
Same as other collection. But order might not be preserved after deserialization in some cases.

### TreeSet and TreeMap
Same as other collections. But `comparator` is recursively serialized as an subelement.

Non primitive arrays
----------------------
Non primitive arrays are recursive classes and are serialized similar way as collections. 

Elsa checks if all elements in array are null. In that case entry serialization is skipped.

On top of packed size and recursive entries. Elsa also writtes component type, which is type of elements in an array (`Object[]` is different from `String[]`).


