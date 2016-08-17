package com.openthos.utis;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import android.os.Environment;

public class MyReader {
    public static  ArrayList<String> getArraylist(File file) {
        ArrayList<String> al = new ArrayList<String>();
        try {
            al.clear();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = br.readLine()) != null) {
                al.add(line);
            }
            br.close();
        } catch (Exception e) {
            return al;
        }
        return al;
    }
}
