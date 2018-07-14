package util.ThreadPool;

import util.Logger.MyLogger;
import SocketProcessor.SocketProcessor;

import java.net.Socket;
import java.util.*;

/**
 * 线程池。在创建后预先创建指定数量的线程等待处理。
 *
 * @author soulike
 */
public class ThreadPool
{
    /**
     * 最小线程数量。
     */
    private int minThreadNum;
    /**
     * 最大线程数量。
     */
    private int maxThreadNum;

    /**
     * 等待线程队列，此队列中线程都处于阻塞状态等待分配。
     */
    private final List<ThreadService> waitingThreadList;
    /**
     * 运行线程队列，此队列中线程都在处理用户连接。
     */
    private final List<ThreadService> runningThreadList;

    /**
     * 等待线程队列锁。
     */
    private final Object waitingThreadListLock = new Object();
    /**
     * 运行线程队列锁。
     */
    private final Object runningThreadListLock = new Object();

    /**
     * 线程池使用的 MyLogger。
     */
    private final MyLogger logger;

    /**
     * 默认构造函数，默认范围 50-200。
     */
    public ThreadPool()
    {
        this(50, 200);
    }

    /**
     * 限定线程池的线程数目限制。如果传入负值，将会忽略并使用默认值。
     * 如果最小值大于最大值，将会对调两个值。
     *
     * @param minThreadNum 最小线程数。当可用线程数小于这个数时将向线程池中追加新线程。
     * @param maxThreadNum 最大线程数。当可用线程数大于这个数时将清理线程池中休眠线程。
     */
    public ThreadPool(int minThreadNum, int maxThreadNum)
    {
        if (minThreadNum < 0)
        {
            minThreadNum = 50;
        }
        if (maxThreadNum < 0)
        {
            maxThreadNum = 200;
        }

        if (maxThreadNum < minThreadNum)
        {
            int temp = maxThreadNum;
            maxThreadNum = minThreadNum;
            minThreadNum = temp;
        }

        this.minThreadNum = minThreadNum;
        this.maxThreadNum = maxThreadNum;
        waitingThreadList = new LinkedList<>();
        runningThreadList = new LinkedList<>();

        logger = new MyLogger("线程池");

        // 启动进程池管理线程
        ThreadPoolManager poolManager = new ThreadPoolManager();
        new Thread(poolManager).start();
    }

    /**
     * 从等待线程队列中找一个可运行的线程。如果都在运行，就创建一个新线程并返回。
     * 此函数无需处理线程在两个队列中切换的问题。
     */
    private ThreadService getRunnableThread()
    {
        // 遍历等待线程列表，找到一个还活着的线程返回
        for (ThreadService threadService : waitingThreadList)
        {
            if (threadService.isAlive())
            {
                return threadService;
            }
        }

        // 如果都死了或者是等待线程列表为空，就创建一个新线程
        ThreadService newThread = new ThreadService();
        newThread.start();
        return newThread;
    }

    /**
     * 从等待线程队列中找一个空闲线程创建一个服务线程，并将其从等待线程队列转移到运行线程队列。
     *
     * @param socket    新建连接产生的 Socket 对象。
     * @param processor 对这个连接进行处理的服务程序，实现 Server 接口。
     */
    public void createServer(Socket socket, SocketProcessor processor)
    {
        ThreadService threadService = getRunnableThread();
        synchronized (waitingThreadList)
        {
            waitingThreadList.remove(threadService);
        }
        synchronized (runningThreadList)
        {
            runningThreadList.add(threadService);
        }
        threadService.runThreadService(socket, processor);
    }

    /**
     * 得到现在所有线程的数量。
     */
    public int getCurrentThreadNum()
    {
        return waitingThreadList.size() + runningThreadList.size();
    }

    /**
     * 得到现在正在运行线程的数量。
     */
    public int getCurrentRunningThreadNum()
    {
        return runningThreadList.size();
    }

    /**
     * 得到现在正在等待线程的数量。
     */
    public int getCurrentWaitingThreadNum()
    {
        return waitingThreadList.size();
    }

    /**
     * 线程池管理类，开启另一个守护线程定时管理线程池。
     */
    class ThreadPoolManager implements Runnable
    {
        private final Timer timer = new Timer(true);

