package SocketProcessor;

import util.MyLogger;
import util.Objects.*;

import static util.MessageProcessor.*;
import static util.AESKeyGenerator.*;
import static util.recursiveDeleter.*;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;

/**
 * 文件上传处理器。
 *
 * @author soulike
 */
public class FileUploadProcessor implements SocketProcessor
{
    /**
     * 保证文件系统同步读取的锁
     */
    private final Object fileSystemLock = new Object();
    /**
     * 创建 Cipher 对象需要的信息
     */
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

    private final MyLogger logger;


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

        logger = new MyLogger("上传处理模块");
    }


    /**
     * 处理上传文件。
     * 循环接收 UploadFileInfo 对象，在接收到该对象后依据该对象内容来决定下一步动作。
     * 如果对象中的 isFile 为 true，就根据 fileSize 从流中接收指定字节的数据存为文件；
     * 如果对象中的 isFile 为 false，就根据 filePath 仅创建路径。
     *
     * @param socket ServerSocket 产生的 Socket 对象。
     */
    public void process(Socket socket) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, InvalidAlgorithmParameterException
    {
        // 创建 Cipher 实例
        Cipher inCipher = Cipher.getInstance(ENCRYPT_MODE);
        Cipher outCipher = Cipher.getInstance(ENCRYPT_MODE);
        inCipher.init(Cipher.DECRYPT_MODE, key, iv);
        outCipher.init(Cipher.ENCRYPT_MODE, key, iv);

        // 创建上传临时文件夹名
        String tempFolderName = String.format("%s-%s-%d", String.valueOf(System.currentTimeMillis()), socket.getInetAddress().getHostAddress(), socket.getPort());

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
            // 文件/文件夹具体路径所用 Path 对象
            Path currentFilePath = null;
            // 文件/文件夹存入的文件夹路径所用 Path 对象
            Path uploadFileRootPath = null;

            // 临时文件/文件夹临时路径所用 Path 对象
            Path tempFilePath = null;
            // 文件/文件夹存入的临时文件夹路径所用 Path 对象
            Path tempRootPath = Paths.get(root.toString(), tempFolderName);

            boolean hasReadUploadRootPath = false;

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

                    // 文件最终实际的存储位置
                    currentFilePath = Paths.get(root.toString(), currentFileInfo.getFile().toPath().toString());
                    // 文件的临时上传位置
                    tempFilePath = Paths.get(root.toString(), tempFolderName, currentFileInfo.getFile().toPath().toString());

                    // 第一个传输过来的信息必然是根目录信息或文件信息
                    if (!hasReadUploadRootPath)
                    {
                        uploadFileRootPath = currentFilePath;
                        hasReadUploadRootPath = true;
                    }

                    // 删除重名文件/文件夹
                    if (Files.exists(currentFilePath))
                    {
                        delete(currentFilePath);
                    }

                    // 如果是文件，就读取文件大小并从流中取出指定大小字节
                    if (currentFileInfo.isFile())
                    {
                        // 生成临时文件夹
                        if (Files.notExists(tempFilePath.getParent()))
                        {
                            Files.createDirectories(tempFilePath.getParent());
                        }

                        // 如果临时文件已经存在，覆盖
                        if (Files.exists(tempFilePath))
                        {
                            Files.delete(tempFilePath);
                        }
                        Files.createFile(tempFilePath);

                        fileOut = new DataOutputStream(new FileOutputStream(tempFilePath.toFile()));

                        int readBytesNum = 0;
                        long totalReadBytesNum = 0;
                        final byte[] buffer = new byte[512];
                        while (totalReadBytesNum < currentFileInfo.getFileSize())
                        {
                            // 最多读到当前文件结束为止
                            readBytesNum = dataIn.read(buffer, 0, (int) (currentFileInfo.getFileSize() - totalReadBytesNum));
                            if (readBytesNum != -1)
                            {
                                fileOut.write(buffer, 0, readBytesNum);
                                totalReadBytesNum += readBytesNum;
                            }
                            else
                            {
                                throw new SocketException("意外的文件结尾");
                            }
                        }
                        fileOut.close();
                    }
                    // 如果是目录，就根据对象信息创建这个目录
                    else
                    {
                        if (Files.notExists(tempFilePath))
                        {
                            Files.createDirectories(tempFilePath);
                        }
                    }
                }

                // 以下代码把临时文件夹下内容移动到上传根目录下
                if (currentFileInfo != null)
                {
                    final Path tempRootPathCopy = Paths.get(tempRootPath.toString(), currentFileInfo.getFileName());
                    final Path uploadFileRootPathCopy = uploadFileRootPath;
                    Files.walkFileTree(tempRootPath, new FileVisitor<>()
                    {
                        @Override
                        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes)
                        {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException
                        {
                            Path relativePath = tempRootPathCopy.relativize(path);
                            Path realPath = Paths.get(uploadFileRootPathCopy.toString(), relativePath.toString()).normalize();
                            if (Files.notExists(realPath.getParent()))
                            {
                                Files.createDirectories(realPath.getParent());
                            }

                            Files.move(path, realPath, StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException
                        {
                            throw new IOException(e);
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException
                        {
                            Path relativePath = tempRootPathCopy.relativize(path);
                            Path realPath = Paths.get(uploadFileRootPathCopy.toString(), relativePath.toString());
                            if (Files.notExists(realPath))
                            {
                                Files.createDirectories(realPath);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
                sendMessage(new Message(true, "上传成功"), encryptedOut);
            }
            catch (SocketException e)
            {
                if (uploadFileRootPath != null)
                {
                    delete(uploadFileRootPath);
                }
                logger.logWarning("Socket 发生错误，与用户断开连接");
            }
            catch (ClassNotFoundException e)
            {
                if (uploadFileRootPath != null)
                {
                    delete(uploadFileRootPath);
                }
                logger.logError("用户发送文件对象错误");
                if (!socket.isClosed())
                {
                    sendMessage(new Message(false, "上传数据非法，请使用客户端上传文件"), encryptedOut);
                }
            }
            catch (IOException e)
            {
                if (uploadFileRootPath != null)
                {
                    delete(uploadFileRootPath);
                }
                logger.logError("服务器 IO 异常");
                if (!socket.isClosed())
                {
                    sendMessage(new Message(false, "服务器 IO 异常，请重新上传"), encryptedOut);
                }
            }
            finally
            {
                // 无论上传结果如何，都要关闭输出流并删除临时文件夹
                if (fileOut != null)
                {
                    fileOut.close();
                }

                if (tempRootPath != null)
                {
                    delete(tempRootPath);
                }
            }
        }
    }
}
