package rebue.mbgx.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

public class MergeJavaFileUtil {
    private final static Logger _log = LoggerFactory.getLogger(MergeJavaFileUtil.class);

    /**
     * 合并Java代码
     * 将已存在的Java文件中的手工添加的部分合并进新模板的Java代码
     * 
     * @param newFileSource
     *            新代码文件的内容
     * @param existingFileFullPath
     *            已存在的代码文件的全路径
     * @param javadocTags
     *            标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @return 合并后的新内容
     */
    public static String merge(String newFileSource, String existingFileFullPath, String[] javadocTags) throws FileNotFoundException {
        return merge(newFileSource, new File(existingFileFullPath), javadocTags);
    }

    /**
     * 合并Java代码
     * 将已存在的Java文件中的手工添加的部分合并进新模板的Java代码
     * 
     * @param newFileSource
     *            新代码文件的内容
     * @param existingFile
     *            已存在的代码文件
     * @param javadocTags
     *            标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @return 合并后的新内容
     */
    public static String merge(String newFileSource, File existingFile, String[] javadocTags) throws FileNotFoundException {
        _log.info("将已存在的Java文件中的手工添加的部分合并进新模板的Java代码中: 已存在的文件-{}", existingFile.getAbsolutePath());
        CompilationUnit newCompilationUnit = JavaParser.parse(newFileSource);
        CompilationUnit existingCompilationUnit = JavaParser.parse(existingFile);
        return mergeCompilationUnit(newCompilationUnit, existingCompilationUnit, javadocTags);
    }

