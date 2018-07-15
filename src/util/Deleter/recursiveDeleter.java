package util.Deleter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class recursiveDeleter
{
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
                for (Object fileObj : fileList)
                {
                    File file = (File) fileObj;
                    delete(file.toPath());
                }
            }
        }
    }
}
