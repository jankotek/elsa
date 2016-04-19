package org.mapdb.elsa;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by jan on 4/18/16.
 */
public class ElsaMaker {

    protected Object[] singletons;
    protected List<Class> classes = new ArrayList<Class>();

    public ElsaMaker singletons(Object... singletons) {
        this.singletons = singletons;
        return this;
    }

    public SerializerPojo make() {
        return new SerializerPojo(
                singletons,
                null,
                new ClassInfoResolver.ArrayBased(classes.toArray(new Class[0]))
        );
    }

    public ElsaMaker registerClasses(Class... classes){
        for(Class clazz:classes)
            this.classes.add(clazz);
        return this;
    }
}
