package org.openthos.ota.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.os.Environment;
import android.text.TextUtils;

public class OtaReader {
    private static BufferedReader mBr;

    public static ArrayList<String> getArraylist(File file) {
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

    public static String getFileDes(File file) {
        BufferedReader reader = null;
        String str = null;
        String content = "";
        try {
            InputStreamReader read = new InputStreamReader(new FileInputStream(file), "UTF-8");
            mBr = new BufferedReader(new FileReader(file));
            int line = 1;
            while ((str = mBr.readLine()) != null) {
                line++;
                content += str + "\n";
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

    public static void writeFile(File file, String fileName) {
        FileWriter mFw = null;
        try {
            mFw = new FileWriter(file);
            mFw.write(fileName + "\n");
            mFw.write("1" + "\n");
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

    public static String readFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return null;
        }
        StringBuilder buffer = new StringBuilder("");
        BufferedReader reader = null;
        try {
            InputStreamReader is = new InputStreamReader(new FileInputStream(file), "utf-8");
            reader = new BufferedReader(is);
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!buffer.toString().equals("")) {
                    buffer.append("\r\n");
                }
                buffer.append(line);
            }
            return buffer.toString();
        } catch (IOException e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean writeFile(String fileName, String content, boolean append) {
        if (TextUtils.isEmpty(content)) {
            return false;
        }
        FileWriter fileWriter = null;
        if (!new File(fileName).exists()){
            creatFile(fileName);
        }
        try {
            fileWriter = new FileWriter(fileName, append);
            fileWriter.write(content);
            fileWriter.flush();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }

    public static boolean creatFile(String filePath) {
        String dirpath = filePath.substring(0, filePath.lastIndexOf("/"));
        if (dirpath != null) {
            creatDirFile(dirpath);
            if (!getFile(filePath).exists()) {
                try {
                    getFile(filePath).createNewFile();
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }

    public static void creatDirFile(String dirPath) {
        if (!getFile(dirPath).exists()) {
            getFile(dirPath).mkdirs();
        }
    }

    private static File getFile(String filePath) {
        return new File(filePath);
    }
}
