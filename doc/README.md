Elsa Serialization 
==================================

Elsa is serialization framework for Java.
It offers more features and data serialized by Elsa are much smaller. 
Elsa was developed as part of [MapDB](http://www.mapdb.org) (formerly JDBM) since 2010, 
and was recently refactored into separate project.  

Elsa was developed as drop-in replacement for `ObjectOutputStream` serialization (Java Serialization).
It offers following features:

- Compact size of serialized data
    - 10-50x smaller data size compared to Java Serialization
    - Class Catalog (class names, field names) can be stored externaly to reduce data usage
    - Serialization is slower than Java Serialization, deserialization is faster
- 99% compatible with Java Serialization
    - Classes should implement `Serializable` interface (option to disable this)
    - Handles `Externalizable`, `objectReplace` and other tricks
    - It falls-back into OOS if advanced features are detected 
- Cyclic Graph handling
    - Optional deduplication using `Object.equals`
    - Options to tune reference tracking overhead to various graph sizes
    - Singletons handling 
- Plugable serializer model 
    - Your own serializer can be integrated into Object Graph traversal
    - Singleton resolvers etc... 
- Long term storage on mind
    - Storage format is well specified
    - Option to rename classes or fields with backward compatibility

    
    