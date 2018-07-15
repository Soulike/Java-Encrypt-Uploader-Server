package util.ThreadPool;

import util.MyLogger;
import SocketProcessor.SocketProcessor;

import java.net.*;

/**
 * 线程类，继承 Thread，以实现对每一个新连接创建一个新线程。
 *
 * @author soulike
 */
public class ThreadService extends Thread
{
    /**
     * 新建连接产生的 Socket 对象。
     */
    private Socket socket;

    /**
     * 对这个连接进行处理的服务程序，实现 Server 接口。
     */
    private SocketProcessor processor;

    /**
     * 是否被要求退出。true-是，false-否。
     */
    private boolean exit;

    /**
     * 线程内部使用的 MyLogger。
     */
    private final MyLogger logger;

    /**
     * 构造函数，用于创建一个等待线程。
     */
    public ThreadService()
    {
        this(null, null);
    }

    /**
     * 用于在线程池内没有空余线程时直接创建新线程。
     *
     * @param socket    新建连接产生的 Socket 对象。
     * @param processor 对这个连接进行处理的服务程序，实现 SocketProcessor 接口。
     */
    public ThreadService(Socket socket, SocketProcessor processor)
    {
        this.socket = socket;
        this.processor = processor;
        exit = false;
        logger = new MyLogger(Thread.currentThread().getName());
    }

    /**
     * 判断这个线程是不是正在等待任务。
     */
    public boolean isWaiting()
    {
        return socket == null && processor == null && !exit && Thread.currentThread().isAlive();
    }

    /**
     * 判断这个线程是不是已经有任务并正在运行。
     */
    public boolean isRunning()
    {
        return socket != null && processor != null && !exit && Thread.currentThread().isAlive();
    }

    /**
     * 把这个线程重新变为阻塞状态。
     */
    private synchronized void toWaiting()
    {
        socket = null;
        processor = null;
        logger.logInfo(String.format("线程 %s 休眠", Thread.currentThread().getName()));
    }

    /**
     * 通知这个线程退出执行。
     */
    public synchronized void setExit()
    {
        exit = true;
        notify();
    }

    /**
     * 检查这个线程是不是已经被要求退出或已经退出了。
     */
    public boolean isExit()
    {
        return exit || !Thread.currentThread().isAlive();
    }

    /**
     * 分配对象并启用线程。
     *
     * @param socket    新建连接产生的 Socket 对象。
     * @param processor 对这个连接进行处理的服务程序，实现 SocketProcessor 接口。
     */
    public synchronized void runThreadService(Socket socket, SocketProcessor processor)
    {
        this.socket = socket;
        this.processor = processor;
        notify();// 内容分配好了，唤醒线程
    }

    /**
     * 在执行线程之后，把 Socket 对象交给 server 对象处理。
     */
    public synchronized void run()
    {
        try
        {
            // 当不要求这个线程退出时再进行循环
            while (!exit)
            {
                // 如果状态为 false，就阻塞这个线程
                while (isWaiting() && !exit)
                {
                    wait();
                }

                // 当不要求这个线程退出时再进行操作
                if (!exit)
                {
                    logger.logInfo(String.format("线程 %s 唤醒", Thread.currentThread().getName()));
                    processor.processSocket(socket);
                    if (!socket.isClosed())
                    {
                        throw new SocketException("处理完成后的 Socket 未关闭");
                    }
                    toWaiting();
                }
            }
            logger.logInfo(String.format("线程 %s 退出", Thread.currentThread().getName()));
        }
        catch (Exception e)
        {
            logger.logError(String.format("线程 %s 处理请求时发生错误", Thread.currentThread().getName()));
            e.printStackTrace();
            toWaiting();
        }
    }
}

