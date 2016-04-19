package org.mapdb.elsa;


import java.util.ArrayList;
import java.util.List;

/**
 * ElsaMaker is used to create and configure Elsa serializer.
 */
public class ElsaMaker {

    protected Object[] singletons = null;
    protected List<Class> classes = new ArrayList<Class>();
    protected ClassCallback unknownClassNotification = null;

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
    public SerializerPojo make() {
        return new SerializerPojo(
                singletons,
                unknownClassNotification,
                new ClassInfoResolver.ArrayBased(classes.toArray(new Class[0]))
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
    public ElsaMaker unknownClassNotification(ClassCallback callback){
        this.unknownClassNotification = callback;
        return this;
    }
}
