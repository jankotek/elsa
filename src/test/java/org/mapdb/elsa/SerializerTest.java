package org.mapdb.elsa;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;

@SuppressWarnings({"rawtypes","unchecked"})
public class SerializerTest {

    SerializerPojo s = new SerializerPojo();

    @Test public void UUID2() throws IOException {
        UUID u = UUID.randomUUID();
        assertEquals(u, SerializerBaseTest.clone(u));
    }

    @Test public void string_ascii() throws IOException {
        String s = "adas9 asd9009asd";
        assertEquals(s, SerializerBaseTest.clone(s));
        s = "";
        assertEquals(s, SerializerBaseTest.clone(s));
        s = "    ";
        assertEquals(s, SerializerBaseTest.clone(s));
    }

    @Test public void compression_wrapper() throws IOException {
        byte[] b = new byte[100];
        new Random().nextBytes(b);
        assertTrue(Arrays.equals(b, SerializerBaseTest.clone(b)));

        b = Arrays.copyOf(b, 10000);
        assertTrue(Arrays.equals(b, SerializerBaseTest.clone(b)));
    }

    @Test public void java_serializer_issue536() throws IOException {
        Long l = 1111L;
        assertEquals(l, SerializerBaseTest.clone(l));
    }



    @Test public void array() throws IOException {
        Object[] a = new Object[]{1,2,3,4};
        assertTrue(Arrays.equals(a, (Object[]) SerializerBaseTest.clone(a)));
    }

    @Test public void Long() throws IOException {
        for(Long i= (long) -1e5;i<1e5;i++){
            assertEquals(i, SerializerBaseTest.clone(i));
        }

        for(Long i=0L;i>0;i+=1+i/10000){
            assertEquals(i, SerializerBaseTest.clone(i));
        }

        Random r = new Random();
        for(int i=0;i<1e6;i++){
            Long a = r.nextLong();
            assertEquals(a, SerializerBaseTest.clone(a));
        }

    }


    @Test public void Int() throws IOException {
        for(Integer i= (int) -1e5;i<1e5;i++){
            assertEquals(i, SerializerBaseTest.clone(i));
        }

        for(Integer i=0;i>0;i+=1+i/10000){
            assertEquals(i, SerializerBaseTest.clone(i));

        }

        Random r = new Random();
        for(int i=0;i<1e6;i++){
            Integer a = r.nextInt();
            assertEquals(a, SerializerBaseTest.clone(a));
        }
    }

	@Test public void testCharSerializer() throws IOException {
		for (char character = 0; character < Character.MAX_VALUE; character++) {
			assertEquals("Serialized and de-serialized characters do not match the original", (int) character,
					(int) SerializerBaseTest.clone(character));
		}
	}

	@Test public void testStringXXHASHSerializer() throws IOException {
		String randomString = UUID.randomUUID().toString();
		for (int executionCount = 0; executionCount < 100; randomString = UUID.randomUUID()
				.toString(), executionCount++) {
			assertEquals("Serialized and de-serialized Strings do not match the original", randomString,
                    SerializerBaseTest.clone(randomString));
		}
	}


	@Test public void testStringInternSerializer() throws IOException {
		String randomString = UUID.randomUUID().toString();
		for (int executionCount = 0; executionCount < 100; randomString = UUID.randomUUID()
				.toString(), executionCount++) {
			assertEquals("Serialized and de-serialized Strings do not match the original", randomString,
                    SerializerBaseTest.clone(randomString));
		}
	}

	@Test public void testBooleanSerializer() throws IOException {
		assertTrue("When boolean value 'true' is serialized and de-serialized, it should still be true",
                SerializerBaseTest.clone(true));
		assertFalse("When boolean value 'false' is serialized and de-serialized, it should still be false",
                SerializerBaseTest.clone(false));
	}

	@Test public void testRecIDSerializer() throws IOException {
		for (Long positiveLongValue = 0L; positiveLongValue > 0; positiveLongValue += 1 + positiveLongValue / 10000) {
			assertEquals("Serialized and de-serialized record ids do not match the original", positiveLongValue,
                    SerializerBaseTest.clone(positiveLongValue));
		}
	}

