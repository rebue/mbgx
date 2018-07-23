package mbgx;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Test;

import rebue.mbgx.util.MergeJavaFileUtil;

public class JavaParseTester {
    @Test
    public void Test01() throws IOException {
        String newFileSource = getStringFromFile(getProjectPath() + "/src/test/resources/conf/Hello.btl");
        String existingFileFullPath = getProjectPath() + "/src/test/java/mbgx/Hello.java";
        System.out
                .println(MergeJavaFileUtil.merge(newFileSource, existingFileFullPath, new String[] { "@ibatorgenerated", "@abatorgenerated", "@mbggenerated", "@mbg.generated" }));
    }

    /**
     * 得到项目的绝对路径
     */
    public static String getProjectPath() {
        return System.getProperty("user.dir");
    }

    public static String getStringFromFile(String filePath) throws IOException {
        return new String(getBytesFromFile(filePath), "utf-8");
    }

    public static byte[] getBytesFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        try (DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {
            dataInputStream.readFully(data);
        }
        return data;
    }
}
