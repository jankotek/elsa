package org.mapdb.elsa

import org.junit.Test

import org.junit.Assert.*
import java.io.Serializable
import java.util.*

/**
 * Randomly generates data, tests that cyclic reference (Object Stack) works correctly.
 */
class CyclicReferenceTest{


    companion object {
        class NotSeriazable1 {}
        class NotSeriazable2 {}

        val SINGLETON1 = NotSeriazable1()
        val SINGLETON2 = NotSeriazable2()


        interface Container<A> : Generator<A> {
            fun add(a: A, content: Array<Any?>): A
            fun getContent(a: A): Array<Any?>
        }

        interface Generator<A> {
            fun random(): A
        }

        object NULL : Generator<Any?> {
            override fun random() = null
        }

        object STRING : Generator<String> {
            override fun random() = TT.randomString()
        }

        object INTEGER : Generator<Int> {
            override fun random() = Random().nextInt()
        }

        object SINGLETON : Generator<Any> {
            override fun random() =
                    if (Random().nextBoolean()) SINGLETON1 else SINGLETON2
        }

        object ARRAY : Container<Array<Any?>> {
            override fun add(a: Array<Any?>, content: Array<Any?>): Array<Any?> {
                val origSize = a.size;
                val a = Arrays.copyOf(a, a.size + content.size)
                System.arraycopy(content, 0, a, origSize, content.size)
                return a;
            }

            override fun getContent(a: Array<Any?>): Array<Any?> {
                return a
            }

            override fun random(): Array<Any?> {
                return Array<Any?>(0,{null})
            }
        }

        class ArrayWrapper(var obj:Array<Any?>) : Serializable {
            override fun equals(other: Any?): Boolean {
                return other is ArrayWrapper
                        && Arrays.deepEquals(this.obj,other.obj)
            }

            override fun hashCode(): Int {
                return Arrays.deepHashCode(this.obj)
            }
        }

        object ARRAY_WRAPPER : Container<ArrayWrapper>{

            override fun random(): ArrayWrapper {
                return ArrayWrapper(ARRAY.random())
            }

            override fun add(a: ArrayWrapper, content: Array<Any?>): ArrayWrapper {
                a.obj = ARRAY.add(a.obj, content)
                return a
            }

            override fun getContent(a: ArrayWrapper): Array<Any?> {
                return a.obj
            }

        }

        abstract class CollectionContainer :Container<MutableCollection<Any?>>{
            override fun add(a: MutableCollection<Any?>, content: Array<Any?>): MutableCollection<Any?> {
                a.addAll(content)
                return a
            }

            override fun getContent(a: MutableCollection<Any?>): Array<Any?> {
                return a.toTypedArray()
            }

        }

        object ARRAYLIST: CollectionContainer(){
            override fun random() = ArrayList<Any?>()
        }


        abstract class MapContainer:Container<MutableMap<Any?,Any?>>{
            override fun add(a: MutableMap<Any?, Any?>, content: Array<Any?>): MutableMap<Any?, Any?> {
                for(i in 0 until content.size step 2){
                    val key = content[i]
                    val value = if(i+1>=content.size) null else content[i+1]
                    a.put(key, value)
                }
                return a
            }

            override fun getContent(a: MutableMap<Any?, Any?>): Array<Any?> {
                return (a.keys.toList()+a.values.toList()).toTypedArray()
            }


        }

        object TREEMAP: MapContainer(){
            override fun random() = TreeMap<Any?,Any?>()
        }

        object HASHMAP: MapContainer(){
            override fun random() = HashMap<Any?,Any?>()

        }

        object LINKEDHASHMAP: MapContainer(){
            override fun random() = LinkedHashMap<Any?,Any?>()
        }

        object TREESET: CollectionContainer(){
            override fun random() = TreeSet<Any?>()
        }

        object HASHSET: CollectionContainer(){
            override fun random() = HashSet<Any?>()
        }


        object LINKEDHASHSET: CollectionContainer(){
            override fun random() = LinkedHashSet<Any?>()
        }

        object LINKEDLIST: CollectionContainer(){
            override fun random() = LinkedList<Any?>()
        }


        val cc = arrayOf(NULL,
                STRING, INTEGER, SINGLETON, ARRAY, ARRAYLIST,
                TREEMAP, HASHMAP, LINKEDHASHMAP,
                TREESET, HASHSET, LINKEDHASHSET, LINKEDLIST
            )

        val ccNoNullArray = cc.filter{it!=ARRAY && it!=NULL}

        val ccObj = ccNoNullArray+ARRAY_WRAPPER

        val singletons = arrayOf(SINGLETON1, SINGLETON2)

        val ser = SerializerBase(singletons, null, null, null)
        val serPojo = SerializerPojo(singletons, null, null, null, null, null)

    }

    fun eq(v1:Any?, v2:Any?){
        if(v1 is Array<*>)
            assertArrayEquals(v1, v2 as  Array<*>)
        else
            assertEquals(v1,v2)
    }

    @Test fun run() {
        for(c in cc){
            val v1 = c.random()
            eq(v1, SerializerBaseTest.clonePojo(v1, ser))
        }
    }

    @Test fun run2() {
        for(c in ccObj){
            val v1 = c.random()
            eq(v1, SerializerBaseTest.clonePojo(v1, serPojo))
        }
    }


    @Test fun cyclic() {
        for(c in ccNoNullArray.filter{it is Container}) {
            for (c2 in ccNoNullArray) try{
                var v = c.random()
                v = (c as Container<Any?>).add(v,
                        arrayOf(c2.random(), c2.random(), c2.random()))

                val cloned = SerializerBaseTest.clonePojo(v,ser)
                eq(v, cloned)
            }catch(e:ClassCastException){
                if(e.message!!.contains("Comparable").not())
                    throw e
            }
        }
    }

    @Test fun cyclic2() {
        for(c in ccObj.filter{it is Container}) {
            for (c2 in ccObj) try{
                var v = c.random()
                v = (c as Container<Any?>).add(v,
                        arrayOf(c2.random(), c2.random(), c2.random()))

                val cloned = SerializerBaseTest.clonePojo(v,serPojo)
                eq(v, cloned)
            }catch(e:ClassCastException){
                if(e.message!!.contains("Comparable").not())
                    throw e
            }
        }
    }

}
