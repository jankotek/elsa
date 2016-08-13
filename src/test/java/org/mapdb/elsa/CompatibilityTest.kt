package org.mapdb.elsa

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import java.io.Serializable


data class CompatibilitycTestExternalizable(var a:Int = 111, var b:String = "oidewoicewc")
    : Serializable, Externalizable {

    override fun writeExternal(out: ObjectOutput) {
        out.writeInt(a)
        out.writeUTF(b)
    }

    override fun readExternal(`in`: ObjectInput) {
        a = `in`.readInt()
        b = `in`.readUTF()
    }
}

class CompatibilitycTest {

    @Test
    fun externalizable() {
        val ser = ElsaMaker().make()
        val obj = CompatibilitycTestExternalizable(a=22, b="aa")
        //println(TT.toHex(ser, obj ))
        val str = "b080002f6f72672e6d617064622e656c73612e436f6d7061746962696c697479635465737445787465726e616c697a61626c650001af80aced000573720f7f7f7fff002f6f72672e6d617064622e656c73612e436f6d7061746962696c697479635465737445787465726e616c697a61626c6578707708000000160002616178"

        assertEquals(obj, TT.hexDeser(ser, str))
    }


    @Test
    fun externalizable_registered() {
        val ser = ElsaMaker().registerClasses(CompatibilitycTestExternalizable::class.java).make()
        val obj = CompatibilitycTestExternalizable(a=22, b="aa")
        //println(TT.toHex(ser, obj ))
        val str = "ad800000001600026161"

        assertEquals(obj, TT.hexDeser(ser, str))
    }
}