        /**
         * 当进程池进程数超出范围时进行干预。
         * 如果线程数量不够，就往等待队列里面添加新的。
         * 如果线程数量过多，从等待队列中关闭指定个线程。
         */
        private void checkPoolRange()
        {
            // 如果线程数量不够，就往等待队列里面添加新的
            if (getCurrentThreadNum() < minThreadNum)
            {
                addThreadToMinNum();
            }

            // 如果线程数量过多，从等待队列中关闭指定个线程
            if (getCurrentThreadNum() > maxThreadNum)
            {
                closeThreadToMaxNum();
            }
        }

        /**
         * 整理进程池。
         * 清理要求退出和死亡的线程。
         * 移动线程到正确队列。
         */
        private void rearrangePool()
        {
            // 清理要求退出和死亡的线程
            cleanPool();
            // 移动线程到正确队列
            moveThread();
        }

        /**
         * 把运行队列中等待的线程移动到等待队列。
         */
        private void moveThread()
        {
            List<ThreadService> threadToMoveList = new ArrayList<>();
            // 找到所有正在等待任务的线程
            synchronized (runningThreadListLock)
            {
                for (ThreadService threadService : runningThreadList)
                {
                    if (threadService.isWaiting())
                    {
                        threadToMoveList.add(threadService);
                    }
                }

                // 从运行队列中删掉它们
                for (ThreadService threadService : threadToMoveList)
                {
                    runningThreadList.remove(threadService);
                }
            }

            synchronized (waitingThreadListLock)
            {
                // 添加到等待队列里
                waitingThreadList.addAll(threadToMoveList);
            }
        }

        /**
         * 清理池中被要求退出或已经死亡的线程。
         */
        private void cleanPool()
        {
            synchronized (runningThreadListLock)
            {
                cleanList(runningThreadList);
            }

            synchronized (waitingThreadListLock)
            {
                cleanList(waitingThreadList);
            }
        }

        /**
         * 清理一个队列中被要求退出或已经死亡的线程。
         *
         * @param threadServiceList 一个线程队列。
         */
        private void cleanList(List<ThreadService> threadServiceList)
        {
            List<ThreadService> exitThreads = new ArrayList<>();
            for (ThreadService threadService : threadServiceList)
            {
                if (threadService.isExit())
                {
                    exitThreads.add(threadService);
                }
            }

            for (ThreadService threadService : exitThreads)
            {
                threadServiceList.remove(threadService);
            }
        }

        /**
         * 在等待队列中添加线程，直到达到最小限制。
         */
        private void addThreadToMinNum()
        {
            ThreadService threadServiceTemp;
            synchronized (waitingThreadListLock)
            {
                while (getCurrentThreadNum() < minThreadNum)
                {
                    threadServiceTemp = new ThreadService();
                    threadServiceTemp.start();
                    waitingThreadList.add(threadServiceTemp);
                }
            }
        }

        /**
         * 在等待队列中关闭线程，直到达到最大限制。
         */
        private void closeThreadToMaxNum()
        {
            synchronized (waitingThreadListLock)
            {
                int exitThreadNum = 0;
                int threadToExitNum = getCurrentThreadNum() - maxThreadNum;
                List<ThreadService> exitThread = new ArrayList<>();

                for (ThreadService threadService : waitingThreadList)
                {
                    exitThread.add(threadService);
                    exitThreadNum++;
                    if (exitThreadNum == threadToExitNum)
                    {
                        break;
                    }
                }

                for (ThreadService threadService : exitThread)
                {
                    threadService.setExit();
                }
            }
        }

        /**
         * 输出当前池的状态。
         */
        private void printPoolStatus()
        {
            logger.logInfo(String.format("线程池整理作业完成\n目前线程池线程数量: %d\n目前线程池等待线程数量: %d\n目前线程池运行线程数量: %d", getCurrentThreadNum(), getCurrentWaitingThreadNum(), getCurrentRunningThreadNum()));
        }

        /**
         * 调用 Timer 运行定时整理任务。
         */
        public void run()
        {
            timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    checkPoolRange();
                    rearrangePool();
                    printPoolStatus();
                }
            }, 0, 60 * 1000);
        }
    }
}

