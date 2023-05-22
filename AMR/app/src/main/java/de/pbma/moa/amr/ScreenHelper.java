package de.pbma.moa.amr;

public class ScreenHelper {
    private static final byte[] DELIMITER = "###".getBytes();

    public byte[][] splitFrameBytes(byte[] frameBytes) {
        int delimiterIndex = -1;
        for (int i = 0; i < frameBytes.length - DELIMITER.length + 1; i++) {
            boolean foundDelimiter = true;
            for (int j = 0; j < DELIMITER.length; j++) {
                if (frameBytes[i + j] != DELIMITER[j]) {
                    foundDelimiter = false;
                    break;
                }
            }
            if (foundDelimiter) {
                delimiterIndex = i;
                break;
            }
        }

        byte[] frameChunk1 = new byte[delimiterIndex];
        byte[] frameChunk2 = new byte[frameBytes.length - delimiterIndex - DELIMITER.length];

        System.arraycopy(frameBytes, 0, frameChunk1, 0, frameChunk1.length);
        System.arraycopy(frameBytes, delimiterIndex + DELIMITER.length, frameChunk2, 0, frameChunk2.length);

        return new byte[][]{frameChunk1, frameChunk2};
    }

    public boolean containsDelimiter(byte[] array) {
        for (int i = 0; i < array.length - DELIMITER.length + 1; i++) {
            boolean foundDelimiter = true;
            for (int j = 0; j < DELIMITER.length; j++) {
                if (array[i + j] != DELIMITER[j]) {
                    foundDelimiter = false;
                    break;
                }
            }
            if (foundDelimiter) {
                return true;
            }
        }
        return false;
    }

    public byte[] concatenateByteArrays(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }
}
