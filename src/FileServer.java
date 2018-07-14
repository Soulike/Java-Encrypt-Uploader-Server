import SocketProcessor.UploadProcessor;
import util.Logger.MyLogger;
import util.ThreadPool.ThreadPool;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class FileServer
{
    public static void main(String[] args)
    {
        MyLogger logger = new MyLogger("主线程");
        if (args.length != 2)
        {
            logger.logError("参数不正确，请输入正确参数。例如: java FileServer 2333 /home/java/files");
        }
        else
        {
            Path root = Paths.get(args[1]);
            try
            {
                UploadProcessor processor = new UploadProcessor(root);
                try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0])))
                {
                    logger.logInfo(String.format("服务器运行在 %s 端口，下载根目录为 %s", args[0], root.toAbsolutePath().toString()));
                    ThreadPool pool = new ThreadPool();
                    while (true)
                    {
                        Socket socket = serverSocket.accept();
                        pool.createServer(socket, processor);
                    }
                }
                catch (IOException e)
                {
                    logger.logError("服务器主线程开启失败");
                    e.printStackTrace();
                }
            }
            catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e)
            {
                logger.logError("加密模块错误");
                e.printStackTrace();
            }
            catch (IOException e)
            {
                logger.logError("上传根目录创建失败");
                e.printStackTrace();
            }


        }
    }
}
