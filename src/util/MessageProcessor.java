package util;

import util.Objects.Message;

import java.io.*;

/**
 * 处理消息发送与接收。
 * @author soulike
 */
public class MessageProcessor
{
    /**
     * 向指定流发送一个 Message 对象。
     *
     * @param message 要发送的 Message 对象。
     * @param out     指定输出流。
     */
    public static void sendMessage(Message message, OutputStream out) throws IOException
    {
        ObjectOutputStream objOut = new ObjectOutputStream(out);
        objOut.writeObject(message);
        objOut.flush();
    }
}
