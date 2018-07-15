package util;

import java.io.IOException;
import java.nio.file.*;

/**
 * 递归删除器。
 */
public class recursiveDeleter
{
    /**
     * 递归删除指定文件/文件夹。
     *
     * @param filePath 要删除的文件/文件夹 Path 对象。
     */
    public static void delete(Path filePath) throws IOException
    {
        if (filePath.toFile().exists())
        {
            // 如果是一个文件，直接删除
            if (filePath.toFile().isFile())
            {
                Files.delete(filePath);
            }
            // 如果是一个目录，就需要递归删除
            else
            {
                Object[] fileList = Files.list(filePath).toArray();
                for (Object pathObj : fileList)
                {
                    delete((Path) pathObj);
                }
                Files.delete(filePath);
            }
        }
    }
}
