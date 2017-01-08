package org.mapdb.elsa;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ElsaMaker is used to create and configure Elsa serializer.
 */
//TODO links to Elsa manual
final public class ElsaMaker {

    protected ClassLoader classLoader = null;
    protected Object[] singletons = null;
    protected List<Class> classes = new ArrayList<Class>();
    protected ElsaClassCallback unknownClassNotification = null;

    protected Map<Class, ElsaSerializerBase.Serializer> registeredSers = new HashMap();
    protected Map<Class, Integer> registeredSerHeaders = new HashMap();
    protected Map<Integer, ElsaSerializerBase.Deserializer> registeredDeser = new HashMap();

    /**
     *
     * 3 is {@link org.mapdb.elsa.ElsaStack.MapStack} with HashMap,
     * 2 is {@link org.mapdb.elsa.ElsaStack.IdentityArray},
     * 1 is {@link org.mapdb.elsa.ElsaStack.NoReferenceStack},
     * 0 is {@link org.mapdb.elsa.ElsaStack.MapStack} with IdentityHashMap,
     */
    protected int objectStack = 0;

    /**
     * Register list of singletons. Singletons are serialized using only two bytes. Deserialized singletons  keep reference equality.
     * Note: Order in which singletons are registered defines storage format. To deserialize data back, you need to always register singleton at the same order.
     *
     * @param singletons array of singletons to be added to list of singletons
     * @return this maker
     */
    public ElsaMaker singletons(Object... singletons) {
        this.singletons = singletons;
        return this;
    }

    /**
     * Creates new serializer with configuration from this builder
     *
     * @return new serializer
     */
    public ElsaSerializerPojo make() {
        return new ElsaSerializerPojo(
                classLoader,
                objectStack,
                singletons,
                registeredSers,
                registeredSerHeaders,
                registeredDeser,
                unknownClassNotification,
                new ElsaClassInfoResolver.ArrayBased(classes.toArray(new Class[0]), classLoader)
        );
    }

    /**
     * Register classes structure. It saves space since class structure does not have to be saved together with data.
     * Note: Order in which classes are registered defines storage format. To deserialize data back, you need to always register classes at the same order.
     * @param classes
     * @return this maker
     */
    public ElsaMaker registerClasses(Class... classes){
        for(Class clazz:classes)
            this.classes.add(clazz);
        return this;
    }

    /**
     * Callback notified when class with unknown structure is serialized.
     * You can than add unknown Class to your Class Catalog (or whatever you are using)
     *
     * @param callback
     * @return this maker
     */
    public ElsaMaker unknownClassNotification(ElsaClassCallback callback){
        this.unknownClassNotification = callback;
        return this;
    }

    /**
     * Register user serializer for single class.
     * <p/>
     * Elsa decides what Serializer to use for each object based on objects class.
     * Internally it uses {@code Map<Class, Deserializer>} to resolve serializer.
     * Exact class match is used, so subclasses are not recognized and has to be registered separately.
     * <p/>
     * Each custom serializer also has Header ID. That is stored as part of binary data,
     * Elsa uses this ID to decide what deserializer to use on deserialization.
     * You need to register matching serializer with {@link ElsaMaker#registerDeserializer(int, ElsaSerializerBase.Deserializer)}.
     * Without ID, Elsa would not be able to deserialize binary data.
     *
     * @param header deserializer ID, is stored in binary data and used on deserialization.
     * @param clazz object class is used to decide what serializer to use
     * @param serializer serializer which turns object instance to binary data
     * @param <E> type of object
     * @return this serializer
     */
    public <E> ElsaMaker registerSerializer(int header, Class<E> clazz, ElsaSerializerBase.Serializer<E> serializer){
        if(registeredSers.containsKey(clazz))
            throw new IllegalArgumentException("Class already has Serializer registered: "+clazz);
        registeredSers.put(clazz, serializer);
        registeredSerHeaders.put(clazz, header);

        return this;
    }

    /**
     * Register custom deserializer for single Header ID.
     * <p/>>
     * Elsa stores Header ID in binary data, it is used to decide what deserializer to use.
     * See {@link ElsaMaker#registerSerializer(int, Class, ElsaSerializerBase.Serializer)} for more details
      *
     * @param header associated with this deserializer.
     * @param deser deserializer used to turn binary data into object
     * @return this maker
     */
    public ElsaMaker registerDeserializer(int header, ElsaSerializerBase.Deserializer deser){
        if(registeredDeser.get(header)!=null)
            throw new IllegalArgumentException("Deserializer for header is already registered: "+header);
        registeredDeser.put(header, deser);
        return this;
    }

    /**
     * Disables reference tracking. With this setting Elsa will not recognize backward references.
     * Circular reference will cause infinitive loop and Stack Overflow on serialization.
     *
     * @return this maker
     */
    public ElsaMaker referenceDisable() {
        objectStack = 1;
        return this;
    }

    /**
     * Uses linear object array to track backward references. Might by faster on tiny (~5 elements) object graphs.
     *
      * @return this maker
     */
    public ElsaMaker referenceArrayEnable() {
        objectStack = 2;
        return this;
    }

    /**
     * Uses HashMap to track backward references.
     * Normally {@code IdentityHashMap} is used, this settings track references but also performs
     * deduplication using {@code hashCode()} and {@code equals()}.
     * This setting slows down serialization significantly, but has zero overhead on deserialization.
     *
     * @return this maker
     */
    public ElsaMaker referenceHashMapEnable() {
        objectStack = 3;
        return this;
    }

    /**
     * User defined Class Loader used by Elsa to load classes.
     *
     * @param classLoader user defined Class Loader
     * @return this maker
     */
    public ElsaMaker classLoader(ClassLoader classLoader){
        this.classLoader = classLoader;
        return this;
    }

}

