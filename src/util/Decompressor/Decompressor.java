package util.Decompressor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 通过命令行解压一个 Zip 文件
 *
 * @author soulike
 */
public class Decompressor
{
    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("参数数量错误");
        }
        else
        {
            Path path = Paths.get(args[0]);
            try
            {
                decompress(path, Paths.get("./"));
            }
            catch (IOException e)
            {
                System.out.println("解压失败");
                e.printStackTrace();
            }
        }
    }


    /**
     * 把一个Zip文件内容解压到指定目录
     *
     * @param zipFilePath 指向 Zip 文件的 Path 对象
     * @param dstPath     Zip 文件内容解压到的文件夹 Path 对象
     */
    public static void decompress(Path zipFilePath, Path dstPath) throws IOException
    {
        if (Files.notExists(dstPath))
        {
            Files.createDirectories(dstPath);
        }

        ZipFile zip = new ZipFile(zipFilePath.toFile());
        for (Enumeration entries = zip.entries(); entries.hasMoreElements(); )
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String zipEntryName = entry.getName();

            try (InputStream in = zip.getInputStream(entry))
            {
                Path outPath = Paths.get(dstPath.toString(), zipEntryName);

                //判断路径是否存在,不存在则创建文件路径
                if (Files.notExists(outPath.getParent()))
                {
                    Files.createDirectories(outPath.getParent());
                }
                if (Files.notExists(outPath))
                {
                    Files.createFile(outPath);
                }

                // 如果这个文件是文件夹，那在解压文件时就已经创建，不需要处理
                if (!outPath.toFile().isDirectory())
                {
                    try (OutputStream out = new BufferedOutputStream(new DataOutputStream(new FileOutputStream(outPath.toString()))))
                    {
                        byte[] buff = new byte[1024 * 1024];
                        int readBytes;
                        while ((readBytes = in.read(buff)) > 0)
                        {
                            out.write(buff, 0, readBytes);
                        }
                    }
                }
            }
        }
    }
}