	@Test public void testLongArraySerializer() throws IOException {
		(new ArraySerializerTester<long[]>() {

			@Override
			void populateValue(long[] array, int index) {
				array[index] = random.nextLong();
			}

			@Override
			long[] instantiateArray(int size) {
				return new long[size];
			}

			@Override
			void verify(long[] array) throws IOException {
				assertArrayEquals("Serialized and de-serialized long arrays do not match the original", array,
                        SerializerBaseTest.clone(array));
			}

		}).test();
	}

	@Test public void testCharArraySerializer() throws IOException {
		(new ArraySerializerTester<char[]>() {

			@Override
			void populateValue(char[] array, int index) {
				array[index] = (char) (random.nextInt(26) + 'a');
			}

			@Override
			char[] instantiateArray(int size) {
				return new char[size];
			}

			@Override
			void verify(char[] array) throws IOException {
				assertArrayEquals("Serialized and de-serialized char arrays do not match the original", array,
						SerializerBaseTest.clone(array));
			}
		}).test();
	}

	@Test public void testIntArraySerializer() throws IOException {
		(new ArraySerializerTester<int[]>() {

			@Override
			void populateValue(int[] array, int index) {
				array[index] = random.nextInt();
			}

			@Override
			int[] instantiateArray(int size) {
				return new int[size];
			}

			@Override
			void verify(int[] array) throws IOException {
				assertArrayEquals("Serialized and de-serialized int arrays do not match the original", array,
						SerializerBaseTest.clone(array));
			}
		}).test();
	}

	@Test public void testDoubleArraySerializer() throws IOException {
		(new ArraySerializerTester<double[]>() {

			@Override
			void populateValue(double[] array, int index) {
				array[index] = random.nextDouble();
			}

			@Override
			double[] instantiateArray(int size) {
				return new double[size];
			}

			void verify(double[] array) throws IOException {
				assertArrayEquals("Serialized and de-serialized double arrays do not match the original", array,
						SerializerBaseTest.clone(array), 0);
			}
		}).test();
	}

	@Test public void testBooleanArraySerializer() throws IOException {
		(new ArraySerializerTester<boolean[]>() {

			@Override
			void populateValue(boolean[] array, int index) {
				array[index] = random.nextBoolean();
			}

			@Override
			boolean[] instantiateArray(int size) {
				return new boolean[size];
			}

			@Override
			void verify(boolean[] array) throws IOException {
				assertArrayEquals("Serialized and de-serialized boolean arrays do not match the original", array,
						SerializerBaseTest.clone(array));
			}
		}).test();
	}

	@Test public void testShortArraySerializer() throws IOException {
		(new ArraySerializerTester<short[]>() {

			@Override
			void populateValue(short[] array, int index) {
				array[index] = (short) random.nextInt();
			}

			@Override
			short[] instantiateArray(int size) {
				return new short[size];
			}

			@Override
			void verify(short[] array) throws IOException {
				assertArrayEquals("Serialized and de-serialized short arrays do not match the original", array,
						SerializerBaseTest.clone(array));
			}
		}).test();
	}

	@Test public void testFloatArraySerializer() throws IOException {
		(new ArraySerializerTester<float[]>() {

			@Override
			void populateValue(float[] array, int index) {
				array[index] = random.nextFloat();
			}

			@Override
			float[] instantiateArray(int size) {
				return new float[size];
			}

			@Override
			void verify(float[] array) throws IOException {
				assertArrayEquals("Serialized and de-serialized float arrays do not match the original", array,
						SerializerBaseTest.clone(array), 0);
			}

		}).test();
	}

	private abstract class ArraySerializerTester<A> {
		Random random = new Random();
		abstract void populateValue(A array, int index);

		abstract A instantiateArray(int size);

		abstract void verify(A array) throws IOException;

		void test() throws IOException {
			verify(getArray());
		}

		private A getArray() {
			int size = random.nextInt(100);
			A array = instantiateArray(size);
			for (int i = 0; i < size; i++) {
				populateValue(array, i);
			}
			return array;
		}
	}

}
