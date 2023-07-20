package de.pbma.moa.amr;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class ScreenHelper {
    private static final byte[] DELIMITER = "######".getBytes();

    public boolean startsWithDel(byte[] array){
        for (int i = 0; i<DELIMITER.length; i++){
            if(array[i] != DELIMITER[i]){
                Log.e("tag", "contains: "+ i);
                return false;
            }
        }
        return true;
    }
    public byte [] removeDel(byte [] array){
        return Arrays.copyOfRange(array, DELIMITER.length, array.length);
    }

    public byte[] concatenateByteArrays(byte[] array1, byte[] array2) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(array1);
        outputStream.write(array2);
        return outputStream.toByteArray( );
    }
}
