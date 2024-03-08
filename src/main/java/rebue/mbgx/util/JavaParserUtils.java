package rebue.mbgx.util;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class JavaParserUtils {

    /**
     * 移除没有用的import，并返回优化处理后的代码
     *
     * @param sourceCode 源代码内容
     * @return 优化处理后的代码内容
     */
    public static void removeUnusedImports(final CompilationUnit compilationUnit) {
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
            if (sName.charAt(0) >= 'A' && sName.charAt(0) <= 'Z') {
                classNames.add(sName);
            }
        }
        log.debug(classNames.toString());
        OUTLOOP:
        for (final ImportDeclaration oldImport : oldImports) {
            if (oldImport.isAsterisk()) {
                log.info("带*号的import，无法判断，直接添加");
                newImports.add(oldImport);
                continue;
            }

            if (oldImport.isStatic()) {
                log.info("静态的import，无法判断，直接添加");
                newImports.add(oldImport);
                continue;
            }

            for (final String className : classNames) {
                if (className.equals(oldImport.getName().getIdentifier())) {
                    newImports.add(oldImport);
                    continue OUTLOOP;
                }
            }
        }
    }

    /**
     * 移除没有用的import，并返回优化处理后的代码
     *
     * @param sourceCode 源代码内容
     * @return 优化处理后的代码内容
     */
    public static String removeUnusedImports(final String sourceCode) {
        final CompilationUnit compilationUnit = StaticJavaParser.parse(sourceCode);
        LexicalPreservingPrinter.setup(compilationUnit);
        removeUnusedImports(compilationUnit);
        return print(compilationUnit);
    }

    public static String print(final CompilationUnit compilationUnit) {
        return LexicalPreservingPrinter.print(compilationUnit);
    }

    /**
     * 格式化源码
     *
     * @param sourceCode 源代码
     * @return 格式化后的代码
     */
    public static String format(final String sourceCode) {
        final PrettyPrinterConfiguration prettyPrinterConfiguration = new PrettyPrinterConfiguration();
        prettyPrinterConfiguration.setOrderImports(true); // 排序imports
        return StaticJavaParser.parse(sourceCode).toString(prettyPrinterConfiguration);
    }

}