    /**
     * 合并Java代码
     * 将已存在的Java文件中的手工添加的部分合并进新模板的Java代码
     * 
     * @param newCompilationUnit
     *            新代码的编译器
     * @param existingCompilationUnit
     *            已存在代码的编译器
     * @param javadocTags
     *            标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @return 合并后的内容
     */
    private static String mergeCompilationUnit(CompilationUnit newCompilationUnit, CompilationUnit existingCompilationUnit, String[] javadocTags) {
        // 是否替换代码开头的javadoc注释
        existingCompilationUnit.getComment().ifPresent(comment -> {
            comment.ifJavadocComment(javadocComment -> {
                if (!hasTag(javadocComment, javadocTags)) {
                    newCompilationUnit.setComment(comment);
                }
            });
        });

        // 类和接口
        List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations = existingCompilationUnit.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration existingClassOrInterfaceDeclaration : classOrInterfaceDeclarations) {
            // 已存在的类或接口的名称
            String existingClassOrInterfaceName = existingClassOrInterfaceDeclaration.getNameAsString();
            // 根据已存在的类或接口获取新的类或接口
            Optional<ClassOrInterfaceDeclaration> newClassOrInterfaceDeclarationOptional = existingClassOrInterfaceDeclaration.isInterface()
                    ? newCompilationUnit.getInterfaceByName(existingClassOrInterfaceName)
                    : newCompilationUnit.getClassByName(existingClassOrInterfaceName);

            // 如果新代码没有此类或接口，则添加此类或接口
            if (!newClassOrInterfaceDeclarationOptional.isPresent()) {
                newCompilationUnit.addType(existingClassOrInterfaceDeclaration);
            } else {
                ClassOrInterfaceDeclaration newClassOrInterfaceDeclaration = newClassOrInterfaceDeclarationOptional.get();
                // 否则说明新代码有此类或接口，继续往下判断
                // 是否替换类或接口的javadoc注释
                existingClassOrInterfaceDeclaration.getComment().ifPresent(comment -> {
                    comment.ifJavadocComment(javadocComment -> {
                        if (!hasTag(javadocComment, javadocTags)) {
                            // 替换注释
                            newClassOrInterfaceDeclaration.setComment(comment);
                        }
                    });
                });

                // 是否替换或添加类或接口的成员
                NodeList<BodyDeclaration<?>> existingMembers = existingClassOrInterfaceDeclaration.getMembers();
                for (BodyDeclaration<?> existingMember : existingMembers) {
                    // 是否替换或添加此成员
                    Optional<Comment> comment = existingMember.getComment();
                    // 如果有javadoc注释且包含指定注解，则不替换或添加此成员
                    if (comment.isPresent() && comment.get() instanceof JavadocComment) {
                        JavadocComment javadocComment = (JavadocComment) comment.get();
                        if (hasTag(javadocComment, javadocTags)) {
                            continue;
                        }
                    }
                    // 替换或添加此成员
                    // 如果是字段
                    if (existingMember.isFieldDeclaration()) {
                        // 已存在的字段
                        FieldDeclaration existingFieldDeclaration = existingMember.asFieldDeclaration();
                        // 获取字段的名称
                        String existingFieldType = ((VariableDeclarator) existingFieldDeclaration.getChildNodes().get(0)).getTypeAsString();
                        // 获取字段的名称
                        String existingFieldName = ((VariableDeclarator) existingFieldDeclaration.getChildNodes().get(0)).getNameAsString();
                        // 新代码中的字段
                        Optional<FieldDeclaration> newFieldOptional = newClassOrInterfaceDeclaration.getFieldByName(existingFieldName);
                        // 如果在新代码中已存在，那么先删除
                        newFieldOptional.ifPresent(fieldDeclaration -> {
                            fieldDeclaration.remove();
                        });
                        // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
                        FieldDeclaration newFieldDeclaration = newClassOrInterfaceDeclaration.addField(existingFieldType, existingFieldName);
                        newClassOrInterfaceDeclaration.replace(newFieldDeclaration, existingMember);
                    }
                    // 如果是构造方法
                    else if (existingMember.isConstructorDeclaration()) {
                        // 获取已存在的构造方法的参数类型
                        ConstructorDeclaration existConstructorDeclaration = existingMember.asConstructorDeclaration();
                        List<String> paramTypeList = new LinkedList<String>();
                        for (Parameter parameter : existConstructorDeclaration.getParameters()) {
                            paramTypeList.add(parameter.getTypeAsString());
                        }
                        String[] paramTypes = paramTypeList.stream().toArray(String[]::new);
                        Optional<ConstructorDeclaration> newConstructorOptional = newClassOrInterfaceDeclaration.getConstructorByParameterTypes(paramTypes);
                        // 如果在新代码中已存在，那么先删除
                        newConstructorOptional.ifPresent(constructorDeclaration -> {
                            constructorDeclaration.remove();
                        });
                        // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
                        ConstructorDeclaration newConstructorDeclaration = newClassOrInterfaceDeclaration.addConstructor();
                        newClassOrInterfaceDeclaration.replace(newConstructorDeclaration, existingMember);
                    }
                    // 如果是方法
                    else if (existingMember.isMethodDeclaration()) {
                        // 已存在的方法
                        MethodDeclaration existingMethodDeclaration = existingMember.asMethodDeclaration();
                        // 获取方法的名称
                        String existingMethodName = existingMethodDeclaration.getNameAsString();
                        // 如果有javadoc注释且包含@mbg.overrideByMethodName，则只按方法名称查找并替换新代码中的方法
                        if (hasTag(existingMethodDeclaration, new String[] { "@mbg.overrideByMethodName" })) {
                            // 新代码中的方法
                            List<MethodDeclaration> newMethodDeclarations = newClassOrInterfaceDeclaration.getMethodsByName(existingMethodName);
                            // 如果在新代码中已存在，那么先删除
                            if (!newMethodDeclarations.isEmpty()) {
                                // 删除所有找到的方法
                                for (MethodDeclaration newMethodDeclaration : newMethodDeclarations) {
                                    newMethodDeclaration.remove();
                                }
                            }
                        } else {
                            // 新代码中的方法
                            List<MethodDeclaration> newMethodDeclarations = newClassOrInterfaceDeclaration.getMethodsBySignature(existingMethodName,
                                    paramTypeToStrings(existingMethodDeclaration.getSignature().getParameterTypes()));
                            // 如果在新代码中已存在，那么先删除
                            if (!newMethodDeclarations.isEmpty()) {
                                // 删除所有找到的方法
                                for (MethodDeclaration newMethodDeclaration : newMethodDeclarations) {
                                    newMethodDeclaration.remove();
                                }
                            }
                        }
                        // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
                        MethodDeclaration newMethodDeclaration = newClassOrInterfaceDeclaration.addMethod(existingMethodName);
                        newClassOrInterfaceDeclaration.replace(newMethodDeclaration, existingMember);
                    }
                }
            }
        }

