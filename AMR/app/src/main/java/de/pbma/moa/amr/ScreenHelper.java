package de.pbma.moa.amr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class ScreenHelper {
    private static final byte[] DELIMITER = "###".getBytes();


    public int containsDelimiter(byte[] array) {
        for (int i = 0; i < array.length - DELIMITER.length + 1; i++) {
            boolean foundDelimiter = true;
            for (int j = 0; j < DELIMITER.length; j++) {
                if (array[i + j] != DELIMITER[j]) {
                    foundDelimiter = false;
                    break;
                }
            }
            if (foundDelimiter) {
                return i;
            }
        }
        return -1;
    }

    public static SplitResult getArrays(byte[] array, int index) {
        byte[] firstPart = Arrays.copyOfRange(array, 0, index);
        byte[] lastPart = Arrays.copyOfRange(array, index + 3, array.length);
        SplitResult result = new SplitResult(firstPart, lastPart);
        return result;

    }

    public byte[] concatenateByteArrays(byte[] array1, byte[] array2) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(array1);
        outputStream.write(array2);
        return outputStream.toByteArray( );
    }

    public static class SplitResult {
        private final byte[] firstPart;
        private final byte[] lastPart;

        public SplitResult(byte[] firstPart, byte[] lastPart) {
            this.firstPart = firstPart;
            this.lastPart = lastPart;
        }

        public byte[] getFirstPart() {
            return firstPart;
        }

        public byte[] getLastPart() {
            return lastPart;
        }
    }
}
