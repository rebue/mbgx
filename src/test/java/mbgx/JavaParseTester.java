package mbgx;

import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rebue.mbgx.co.TagsCo;
import rebue.mbgx.util.MergeJavaFileUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JavaParseTester {
    private final static Logger _log = LoggerFactory.getLogger(JavaParseTester.class);

    /**
     * 得到项目的绝对路径
     */
    public static String getProjectPath() {
        return System.getProperty("user.dir");
    }

    public static String getStringFromFile(final String filePath) throws IOException {
        return new String(getBytesFromFile(filePath), StandardCharsets.UTF_8);
    }

    public static byte[] getBytesFromFile(final String filePath) throws IOException {
        final File            file            = new File(filePath);
        final FileInputStream fileInputStream = new FileInputStream(file);
        final byte[]          data            = new byte[(int) file.length()];
        try (DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {
            dataInputStream.readFully(data);
        }
        return data;
    }

    /**
     * 通过conf/Hello.btl模板文件，与Hello.java文件内容合并，打印出合并后的内容
     */
    @Test
    public void Test01() throws IOException {
        // 初始化代码
        final ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader();
        final Configuration           cfg            = Configuration.defaultConfiguration();
        final GroupTemplate           gt             = new GroupTemplate(resourceLoader, cfg);
        // 获取模板
        final Template t = gt.getTemplate("Hello.btl");
        // t.binding("name", "beetl");
        // 渲染结果
        final String newFileSource = t.render();

        final String existingFileFullPath = getProjectPath() + "/src/test/java/mbgx/Hello.java";
        final String mergeText = MergeJavaFileUtils.merge(newFileSource, existingFileFullPath, TagsCo.autoGenTags, TagsCo.removedMemberTags,
                TagsCo.dontOverWriteFileTags, TagsCo.dontOverWriteAnnotationTags, TagsCo.dontOverWriteExtendsTags, TagsCo.dontOverWriteImplementsTags);
        _log.debug(mergeText);
    }
}
