package rebue.mbgx.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

public class JavaSourceUtils {
    private final static Logger _log = LoggerFactory.getLogger(JavaSourceUtils.class);

    /**
     * 移除没有用的import，并返回格式化后的源代码
     * 
     * @param sourceCode
     *            源代码内容
     * @return 优化处理后的代码内容
     */
    public static String removeUnusedImports(final String sourceCode) {
        final CompilationUnit compilationUnit = JavaParser.parse(sourceCode);

        // 先清空imports，避免查询节点的时候查到就不能判断是否使用过了
        final NodeList<ImportDeclaration> oldImports = compilationUnit.getImports();
        final NodeList<ImportDeclaration> newImports = new NodeList<>();
        compilationUnit.setImports(newImports);

        final Set<String> classNames = new HashSet<>();
        // 获取类
        final List<SimpleName> simpleNames = compilationUnit.findAll(SimpleName.class);
        for (final SimpleName simpleName : simpleNames) {
            final String sName = simpleName.getIdentifier();
            if (sName.charAt(0) >= 'A' && sName.charAt(0) <= 'Z') {
                classNames.add(sName);
            }
        }
        // 获取注解
        final List<Name> names = compilationUnit.findAll(Name.class);
        for (final Name name : names) {
            final String sName = name.getIdentifier();
            if (sName.equals("PageInfo")) {
                System.out.println(sName);
            }
            if (sName.charAt(0) >= 'A' && sName.charAt(0) <= 'Z') {
                classNames.add(sName);
            }
        }
        _log.debug(classNames.toString());
        OUTLOOP: for (final ImportDeclaration importDeclaration : oldImports) {
            for (final String className : classNames) {
                if (className.equals(importDeclaration.getName().getIdentifier())) {
                    newImports.add(importDeclaration);
                    continue OUTLOOP;
                }
            }
        }

        final PrettyPrinterConfiguration prettyPrinterConfiguration = new PrettyPrinterConfiguration();
        prettyPrinterConfiguration.setOrderImports(true);   // 排序imports
        return compilationUnit.toString(prettyPrinterConfiguration);
    }
}
