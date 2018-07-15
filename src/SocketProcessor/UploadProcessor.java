package SocketProcessor;

import util.Objects.Message;
import util.Objects.UploadFileInfo;

import static util.Decompressor.Decompressor.*;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import static util.MessageProcessor.MessageProcessor.*;

public class UploadProcessor implements SocketProcessor
{
    /**
     * 加密 Cipher 对象
     */
    private final Cipher cipher;
    /**
     * 上传文件存放根目录
     */
    private final Path root;
    /**
     * 加密方法
     */
    private static final String ENCRYPT_MODE = "AES/CFB8/NoPadding";

    public UploadProcessor(Path root) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, InvalidAlgorithmParameterException
    {
        Key key = getKey("JavaUploader");
        cipher = Cipher.getInstance(ENCRYPT_MODE);
        IvParameterSpec iv = new IvParameterSpec("1122334455667788".getBytes());
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        this.root = root;
        if (Files.notExists(root))
        {
            Files.createDirectories(root);
        }
    }

    private static Key getKey(String password)
    {
        byte[] keyData = get16BytesKetData(password);
        return new SecretKeySpec(keyData, "AES");
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


    /**
     * 处理上传文件。
     * 循环接收 UploadFileInfo 对象，在接收到该对象后依据该对象内容来决定下一步动作。
     * 如果对象中的 isFile 为 true，就根据 fileSize 从流中接收指定字节的数据存为文件；
     * 如果对象中的 isFile 为 false，就根据 filePath 仅创建路径。
     *
     * @param socket ServerSocket 产生的 Socket 对象
     */
    public void processSocket(Socket socket)
    {

    }
}
