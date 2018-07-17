package util.ThreadPool;

import SocketProcessor.DatagramSocketProcessor;
import SocketProcessor.SocketProcessor;

import java.net.DatagramSocket;
import java.net.Socket;

/**
 * 线程类，继承 Thread，以实现对每一个新连接创建一个新线程。
 *
 * @author soulike
 */
public class ThreadService extends Thread
{
    /**
     * 需要被处理的对象，可以是 Socket 或者是 DatagramSocket。
     */
    private Socket socketConnection;
    private DatagramSocket datagramSocket;

    /**
     * 对这个连接进行处理的服务程序。
     */
    private SocketProcessor socketProcessor;
    private DatagramSocketProcessor datagramSocketProcessor;

    /**
     * 是否被要求退出。true-是，false-否。
     */
    private boolean exit;


    /**
     * 构造函数，用于创建一个等待线程。
     */
    public ThreadService()
    {
        socketConnection = null;
        socketProcessor = null;
        datagramSocketProcessor = null;
        exit = false;
    }

    /**
     * 用于在线程池内没有空余线程时直接创建新线程。
     *
     * @param socketConnection 新建连接产生的被处理 Socket 对象。
     * @param socketProcessor  对这个连接进行处理的服务程序，实现 SocketProcessor 接口。
     */
    public ThreadService(Socket socketConnection, SocketProcessor socketProcessor)
    {
        this.socketConnection = socketConnection;
        this.socketProcessor = socketProcessor;
        exit = false;
    }

    /**
     * 用于在线程池内没有空余线程时直接创建新线程。
     *
     * @param datagramSocket          新建连接产生的被处理 DatagramSocket 对象。
     * @param datagramSocketProcessor 对这个连接进行处理的服务程序，实现 DatagramSocketProcessor 接口。
     */
    public ThreadService(DatagramSocket datagramSocket, DatagramSocketProcessor datagramSocketProcessor)
    {
        this.datagramSocket = datagramSocket;
        this.datagramSocketProcessor = datagramSocketProcessor;
        exit = false;
    }

    /**
     * 判断这个线程是不是正在等待任务。
     */
    public boolean isWaiting()
    {
        return (socketConnection == null && socketProcessor == null && datagramSocket == null && datagramSocketProcessor == null && !exit && Thread.currentThread().isAlive());
    }

    /**
     * 判断这个线程是不是已经有任务并正在运行。
     */
    public boolean isRunning()
    {
        return (socketConnection != null && socketProcessor != null || datagramSocket != null && datagramSocketProcessor != null) && Thread.currentThread().isAlive();
    }

    /**
     * 把这个线程重新变为阻塞状态。
     */
    private synchronized void toWaiting()
    {
        socketConnection = null;
        socketProcessor = null;
        datagramSocket = null;
        datagramSocketProcessor = null;
        System.out.printf("线程 %s 休眠\n", Thread.currentThread().getName());
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
     * @param datagramSocket          新建连接产生的被处理 DatagramSocket 对象。
     * @param datagramSocketProcessor 对这个连接进行处理的服务程序，实现 datagramSocketProcessor 接口。
     */
    public synchronized void runThreadService(DatagramSocket datagramSocket, DatagramSocketProcessor datagramSocketProcessor)
    {
        this.datagramSocket = datagramSocket;
        this.datagramSocketProcessor = datagramSocketProcessor;
        notify();// 内容分配好了，唤醒线程
    }

    /**
     * 分配对象并启用线程。
     *
     * @param socketConnection 新建连接产生的被处理 Socket 对象。
     * @param socketProcessor  对这个连接进行处理的服务程序，实现 SocketProcessor 接口。
     */
    public synchronized void runThreadService(Socket socketConnection, SocketProcessor socketProcessor)
    {
        this.socketConnection = socketConnection;
        this.socketProcessor = socketProcessor;
        notify();// 内容分配好了，唤醒线程
    }

    /**
     * 判断这个线程处理的是否是一个 TCP 连接
     */
    private boolean isTCP()
    {
        return socketConnection != null && socketProcessor != null;
    }

    /**
     * 判断这个线程处理的是否是一个 UDP 连接
     */
    private boolean isUDP()
    {
        return datagramSocket != null && datagramSocketProcessor != null;
    }

    /**
     * 在执行线程之后，把对象交给对应处理器处理。
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
                    System.out.println(String.format("线程 %s 唤醒", Thread.currentThread().getName()));
                    if (isTCP())
                    {
                        System.out.println(String.format("线程 %s 处理 TCP 连接", Thread.currentThread().getName()));
                        socketProcessor.process(socketConnection);
                    }
                    else if (isUDP())
                    {
                        System.out.println(String.format("线程 %s 监听 UDP 封包", Thread.currentThread().getName()));
                        datagramSocketProcessor.process(datagramSocket);
                    }
                    toWaiting();
                }
            }
            System.out.println(String.format("线程 %s 退出", Thread.currentThread().getName()));
        }
        catch (Exception e)
        {
            System.err.println(String.format("线程 %s 处理请求时发生错误", Thread.currentThread().getName()));
            e.printStackTrace();
            toWaiting();
        }
    }
}

