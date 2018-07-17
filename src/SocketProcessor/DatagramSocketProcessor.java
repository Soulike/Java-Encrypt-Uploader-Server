package SocketProcessor;

import java.net.DatagramSocket;

/**
 * 对传入 DatagramSocket 对象进行监听并处理。
 *
 * @author soulike
 */
public interface DatagramSocketProcessor
{
    /**
     * 对传入 DatagramSocket 对象进行监听并处理的函数。
     *
     * @param datagramSocket 要监听的 DatagramSocket 对象。
     */
    void process(DatagramSocket datagramSocket) throws Exception;
}
