package rebue.mbgx.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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
import com.github.javaparser.javadoc.description.JavadocDescriptionElement;
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
        List<ClassOrInterfaceDeclaration> classOrInterfaces = existingCompilationUnit.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration existingClassOrInterface : classOrInterfaces) {
            // 已存在的类或接口的名称
            String existingClassOrInterfaceName = existingClassOrInterface.getNameAsString();
            // 根据已存在的类或接口获取新的类或接口
            Optional<ClassOrInterfaceDeclaration> newClassOrInterfaceOptional = existingClassOrInterface.isInterface()
                    ? newCompilationUnit.getInterfaceByName(existingClassOrInterfaceName)
                    : newCompilationUnit.getClassByName(existingClassOrInterfaceName);

            // 如果新代码没有此类或接口，则添加此类或接口
            if (!newClassOrInterfaceOptional.isPresent()) {
                newCompilationUnit.addType(existingClassOrInterface);
            } else {
                // 否则说明新代码有此类或接口，继续往下判断
                ClassOrInterfaceDeclaration newClassOrInterface = newClassOrInterfaceOptional.get();
                // 是否替换类或接口的javadoc注释
                existingClassOrInterface.getComment().ifPresent(comment -> {
                    comment.ifJavadocComment(javadocComment -> {
                        // 在已存在的类或接口的注释中，如果没有包含自动生成注解，则新代码使用已存在代码的注释
                        if (!hasTag(javadocComment, javadocTags)) {
                            // 替换注释
                            newClassOrInterface.setComment(comment);
                        }
                        // 在新代码的类或接口中，删除标记有@mbg.removeField的成员
                        removeTagMembers(javadocComment, newClassOrInterface);
                    });
                });

                // 是否替换或添加类或接口的成员
                NodeList<BodyDeclaration<?>> existingMembers = existingClassOrInterface.getMembers();
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
                        FieldDeclaration existingField = existingMember.asFieldDeclaration();
                        // 获取字段的名称
                        String existingFieldName = getFieldName(existingField);
                        // 获取字段的类型
                        String existingFieldType = getFieldType(existingField);
                        // 新代码中的字段
                        Optional<FieldDeclaration> newFieldOptional = newClassOrInterface.getFieldByName(existingFieldName);
                        // 如果在新代码中已存在，那么先删除
                        newFieldOptional.ifPresent(field -> {
                            field.remove();
                        });
                        // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
                        FieldDeclaration newField = newClassOrInterface.addField(existingFieldType, existingFieldName);
                        newClassOrInterface.replace(newField, existingMember);
                    }
                    // 如果是构造方法
                    else if (existingMember.isConstructorDeclaration()) {
                        // 获取已存在的构造方法的参数类型
                        ConstructorDeclaration existConstructor = existingMember.asConstructorDeclaration();
                        List<String> paramTypeList = new LinkedList<String>();
                        for (Parameter parameter : existConstructor.getParameters()) {
                            paramTypeList.add(parameter.getTypeAsString());
                        }
                        String[] paramTypes = paramTypeList.stream().toArray(String[]::new);
                        Optional<ConstructorDeclaration> newConstructorOptional = newClassOrInterface.getConstructorByParameterTypes(paramTypes);
                        // 如果在新代码中已存在，那么先删除
                        newConstructorOptional.ifPresent(constructor -> {
                            constructor.remove();
                        });
                        // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
                        ConstructorDeclaration newConstructor = newClassOrInterface.addConstructor();
                        newClassOrInterface.replace(newConstructor, existingMember);
                    }
                    // 如果是方法
                    else if (existingMember.isMethodDeclaration()) {
                        // 已存在的方法
                        MethodDeclaration existingMethod = existingMember.asMethodDeclaration();
                        // 获取方法的名称
                        String existingMethodName = existingMethod.getNameAsString();
                        // 如果有javadoc注释且包含@mbg.overrideByMethodName，则只按方法名称查找并替换新代码中的方法
                        if (hasTag(existingMethod, new String[] { "@mbg.overrideByMethodName" })) {
                            // 新代码中的方法
                            List<MethodDeclaration> newMethods = newClassOrInterface.getMethodsByName(existingMethodName);
                            // 如果在新代码中已存在，那么先删除
                            if (!newMethods.isEmpty()) {
                                // 删除所有找到的方法
                                for (MethodDeclaration newMethod : newMethods) {
                                    newMethod.remove();
                                }
                            }
                        } else {
                            // 新代码中的方法
                            List<MethodDeclaration> newMethods = newClassOrInterface.getMethodsBySignature(existingMethodName,
                                    paramTypeToStrings(existingMethod.getSignature().getParameterTypes()));
                            // 如果在新代码中已存在，那么先删除
                            if (!newMethods.isEmpty()) {
                                // 删除所有找到的方法
                                for (MethodDeclaration newMethod : newMethods) {
                                    newMethod.remove();
                                }
                            }
                        }
                        // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
                        MethodDeclaration newMethod = newClassOrInterface.addMethod(existingMethodName);
                        newClassOrInterface.replace(newMethod, existingMember);
                    }
                }
            }
        }

        // 合并imports
        NodeList<ImportDeclaration> existingImports = existingCompilationUnit.getImports();
        NodeList<ImportDeclaration> newImports = newCompilationUnit.getImports();
        OUTLOOP: for (ImportDeclaration existingImport : existingImports) {
            for (ImportDeclaration newImport : newImports) {
                if (newImport.getName().equals(existingImport.getName())) {
                    continue OUTLOOP;
                }
            }
            newImports.add(existingImport);
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
     * 在新代码的类或接口中，删除标记有@mbg.removeField的成员
     * 
     * @param javadocComment
     *            Javadoc的注释
     * @param classOrInterface
     *            查找的类或接口
     */
    private static void removeTagMembers(JavadocComment javadocComment, ClassOrInterfaceDeclaration classOrInterface) {
        Javadoc javadoc = javadocComment.parse();
        List<JavadocBlockTag> blockTags = javadoc.getBlockTags();
        // 查找标记的成员
        for (JavadocBlockTag javadocBlockTag : blockTags) {
            if (javadocBlockTag.getTagName().equals("mbg.removeField")) {
                for (JavadocDescriptionElement item : javadocBlockTag.getContent().getElements()) {
                    // 要删除的字段
                    String fieldName = item.toText();

                    // 类或接口中的字段
                    Optional<FieldDeclaration> fieldOptional = classOrInterface.getFieldByName(fieldName);
                    // 删除字段
                    fieldOptional.ifPresent(field -> {
                        field.remove();
                    });

                    // 删除get方法
                    List<MethodDeclaration> methods = classOrInterface.getMethodsByName("get" + StringUtils.capitalize(fieldName));
                    if (!methods.isEmpty()) {
                        methods.get(0).remove();
                    }

                    // 删除set方法
                    methods = classOrInterface.getMethodsByName("set" + StringUtils.capitalize(fieldName));
                    if (!methods.isEmpty()) {
                        methods.get(0).remove();
                    }
                }
            }
        }
    }

    /**
     * 获取字段名称
     */
    private static String getFieldName(FieldDeclaration fieldDeclaration) {
        List<Node> childNodes = fieldDeclaration.getChildNodes();
        for (Node node : childNodes) {
            if (node instanceof VariableDeclarator) {
                return ((VariableDeclarator) node).getNameAsString();
            }
        }
        return null;
    }

    /**
     * 获取字段类型
     */
    private static String getFieldType(FieldDeclaration fieldDeclaration) {
        List<Node> childNodes = fieldDeclaration.getChildNodes();
        for (Node node : childNodes) {
            if (node instanceof VariableDeclarator) {
                return ((VariableDeclarator) node).getTypeAsString();
            }
        }
        return null;
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
