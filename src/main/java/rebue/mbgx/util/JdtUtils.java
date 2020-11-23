package rebue.mbgx.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;
import rebue.wheel.XmlUtils;

@Slf4j
public class JdtUtils {

    /**
     * 移除没有用的import，并返回优化处理后的代码
     *
     * @param sourceCode 源代码内容
     * 
     * @return 优化处理后的代码内容
     */
    public static String organizeImports(final String filePath) {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IPath path = Path.fromOSString(filePath);
        final IFile file = workspace.getRoot().getFileForLocation(path);
        final ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(file);

        try {
            compilationUnit.becomeWorkingCopy(null);
            final NullProgressMonitor nullProgressMonitor = new NullProgressMonitor();
            final CompilationUnit astRoot = compilationUnit.reconcile(AST.JLS14, false, null, nullProgressMonitor);
            final OrganizeImportsOperation organizeImportsOperation = new OrganizeImportsOperation(compilationUnit, astRoot,
                    true, true, true, null);
            final TextEdit textEdit = organizeImportsOperation.createTextEdit(nullProgressMonitor);
            JavaModelUtil.applyEdit(compilationUnit, textEdit, true, nullProgressMonitor);
            compilationUnit.commitWorkingCopy(true, nullProgressMonitor);
            compilationUnit.save(nullProgressMonitor, true);
        } catch (final CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

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
            final org.dom4j.Document document = XmlUtils.getDocument(JdtUtils.class.getResourceAsStream("/conf/java-code-format-options.xml"));
            final List<Node> settingList = document.selectNodes("/profiles/profile/setting");
            for (final Node setting : settingList) {
                final Element element = (Element) setting;
                options.put(element.attributeValue("id"), element.attributeValue("value"));
            }
        } catch (DocumentException | SAXException e1) {
            e1.printStackTrace();
        }

        final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);

        final TextEdit textEdit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, sourceCode, 0, sourceCode.length(), 0, null);
        final IDocument doc = new Document(sourceCode);
        try {
            textEdit.apply(doc);
            return doc.get();
        } catch (final MalformedTreeException e) {
            e.printStackTrace();
            return null;
        } catch (final BadLocationException e) {
            e.printStackTrace();
            return null;
        }
    }

}
