package SocketProcessor;

import util.Objects.Message;
import util.Objects.UploadFileInfo;

import static util.Decompressor.Decompressor.*;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import static util.MessageProcessor.MessageProcessor.*;

public class UploadProcessor implements SocketProcessor
{
    private final Cipher cipher;
    private final Path root;

    public UploadProcessor(Path root) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException
    {
        Key key = getKey("JavaUploader");
        cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);

        this.root = root;
        if (Files.notExists(root))
        {
            Files.createDirectories(root);
        }
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


    /**
     * 上传对象格式
     * UploadFileInfo Object
     * File Content
     * 先读取一个 UploadFileInfo 对象，再读取文件的二进制内容
     */
    public void processSocket(Socket socket) throws IOException, ClassNotFoundException
    {
        UploadFileInfo fileObj;
        try (InputStream rawIn = socket.getInputStream();
             CipherInputStream in = new CipherInputStream(rawIn, cipher))
        {
            // 先读取文件对象
            try (ObjectInputStream objIn = new ObjectInputStream(in))
            {
                fileObj = (UploadFileInfo) objIn.readObject();
            }

            Path tempFilePath = Files.createTempFile(null, "tmp");
            // 再读取二进制内容
            try (DataInputStream dataIn = new DataInputStream(in);
                 FileOutputStream out = new FileOutputStream(tempFilePath.toFile());
                 DataOutputStream dataOut = new DataOutputStream(out))
            {
                byte[] buffer = new byte[1024 * 1024];
                int readLength = 0;
                while ((readLength = dataIn.read(buffer)) != -1)
                {
                    dataOut.write(buffer, 0, readLength);
                }

                if (dataOut.size() != fileObj.getFileSize())
                {
                    sendMessage(new Message(false, "文件上传不完整，请重试"), socket);
                    Files.delete(tempFilePath);
                }
                else if (fileObj.isZipped())
                {
                    decompress(tempFilePath, root);
                    sendMessage(new Message(true, "文件上传成功"), socket);
                }
                else if (!fileObj.isZipped())
                {
                    Path finalFilePath = Paths.get(root.toAbsolutePath().toString(), fileObj.getFileName());
                    Files.move(tempFilePath, finalFilePath);
                    sendMessage(new Message(true, "文件上传成功"), socket);
                }
            }
        }
    }
}
