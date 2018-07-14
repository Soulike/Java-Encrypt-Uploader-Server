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
    private final Cipher cipher;
    private final Path root;
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
     * 上传对象格式
     * UploadFileInfo Object
     * File Content
     * 先读取一个 UploadFileInfo 对象，再读取文件的二进制内容
     */
    public void processSocket(Socket socket) throws IOException, ClassNotFoundException
    {
        UploadFileInfo fileObj;

        InputStream rawIn = socket.getInputStream();
        OutputStream rawOut = socket.getOutputStream();
        CipherInputStream in = new CipherInputStream(rawIn, cipher);

        // 先读取文件对象
        ObjectInputStream objIn = new ObjectInputStream(in);
        fileObj = (UploadFileInfo) objIn.readObject();

        Path tempFilePath = Files.createTempFile(null, ".tmp");
        // 再读取二进制内容
        DataInputStream dataIn = new DataInputStream(in);

        FileOutputStream fileOut = new FileOutputStream(tempFilePath.toFile());
        DataOutputStream fileDataOut = new DataOutputStream(fileOut);
        {
            byte[] buffer = new byte[256];
            int readLength = 0;
            while ((readLength = dataIn.read(buffer)) != -1)
            {
                fileDataOut.write(buffer, 0, readLength);
                fileDataOut.flush();
            }
            fileDataOut.close();

            if (fileDataOut.size() != fileObj.getFileSize())
            {
                sendMessage(new Message(false, "文件上传不完整，请重试"), rawOut);
                Files.delete(tempFilePath);
            }
            else if (fileObj.isZipped())
            {
                decompress(tempFilePath, root);
                sendMessage(new Message(true, "文件夹上传成功"), rawOut);
            }
            else if (!fileObj.isZipped())
            {
                Path finalFilePath = Paths.get(root.toAbsolutePath().toString(), fileObj.getFileName());
                Files.move(tempFilePath, finalFilePath, StandardCopyOption.REPLACE_EXISTING);
                sendMessage(new Message(true, "文件上传成功"), rawOut);
            }
            socket.close();
        }
    }
}
