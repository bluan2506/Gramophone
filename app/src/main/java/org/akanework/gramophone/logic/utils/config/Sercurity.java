package org.akanework.gramophone.logic.utils.config;

import org.akanework.gramophone.logic.utils.firebase.FirebaseEventUtils;

import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import Decoder.BASE64Decoder;
import Decoder.BASE64Encoder;

/**
 * Created by Quang Phúc on 15/10/24.
 */
public class Sercurity {

    static String key1 = "KJHB";
    static String key2 = "KU9876";
    static String key3 = "jkloj";

    public static String decrypt(String input) {
        try {
            byte[] message = new BASE64Decoder().decodeBuffer(input);
            final MessageDigest md = MessageDigest.getInstance("md5");
            final byte[] digestOfPassword = md.digest((key1 + key2 + key3).getBytes("utf-8"));
            final byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
            for (int j = 0, k = 16; j < 8; ) {
                keyBytes[k++] = keyBytes[j++];
            }

            final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
            final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
            final Cipher decipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            decipher.init(Cipher.DECRYPT_MODE, key, iv);
            final byte[] plainText = decipher.doFinal(message);
            return new String(plainText, "UTF-8");
        } catch (Exception e) {
            FirebaseEventUtils.getInstances().recordException(e);
        }
        return "";
    }

    public static String encrypt(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] digestOfPassword = md.digest((key1 + key2 + key3).getBytes("utf-8"));
            byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
            for (int j = 0, k = 16; j < 8; ) {
                keyBytes[k++] = keyBytes[j++];
            }

            SecretKey key = new SecretKeySpec(keyBytes, "DESede");
            IvParameterSpec iv = new IvParameterSpec(new byte[8]);
            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] plainTextBytes = message.getBytes("utf-8");
            byte[] cipherText = cipher.doFinal(plainTextBytes);
            String encodedCipherText = new BASE64Encoder().encode(cipherText);
            return encodedCipherText;
        } catch (Exception e) {
            FirebaseEventUtils.getInstances().recordException(e);
        }
        return "";
    }
}
