package org.mapdb.elsa

import org.junit.Test
import org.junit.Assert.*
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import java.util.*
import java.util.Arrays.asList

class MiscTest{

    @Test fun arrays_array(){
       fun clone(a1:Any?){
           val ser = ElsaSerializerPojo()
           assertEquals(a1, ElsaSerializerBaseTest.clonePojo(a1, ser))
       }

        clone(asList<Any>())
        clone(asList<Any>("aa","bb"))
    }


    class Extern2: Externalizable {

        companion object{
            var readCount = 0
            var writeCount = 0
        }

        var notSet:Any? =null

        var field:String = ""

        var readCountHere = 0
        var writeCountHere = 0

        override fun readExternal(oi: ObjectInput) {
            readCount++
            readCountHere++
            field = oi.readUTF()
        }

        override fun writeExternal(out: ObjectOutput) {
            writeCount++
            writeCountHere++
            out.writeUTF(field)
        }

    }

    @Test fun externalizableUsed(){
        assertEquals(0,Extern2.readCount)
        assertEquals(0,Extern2.writeCount)

        val e = Extern2()
        e.notSet = "ignore this"
        e.field = "dont ignore"
        val e2 = ElsaSerializerBaseTest.clonePojo(e)

        assertEquals(0, e.readCountHere)
        assertEquals(1, e.writeCountHere)

        assertEquals(1,Extern2.readCount)
        assertEquals(1,Extern2.writeCount)

        assertEquals(1, e2.readCountHere)
        assertEquals(0, e2.writeCountHere)

        assertNull(e2.notSet)
        assertEquals("dont ignore", e.field)
    }

}