        // 合并imports
        NodeList<ImportDeclaration> existingImports = existingCompilationUnit.getImports();
        NodeList<ImportDeclaration> newImports = newCompilationUnit.getImports();
        OUTLOOP: for (ImportDeclaration existingImportDeclaration : existingImports) {
            for (ImportDeclaration newImportDeclaration : newImports) {
                if (newImportDeclaration.getName().equals(existingImportDeclaration.getName())) {
                    continue OUTLOOP;
                }
            }
            newImports.add(existingImportDeclaration);
        }

        // 移除没有用的import，并返回格式化后的源代码
        return removeUnusedImports(newCompilationUnit.toString());
    }

    /**
     * 判断节点是否有Javadoc注释，且里面包含指定的注解
     * 
     * @param node
     *            节点
     * @param tags
     *            判断是否包含的注解
     * @return 是否有Javadoc注释，且里面包含指定的注解
     */
    private static boolean hasTag(Node node, String[] tags) {
        Optional<Comment> comment = node.getComment();
        if (comment.isPresent() && comment.get() instanceof JavadocComment) {
            JavadocComment javadocComment = (JavadocComment) comment.get();
            return hasTag(javadocComment, tags);
        }
        return false;
    }

    /**
     * 判断Javadoc的注释中有没有包含指定的注解
     * 
     * @param javadocComment
     *            Javadoc的注释
     * @param tags
     *            判断是否包含的注解
     * @return 是否包含
     */
    private static boolean hasTag(JavadocComment javadocComment, String[] tags) {
        Javadoc javadoc = javadocComment.parse();
        List<JavadocBlockTag> blockTags = javadoc.getBlockTags();
        for (String tag : tags) {
            for (JavadocBlockTag javadocBlockTag : blockTags) {
                if (javadocBlockTag.getTagName().equals(tag.substring(1)))
                    return true;
            }
        }
        return false;
    }

    /**
     * 将参数列表转成String[]
     * 
     * @param paramTypes
     *            参数列表
     * @return String[]
     */
    private static String[] paramTypeToStrings(List<Type> paramTypes) {
        return paramTypes.stream().map(paramType -> paramType.asString()).toArray(String[]::new);
    }

    /**
     * 移除没有用的import，并返回格式化后的源代码
     * 
     * @param sourceCode
     *            源代码内容
     * @return 优化处理后的代码内容
     */
    private static String removeUnusedImports(String sourceCode) {
        CompilationUnit compilationUnit = JavaParser.parse(sourceCode);

        // 先清空imports，避免查询节点的时候查到就不能判断是否使用过了
        NodeList<ImportDeclaration> oldImports = compilationUnit.getImports();
        NodeList<ImportDeclaration> newImports = new NodeList<>();
        compilationUnit.setImports(newImports);

        Set<String> classNames = new HashSet<>();
        // 获取类
        List<SimpleName> simpleNames = compilationUnit.findAll(SimpleName.class);
        for (SimpleName simpleName : simpleNames) {
            String sName = simpleName.getIdentifier();
            if (sName.charAt(0) >= 'A' && sName.charAt(0) <= 'Z')
                classNames.add(sName);
        }
        // 获取注解
        List<Name> names = compilationUnit.findAll(Name.class);
        for (Name name : names) {
            String sName = name.getIdentifier();
            if (sName.equals("PageInfo"))
                System.out.println(sName);
            if (sName.charAt(0) >= 'A' && sName.charAt(0) <= 'Z')
                classNames.add(sName);
        }
        _log.debug(classNames.toString());
        OUTLOOP: for (ImportDeclaration importDeclaration : oldImports) {
            for (String className : classNames) {
                if (className.equals(importDeclaration.getName().getIdentifier())) {
                    newImports.add(importDeclaration);
                    continue OUTLOOP;
                }
            }
        }

        PrettyPrinterConfiguration prettyPrinterConfiguration = new PrettyPrinterConfiguration();
        prettyPrinterConfiguration.setOrderImports(true);   // 排序imports
        return compilationUnit.toString(prettyPrinterConfiguration);
    }
}
