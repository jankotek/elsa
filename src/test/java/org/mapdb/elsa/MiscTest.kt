package org.mapdb.elsa

import org.junit.Test
import org.junit.Assert.*
import java.util.*
import java.util.Arrays.asList

class MiscTest{

    @Test fun arrays_array(){
       fun clone(a1:Any?){
           val ser = SerializerPojo()
           assertEquals(a1, SerializerBaseTest.clonePojo(a1, ser))
       }

        clone(asList<Any>())
        clone(asList<Any>("aa","bb"))
    }


}