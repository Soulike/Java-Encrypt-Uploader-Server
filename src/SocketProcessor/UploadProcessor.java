package SocketProcessor;

import util.Objects.UploadFileInfo;

import static util.Deleter.recursiveDeleter.*;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class UploadProcessor implements SocketProcessor
{
    /**
     * 加密 Cipher 对象
     */
    private final Cipher inCipher;
    private final Cipher outCipher;
    /**
     * 上传文件存放根目录
     */
    private final Path root;
    /**
     * 加密方法
     */
    private static final String ENCRYPT_MODE = "AES/CFB8/NoPadding";

    /**
     * 上传文件时使用的缓冲区
     */
    private final byte[] buffer;

    public UploadProcessor(Path root) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, InvalidAlgorithmParameterException
    {
        Key key = getKey("JavaUploader");
        inCipher = Cipher.getInstance(ENCRYPT_MODE);
        outCipher = Cipher.getInstance(ENCRYPT_MODE);
        IvParameterSpec iv = new IvParameterSpec("1122334455667788".getBytes());
        inCipher.init(Cipher.DECRYPT_MODE, key, iv);
        outCipher.init(Cipher.ENCRYPT_MODE, key, iv);

        this.root = root;
        if (Files.notExists(root))
        {
            Files.createDirectories(root);
        }

        buffer = new byte[512];
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
    public void processSocket(Socket socket) throws IOException, ClassNotFoundException
    {
        try (CipherInputStream decryptedIn = new CipherInputStream(socket.getInputStream(), inCipher);
             CipherOutputStream encryptedOut = new CipherOutputStream(socket.getOutputStream(), outCipher))
        {
            // 用于读 UploadFileInfo 对象的输入流
            ObjectInputStream objIn = new ObjectInputStream(decryptedIn);
            // 用于读文件二进制信息的输入流
            DataInputStream dataIn = new DataInputStream(decryptedIn);
            // 用于存放当前上传的 UploadFileInfo 对象
            UploadFileInfo currentFileInfo = null;
            // 存储文件时使用的输出流
            DataOutputStream fileOut = null;
            // 文件/文件夹路径所用 Path 对象
            Path currentFilePath = null;

            while (!socket.isClosed())
            {
                currentFileInfo = (UploadFileInfo) objIn.readObject();
                currentFilePath = Paths.get(root.toString(), currentFileInfo.getFilePath());
                if (Files.exists(currentFilePath))
                {
                    delete(currentFilePath);
                }
                // 如果是文件，就读取文件大小并从流中取出指定大小字节
                if (currentFileInfo.isFile())
                {
                    Files.createFile(currentFilePath);
                    fileOut = new DataOutputStream(new FileOutputStream(currentFilePath.toFile()));
                    // 这一次文件读取的总字节数
                    long totalReadBytesNum = 0;
                    int readBytesNum = 0;
                    while (totalReadBytesNum < currentFileInfo.getFileSize())
                    {
                        readBytesNum = dataIn.read(buffer);
                        fileOut.write(buffer, 0, readBytesNum);
                        totalReadBytesNum += readBytesNum;
                    }
                    fileOut.close();
                }
                // 如果是目录，就根据对象信息创建这个目录
                else
                {
                    Files.createDirectories(currentFilePath);
                }
            }
        }
    }
}
