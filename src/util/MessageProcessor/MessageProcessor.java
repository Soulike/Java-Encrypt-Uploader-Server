package util.MessageProcessor;

import util.Objects.Message;

import java.io.*;
import java.net.Socket;

/**
 * 处理消息发送与接收。
 */
public class MessageProcessor
{
    public static void sendMessage(Message message, OutputStream out) throws IOException
    {
        ObjectOutputStream objOut = new ObjectOutputStream(out);
        objOut.writeObject(message);
        objOut.flush();
    }
}
