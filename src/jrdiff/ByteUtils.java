package jrdiff;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class ByteUtils {


    /**
     * Reads from inputstream and fills buffer entirely if possible
     *
     * @param input
     * @param buffer byte array
     * @param length number of bytes to read
     * @return number of bytes read
     * @throws IOException
     */
    public static int fillRead(InputStream input, byte[] buffer, int length) throws IOException {

        int bufferMark = 0;
        while (bufferMark < length) {

            int numRead = input.read(buffer, bufferMark, (length - bufferMark));

            if (numRead < 0)
                break;

            bufferMark += numRead;
        }

        return bufferMark;
    }

    public static int fillRead(InputStream input, byte[] buffer) throws IOException {
        return fillRead(input, buffer, buffer.length);
    }

    /**
     * Reads from RandomAccessFile and fills buffer entirely if possible
     *
     * @param input
     * @param buffer
     * @return number of bytes read
     * @throws IOException
     */
    public static int fillRead(RandomAccessFile input, byte[] buffer, int length) throws IOException {

        int bufferMark = 0;
        while (bufferMark < length) {

            int numRead = input.read(buffer, bufferMark, (length - bufferMark));

            if (numRead < 0)
                break;

            bufferMark += numRead;
        }

        return bufferMark;
    }


    /**
     * Compares two byte arrays
     *
     * @param arr1
     * @param arr2
     * @return whether byte arrays are equal
     */
    public static boolean areArraysEqual(byte[] arr1, byte[] arr2) {

        if (arr1.length != arr2.length)
            return false;

        for (int i = 0; i < arr1.length; i++)
            if (arr1[i] != arr2[i])
                return false;

        return true;
    }


    /**
     * Prints out contents of a byte array in hex notation. For debugging.
     */
    public static void printArray(byte[] data) {

        System.out.println("Printing byte array:");

        int count = 0;
        for (byte crumb : data) {

            System.out.printf("%d, ", crumb);

            count++;
            if (count % 30 == 0)
                System.out.println();

        }

        System.out.println();
    }

}
