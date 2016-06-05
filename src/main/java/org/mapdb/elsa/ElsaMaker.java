package org.mapdb.elsa;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ElsaMaker is used to create and configure Elsa serializer.
 */
public class ElsaMaker {

    protected Object[] singletons = null;
    protected List<Class> classes = new ArrayList<Class>();
    protected ElsaClassCallback unknownClassNotification = null;

    protected Map<Class, ElsaSerializerBase.Ser> registeredSers = new HashMap();
    protected Map<Class, Integer> registeredSerHeaders = new HashMap();
    protected Map<Integer, ElsaSerializerBase.Deser> registeredDeser = new HashMap();

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
     * @param singletons
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
                objectStack,
                singletons,
                registeredSers,
                registeredSerHeaders,
                registeredDeser,
                unknownClassNotification,
                new ElsaClassInfoResolver.ArrayBased(classes.toArray(new Class[0]))
        );
    }

    /**
     * Register classes structure. It saves space since class structure does not have to be saved together with data.
     * Note: Order in which classes are registered defines storage format. To deserialize data back, you need to always register classes at the same order.
     * @param classes
     * @return
     */
    public ElsaMaker registerClasses(Class... classes){
        for(Class clazz:classes)
            this.classes.add(clazz);
        return this;
    }

    /**
     * Callback notified when class with unknown structure is serialized.
     *
     * @param callback
     * @return
     */
    public ElsaMaker unknownClassNotification(ElsaClassCallback callback){
        this.unknownClassNotification = callback;
        return this;
    }

    public <E> ElsaMaker registerSer(int header, Class<E> clazz, ElsaSerializerBase.Ser<E> ser){
        if(registeredSers.containsKey(clazz))
            throw new IllegalArgumentException("Class already has Ser registered: "+clazz);
        registeredSers.put(clazz, ser);
        registeredSerHeaders.put(clazz, header);

        return this;
    }

    public ElsaMaker registerDeser(int header, ElsaSerializerBase.Deser deser){
        if(registeredDeser.get(header)!=null)
            throw new IllegalArgumentException("Deser for header is already registered: "+header);
        registeredDeser.put(header, deser);
        return this;
    }

    public ElsaMaker referenceDisable() {
        objectStack = 1;
        return this;
    }

    public ElsaMaker referenceArrayEnable() {
        objectStack = 2;
        return this;
    }

    public ElsaMaker referenceHashMapEnable() {
        objectStack = 3;
        return this;
    }


}

