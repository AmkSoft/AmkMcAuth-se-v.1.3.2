package com.mooo.amksoft.amkmcauth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher
{
  private static String getType(String type)
  {
    type = type.trim();
    if (type.equalsIgnoreCase("md5")) {
      return "MD5";
    }
    if ((type.equalsIgnoreCase("sha-512")) || (type.equalsIgnoreCase("sha512"))) {
      return "SHA-512";
    }
    if ((type.equalsIgnoreCase("sha-256")) || (type.equalsIgnoreCase("sha256"))) {
      return "SHA-256";
    }
    if (type.equalsIgnoreCase("rauth")) {
      return "AMKAUTH";
    }
    if (type.equalsIgnoreCase("amkauth")) {
      return "AMKAUTH";
    }
    return type;
  }
  
  private static String hash(String data, String type)
    throws NoSuchAlgorithmException
  {
    String rtype = getType(type);
    if (rtype.equals("AMKAUTH")) {
      rtype = "SHA-512";
    }
    MessageDigest md = MessageDigest.getInstance(rtype);
    md.update(data.getBytes());
    byte[] byteData = md.digest();
    StringBuilder sb = new StringBuilder();
    byte[] arrayOfByte1;
    int j = (arrayOfByte1 = byteData).length;
    for (int i = 0; i < j; i++)
    {
      byte aByteData = arrayOfByte1[i];sb.append(Integer.toString((aByteData & 0xFF) + 256, 16).substring(1));
    }
    return sb.toString();
  }
  
  public static String encrypt(String data, String type)
    throws NoSuchAlgorithmException
  {
    String rtype = getType(type);
    if (rtype.equals("AMKAUTH"))
    {
      for (int i = 0; i < 25; i++) {
        data = hash(data, rtype);
      }
      return data;
    }
    return hash(data, rtype);
  }
}
