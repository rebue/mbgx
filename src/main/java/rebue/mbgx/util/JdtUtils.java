package rebue.mbgx.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import rebue.wheel.core.ResourcesWrapper;
import rebue.wheel.serialization.xml.XmlUtils;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class JdtUtils {

    /**
     * 格式化源码
     *
     * @param sourceCode 源代码
     * @return 格式化后的代码
     */
    @SneakyThrows
    public static String format(final String sourceCode)
    {
        // 将解析结果存储在Map中
        final Map<String, Object> options = new LinkedHashMap<>();
        org.dom4j.Document document = parseJavaFormatFile();
        final List<Node> settingList = document.selectNodes("/profiles/profile/setting");
        for (final Node setting : settingList) {
            final Element element = (Element) setting;
            options.put(element.attributeValue("id"), element.attributeValue("value"));
        }

        final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);
        final TextEdit textEdit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, sourceCode, 0, sourceCode.length(), 0, null);
        final IDocument doc = new Document(sourceCode);
        textEdit.apply(doc);
        return doc.get();
    }

    private static org.dom4j.Document parseJavaFormatFile() throws Exception
    {
        if (File.separator.equals("\\")) {
            String xmlStr = ResourcesWrapper.fileStr("/conf/java-code-format-options.xml", JdtUtils.class);
            return DocumentHelper.parseText(xmlStr);
        } else {
            return XmlUtils.getDocument(JdtUtils.class.getResourceAsStream("/conf/java-code-format-options.xml"));
        }
    }

}
