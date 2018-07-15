package SocketProcessor;

import util.Objects.*;

import static util.MessageProcessor.*;
import static util.AESKeyGenerator.*;
import static util.recursiveDeleter.*;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.security.*;

/**
 * 文件上传处理器。
 *
 * @author soulike
 */
public class FileUploadProcessor implements SocketProcessor
{
    /**
     * 创建 Cipher 对象需要的信息
     * */
    private final Key key;
    private final IvParameterSpec iv;
    /**
     * 上传文件存放根目录。
     */
    private final Path root;
    /**
     * 加密方法。
     */
    private static final String ENCRYPT_MODE = "AES/CFB8/NoPadding";


    /**
     * 处理器初始化构造器。
     *
     * @param root 上传文件存放的文件夹。
     */
    public FileUploadProcessor(Path root) throws IOException
    {
        key = getAESKey("JavaUploader");
        iv = new IvParameterSpec("1122334455667788".getBytes());

        this.root = root;
        if (Files.notExists(root))
        {
            Files.createDirectories(root);
        }
    }


    /**
     * 处理上传文件。
     * 循环接收 UploadFileInfo 对象，在接收到该对象后依据该对象内容来决定下一步动作。
     * 如果对象中的 isFile 为 true，就根据 fileSize 从流中接收指定字节的数据存为文件；
     * 如果对象中的 isFile 为 false，就根据 filePath 仅创建路径。
     *
     * @param socket ServerSocket 产生的 Socket 对象。
     */
    public void processSocket(Socket socket) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, InvalidAlgorithmParameterException
    {
        // 创建 Cipher 实例
        Cipher inCipher = Cipher.getInstance(ENCRYPT_MODE);
        Cipher outCipher = Cipher.getInstance(ENCRYPT_MODE);
        inCipher.init(Cipher.DECRYPT_MODE, key, iv);
        outCipher.init(Cipher.ENCRYPT_MODE, key, iv);

        try (final CipherInputStream decryptedIn = new CipherInputStream(socket.getInputStream(), inCipher);
             final CipherOutputStream encryptedOut = new CipherOutputStream(socket.getOutputStream(), outCipher))
        {
            // 用于读 UploadFileInfo 对象的输入流
            final ObjectInputStream objIn = new ObjectInputStream(decryptedIn);
            // 用于读文件二进制信息的输入流
            final DataInputStream dataIn = new DataInputStream(decryptedIn);

            // 用于存放当前上传的 UploadFileInfo 对象
            UploadFileInfo currentFileInfo = null;
            // 存储文件时使用的输出流
            DataOutputStream fileOut = null;
            // 文件/文件夹路径所用 Path 对象
            Path currentFilePath = null;

            try
            {
                while (true)
                {
                    // 服务器不断尝试是不是还有下一个文件，如果没有就结束这一次传输
                    try
                    {
                        currentFileInfo = (UploadFileInfo) objIn.readObject();
                    }
                    catch (EOFException e)
                    {
                        break;
                    }

                    currentFilePath = Paths.get(root.toString(), currentFileInfo.getFilePath());

                    // 删除重名文件/文件夹
                    if (Files.exists(currentFilePath))
                    {
                        delete(currentFilePath);
                    }


                    // 如果是文件，就读取文件大小并从流中取出指定大小字节
                    if (currentFileInfo.isFile())
                    {
                        // 如果文件已经存在，覆盖
                        if (Files.exists(currentFilePath))
                        {
                            Files.delete(currentFilePath);
                        }
                        Files.createFile(currentFilePath);

                        fileOut = new DataOutputStream(new FileOutputStream(currentFilePath.toFile()));

                        int readBytesNum = 0;
                        long totalReadBytesNum = 0;
                        final byte[] buffer = new byte[512];
                        while (totalReadBytesNum < currentFileInfo.getFileSize())
                        {
                            // 最多读到当前文件结束为止
                            readBytesNum = dataIn.read(buffer, 0, (int) (currentFileInfo.getFileSize() - totalReadBytesNum));
                            fileOut.write(buffer, 0, readBytesNum);
                            totalReadBytesNum += readBytesNum;
                        }
                        fileOut.close();
                    }
                    // 如果是目录，就根据对象信息创建这个目录
                    else
                    {
                        if (Files.notExists(currentFilePath))
                        {
                            Files.createDirectories(currentFilePath);
                        }
                    }
                }
                sendMessage(new Message(true, "上传成功"), encryptedOut);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                sendMessage(new Message(false, "上传失败"), encryptedOut);
            }
        }
    }
}
