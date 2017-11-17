package org.mapdb.elsa

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.*
import kotlin.test.assertFailsWith

class RecursionProblem{

    val depth = 1e6.toLong()

    val elem = (0L until depth).fold(R(null,-1),{ a, i->R(a,i)})

    @Test
    fun print_depth(){
        assertFailsWith<StackOverflowError> {
            println(elem.depth())
        }
    }

    @Test
    fun java_ser(){
        val out0 = ByteArrayOutputStream()
        val out = ObjectOutputStream(out0)
        assertFailsWith<StackOverflowError> {
            out.writeObject(elem)
        }
    }


    @Test
    fun elsa_ser(){
        val ser = ElsaSerializerPojo()
        val out0 = ByteArrayOutputStream()
        val out = DataOutputStream(out0)

        ser.serialize(out, elem)

        val in0 = ByteArrayInputStream(out0.toByteArray())
        val in1 = DataInputStream(in0)
        assertFailsWith<StackOverflowError> {
            val elem2 = ser.deserialize(in1)
            assertEquals(elem, elem2)
        }
    }

}



class R(val r:R?, val i:Long):Serializable{

    fun depth():Long = if(r==null) 1 else r.depth()+1

    /** non recursive equal*/
    override fun equals(other: Any?): Boolean {
        if(other !is R)
            return false
        var this2:R? = this
        var other2:R? = other
        while(true){
            if(this2 == null && other2==null)
                return true
            else if(this2==null || other2 == null)
                return false

            if(this2.i != other2.i)
                return false

            this2 = this2.r
            other2 = other2.r
        }
    }
}