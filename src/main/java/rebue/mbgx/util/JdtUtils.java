package rebue.mbgx.util;

import lombok.SneakyThrows;
import org.dom4j.Element;
import org.dom4j.Node;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import rebue.wheel.serialization.xml.XmlUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdtUtils {

    /**
     * 格式化源码
     *
     * @param sourceCode        源代码
     * @param formatOptionsFile 格式化选项配置文件
     * @return 格式化后的代码
     */
    @SneakyThrows
    public static String format(final String sourceCode, String formatOptionsFile) {
        // 将解析结果存储在Map中
        final Map<String, Object> options     = new LinkedHashMap<>();
        org.dom4j.Document        document    = parseJavaFormatFile(formatOptionsFile);
        final List<Node>          settingList = document.selectNodes("/profiles/profile/setting");
        for (final Node setting : settingList) {
            final Element element = (Element) setting;
            options.put(element.attributeValue("id"), element.attributeValue("value"));
        }

        final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);
        final TextEdit      textEdit      = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, sourceCode, 0, sourceCode.length(), 0, null);
        final IDocument     doc           = new Document(sourceCode);
        textEdit.apply(doc);
        return doc.get();
    }

    private static org.dom4j.Document parseJavaFormatFile(String formatOptionsFile) throws Exception {
        return XmlUtils.getDocument(JdtUtils.class.getResourceAsStream(formatOptionsFile));
    }

}
