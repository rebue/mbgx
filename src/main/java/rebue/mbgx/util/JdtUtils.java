package rebue.mbgx.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;
import rebue.wheel.serialization.xml.XmlUtils;

@Slf4j
public class JdtUtils {

    /**
     * 格式化源码
     *
     * @param sourceCode 源代码
     * 
     * @return 格式化后的代码
     */
    public static String format(final String sourceCode) {
        // 将解析结果存储在Map中
        final Map<String, Object> options = new LinkedHashMap<>();
        try {
            final org.dom4j.Document document    = XmlUtils.getDocument(GoogleJavaFormatUtils.class.getResourceAsStream("/conf/java-code-format-options.xml"));
            final List<Node>         settingList = document.selectNodes("/profiles/profile/setting");
            for (final Node setting : settingList) {
                final Element element = (Element) setting;
                options.put(element.attributeValue("id"), element.attributeValue("value"));
            }
        } catch (DocumentException | SAXException e) {
            final String msg = "读取格式化选项的配置文件失败";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);

        final TextEdit      textEdit      = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, sourceCode, 0, sourceCode.length(), 0, null);
        final IDocument     doc           = new Document(sourceCode);
        try {
            textEdit.apply(doc);
            return doc.get();
        } catch (final MalformedTreeException | BadLocationException e) {
            final String msg = "格式化代码失败";
            log.error(msg + ":" + sourceCode, e);
            throw new RuntimeException(msg);
        }
    }

}
