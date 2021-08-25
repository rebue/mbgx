package rebue.mbgx.util;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Paths;

public class PathUtils {
    /**
     * 判断是否是绝对路径
     *
     * @param path 需要判断的路径
     * @return 是否是绝对路径，如果是false，则说明是相对路径
     */
    public final static boolean isAbsPath(String path)
    {
        return FileSystems.getDefault().getPath(path).isAbsolute();
    }

    public static String getProjectRoot(Class<?> clazz)
    {
        String path = clazz.getResource("/").getPath();
        if (File.separator.equals("\\") && path.startsWith("/")) {
            path = path.substring(1);
        }
        return Paths.get(path).getParent().getParent().getParent().toAbsolutePath().toString();
    }

}
