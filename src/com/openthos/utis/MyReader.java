package com.openthos.utis;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import android.os.Environment;

public class MyReader {
    private static BufferedReader mBr;
    public  static  ArrayList<String> getArraylist(File file) {
        ArrayList<String> mAl = new ArrayList<String>();
        try {
            mAl.clear();
            BufferedReader mBr = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = mBr.readLine()) != null) {
                mAl.add(line);
            }
            mBr.close();
        } catch (Exception e) {
            return mAl;
        }
        return mAl;
    }

    public  static String getFileDes(File file) {
        BufferedReader reader=null;
        String str=null;
        String content=null;
        try {
            InputStreamReader read=new InputStreamReader(new FileInputStream(file),"UTF-8");
            mBr = new BufferedReader(new FileReader(file));
            int line =1;
            while ((str = mBr.readLine()) != null) {
               line++;
               content+=str + "\n";
            }
            mBr.close();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
            if (mBr != null) {
                try {
                    mBr.close();
                } catch (IOException e1) {
                }
            }
        }
        return content;
    }

    public static  void writeFile(File file, String fileName) {
        FileWriter  mFw = null;
        try {
            mFw = new FileWriter(file);
            mFw.write(fileName + "\n");
            mFw.write("1");
            mFw.flush();
        } catch (Exception e) {
        } finally {
            if (mFw != null) {
                try {
                    mFw.close();
                } catch (IOException e) {
                    throw new RuntimeException("close fail");
                }
            }
        }
 }
}
