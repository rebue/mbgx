package rebue.mbgx.util;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

public class GoogleJavaFormatUtils {

    /**
     * 格式化源码
     *
     * @param sourceCode 源代码
     * @return 格式化后的代码
     */
    public static String format(final String sourceCode) {
        try {
            return new Formatter().formatSource(sourceCode);
        } catch (final FormatterException e) {
            throw new RuntimeException("格式化出错", e);
        }
    }

}
