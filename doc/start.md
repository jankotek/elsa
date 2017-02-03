Intro
=======

Get it
-------------

Elsa is available in Maven repository. 
Jar files can be  [downloaded here](http://mvnrepository.com/artifact/org.mapdb/elsa).
Elsa has no dependencies and requires Java6. 
Maven snipped is bellow, latest VERSION is [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.mapdb/elsa/badge.svg](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.mapdb%22%20AND%20a%3Aelsa)

```xml
<dependency>
    <groupId>org.mapdb</groupId>
    <artifactId>elsa</artifactId>
    <version>VERSION</version>
</dependency>
```

Hello World
------------------

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

Configuration with Maker pattern
---------------------------------

Elsa uses Maker pattern (also known as Builder pattern) to set configuration parameters
in developer friendly way. 

Example bellow will change Cyclic Graph resultion settings to deduplication and will 
register `this` instance as an singleton:

<!---#file#doc/intro_maker.java--->
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