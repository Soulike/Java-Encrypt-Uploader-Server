package SocketProcessor;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class UploadProcessor implements SocketProcessor
{
    private final Cipher cipher;

    public UploadProcessor() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException
    {
        Key key = getKey("JavaUploader");
        cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
    }

    private Key getKey(String password)
    {
        byte[] ketData = get16BytesKetData(password);
        return new SecretKeySpec(ketData, "AES");
    }

    private static byte[] get16BytesKetData(String password)
    {
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] BytesKeyData = new byte[16];
        int copyLength = passwordBytes.length >= 16 ? 16 : passwordBytes.length;
        for (int i = 0; i < copyLength; i++)
        {
            BytesKeyData[i] = passwordBytes[i];
        }

        for (int i = copyLength; i < 16; i++)
        {
            BytesKeyData[i] = 0;
        }

        return BytesKeyData;
    }


    public void processSocket(Socket socket)
    {

    }
}
