package jrdiff;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksum {

    private static final String DEFAULT_TYPE = "SHA-1";
    private MessageDigest messageDigest;
    private boolean isDone = false;

    public Checksum(String hashType) throws NoSuchAlgorithmException {

        messageDigest = MessageDigest.getInstance(hashType);

    }

    public Checksum() throws NoSuchAlgorithmException {
        this(DEFAULT_TYPE);
    }

    public void addData(byte data) {
        messageDigest.update(data);
    }

    public void addData(byte[] data) {
        addData(data, 0, data.length);
    }

    public void addData(byte[] data, int offset, int length) {
        messageDigest.update(data, offset, length);
    }

    public byte[] getHash() {

        if (isDone)
            throw new RuntimeException("Hash object already used");
        isDone = true;

        return messageDigest.digest();
    }

    public String getHexHash() {

        if (isDone)
            throw new RuntimeException("Hash object already used");
        isDone = true;

        StringBuffer hexString = new StringBuffer();

        for (byte crumb : messageDigest.digest()) {
            String hex = Integer.toHexString(0xFF & crumb);
            if (hex.length() < 2)
                hex = "0" + hex;
            hexString.append(hex);
        }

        return hexString.toString();

    }

    public String getBase64Hash() {
        if (isDone)
            throw new RuntimeException("Hash object already used");
        isDone = true;

        return Base64.encodeBytes(messageDigest.digest());
    }

    public void reset() {
        messageDigest.reset();
        isDone = false;
    }

}
