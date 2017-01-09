package com.openthos.utis;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class OtaMd5 {

    private final static int MD5_BUF_MAX = 1024;
    private final static int MD5_NUMBER_LEN = 16;

    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[MD5_BUF_MAX];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, MD5_BUF_MAX)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        System.out.println("" + bigInt.toString(MD5_NUMBER_LEN));
        String s = bigInt.toString(MD5_NUMBER_LEN);
        if (s.length() < MD5_NUMBER_LEN * 2) {
            int i = MD5_NUMBER_LEN * 2 - s.length();
            while (i-- > 0) {
                s = "0" + s;
            }
        }
        return s;
    }
}
