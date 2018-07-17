package SocketProcessor;

import java.net.Socket;

/**
 * 对传入 Socket 对象进行处理。
 *
 * @author soulike
 */
public interface SocketProcessor
{
    /**
     * 对传入 Socket 对象进行处理的函数。
     *
     * @param socket 由 ServerSocket.accept() 产生的 Socket 对象。
     */
    void process(Socket socket) throws Exception;
}
