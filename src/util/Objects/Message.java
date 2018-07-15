package util.Objects;

import java.io.Serializable;

/**
 * 双方通信消息。
 */
public class Message implements Serializable
{
    /**
     * 是否是传输成功的消息。
     */
    private final boolean isSuccessful;
    /**
     * 要传输的消息文本。
     */
    private final String message;

    /**
     * 消息构造函数。
     *
     * @param isSuccessful 是否是传输成功的消息。
     * @param message      要传输的消息文本。
     */
    public Message(boolean isSuccessful, String message)
    {
        this.isSuccessful = isSuccessful;
        this.message = message;
    }

    /**
     * 获得消息文本
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * 获得是否传输成功信息
     */
    public boolean isSuccessful()
    {
        return isSuccessful;
    }
}
