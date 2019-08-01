package rebue.mbgx.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
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
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescriptionElement;

public class MergeJavaFileUtils {
    private final static Logger _log = LoggerFactory.getLogger(MergeJavaFileUtils.class);

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
    public static String merge(final String newFileSource, final String existingFileFullPath, final String[] javadocTags) throws FileNotFoundException {
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
    public static String merge(final String newFileSource, final File existingFile, final String[] javadocTags) throws FileNotFoundException {
        _log.info("将已存在的Java文件中的手工添加的部分合并进新模板的Java代码中: 已存在的文件-{}", existingFile.getAbsolutePath());
        final CompilationUnit newCompilationUnit = StaticJavaParser.parse(newFileSource);
        final CompilationUnit existingCompilationUnit = StaticJavaParser.parse(existingFile);
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
    private static String mergeCompilationUnit(final CompilationUnit newCompilationUnit, final CompilationUnit existingCompilationUnit, final String[] javadocTags) {
        // 是否替换代码开头的javadoc注释
        existingCompilationUnit.getComment().ifPresent(comment -> {
            comment.ifJavadocComment(javadocComment -> {
                if (!hasTag(javadocComment, javadocTags)) {
                    newCompilationUnit.setComment(comment);
                }
            });
        });

        // 类和接口
        final List<ClassOrInterfaceDeclaration> classOrInterfaces = existingCompilationUnit.findAll(ClassOrInterfaceDeclaration.class);
        for (final ClassOrInterfaceDeclaration existingClassOrInterface : classOrInterfaces) {
            // 已存在的类或接口的名称
            final String existingClassOrInterfaceName = existingClassOrInterface.getNameAsString();
            // 根据已存在的类或接口获取新的类或接口
            final Optional<ClassOrInterfaceDeclaration> newClassOrInterfaceOptional = existingClassOrInterface.isInterface()
                    ? newCompilationUnit.getInterfaceByName(existingClassOrInterfaceName)
                    : newCompilationUnit.getClassByName(existingClassOrInterfaceName);

            // 如果新代码没有此类或接口，则添加此类或接口
            if (!newClassOrInterfaceOptional.isPresent()) {
                newCompilationUnit.addType(existingClassOrInterface);
            } else {
                // 否则说明新代码有此类或接口，继续往下判断
                final ClassOrInterfaceDeclaration newClassOrInterface = newClassOrInterfaceOptional.get();
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
                final NodeList<BodyDeclaration<?>> existingMembers = existingClassOrInterface.getMembers();
                for (final BodyDeclaration<?> existingMember : existingMembers) {
                    // 是否替换或添加此成员
                    final Optional<Comment> comment = existingMember.getComment();
                    // 如果有javadoc注释且包含指定注解，则不替换或添加此成员
                    if (comment.isPresent() && comment.get() instanceof JavadocComment) {
                        final JavadocComment javadocComment = (JavadocComment) comment.get();
                        if (hasTag(javadocComment, javadocTags)) {
                            continue;
                        }
                    }
                    // 替换或添加此成员
                    // 如果是字段
                    if (existingMember.isFieldDeclaration()) {
                        // 已存在的字段
                        final FieldDeclaration existingField = existingMember.asFieldDeclaration();
                        // 获取字段的名称
                        final String existingFieldName = getFieldName(existingField);
                        // 获取字段的类型
                        final String existingFieldType = getFieldType(existingField);
                        // 新代码中的字段
                        final Optional<FieldDeclaration> newFieldOptional = newClassOrInterface.getFieldByName(existingFieldName);
                        // 如果在新代码中已存在，那么先删除
                        newFieldOptional.ifPresent(field -> {
                            field.remove();
                        });
                        // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
                        final FieldDeclaration newField = newClassOrInterface.addField(existingFieldType, existingFieldName);
                        newClassOrInterface.replace(newField, existingMember);
                    }
                    // 如果是构造方法
                    else if (existingMember.isConstructorDeclaration()) {
                        // 获取已存在的构造方法的参数类型
                        final ConstructorDeclaration existConstructor = existingMember.asConstructorDeclaration();
                        final List<String> paramTypeList = new LinkedList<>();
                        for (final Parameter parameter : existConstructor.getParameters()) {
                            paramTypeList.add(parameter.getTypeAsString());
                        }
                        final String[] paramTypes = paramTypeList.stream().toArray(String[]::new);
                        final Optional<ConstructorDeclaration> newConstructorOptional = newClassOrInterface.getConstructorByParameterTypes(paramTypes);
                        // 如果在新代码中已存在，那么先删除
                        newConstructorOptional.ifPresent(constructor -> {
                            constructor.remove();
                        });
                        // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
                        final ConstructorDeclaration newConstructor = newClassOrInterface.addConstructor();
                        newClassOrInterface.replace(newConstructor, existingMember);
                    }
                    // 如果是方法
                    else if (existingMember.isMethodDeclaration()) {
                        // 已存在的方法
                        final MethodDeclaration existingMethod = existingMember.asMethodDeclaration();
                        // 获取方法的名称
                        final String existingMethodName = existingMethod.getNameAsString();
                        // 如果有javadoc注释且包含@mbg.overrideByMethodName，则只按方法名称查找并替换新代码中的方法
                        if (hasTag(existingMethod, new String[] { "@mbg.overrideByMethodName" })) {
                            // 新代码中的方法
                            final List<MethodDeclaration> newMethods = newClassOrInterface.getMethodsByName(existingMethodName);
                            // 如果在新代码中已存在，那么先删除
                            if (!newMethods.isEmpty()) {
                                // 删除所有找到的方法
                                for (final MethodDeclaration newMethod : newMethods) {
                                    newMethod.remove();
                                }
                            }
                        } else {
                            // 新代码中的方法
                            final List<MethodDeclaration> newMethods = newClassOrInterface.getMethodsBySignature(existingMethodName,
                                    paramTypeToStrings(existingMethod.getSignature().getParameterTypes()));
                            // 如果在新代码中已存在，那么先删除
                            if (!newMethods.isEmpty()) {
                                // 删除所有找到的方法
                                for (final MethodDeclaration newMethod : newMethods) {
                                    newMethod.remove();
                                }
                            }
                        }
                        // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
                        final MethodDeclaration newMethod = newClassOrInterface.addMethod(existingMethodName);
                        newClassOrInterface.replace(newMethod, existingMember);
                    }
                }
            }
        }

        // 合并imports
        final NodeList<ImportDeclaration> existingImports = existingCompilationUnit.getImports();
        final NodeList<ImportDeclaration> newImports = newCompilationUnit.getImports();
        OUTLOOP: for (final ImportDeclaration existingImport : existingImports) {
            for (final ImportDeclaration newImport : newImports) {
                if (newImport.getName().equals(existingImport.getName())) {
                    continue OUTLOOP;
                }
            }
            newImports.add(existingImport);
        }

        // 移除没有用的import，并返回格式化后的源代码
        return JavaSourceUtils.removeUnusedImports(newCompilationUnit.toString());
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
    private static boolean hasTag(final Node node, final String[] tags) {
        final Optional<Comment> comment = node.getComment();
        if (comment.isPresent() && comment.get() instanceof JavadocComment) {
            final JavadocComment javadocComment = (JavadocComment) comment.get();
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
    private static boolean hasTag(final JavadocComment javadocComment, final String[] tags) {
        final Javadoc javadoc = javadocComment.parse();
        final List<JavadocBlockTag> blockTags = javadoc.getBlockTags();
        for (final String tag : tags) {
            for (final JavadocBlockTag javadocBlockTag : blockTags) {
                if (javadocBlockTag.getTagName().equals(tag.substring(1))) {
                    return true;
                }
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
    private static void removeTagMembers(final JavadocComment javadocComment, final ClassOrInterfaceDeclaration classOrInterface) {
        final Javadoc javadoc = javadocComment.parse();
        final List<JavadocBlockTag> blockTags = javadoc.getBlockTags();
        // 查找标记的成员
        for (final JavadocBlockTag javadocBlockTag : blockTags) {
            if (javadocBlockTag.getTagName().equals("mbg.removeField")) {
                for (final JavadocDescriptionElement item : javadocBlockTag.getContent().getElements()) {
                    // 要删除的字段
                    final String fieldName = item.toText();

                    // 类或接口中的字段
                    final Optional<FieldDeclaration> fieldOptional = classOrInterface.getFieldByName(fieldName);
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
    private static String getFieldName(final FieldDeclaration fieldDeclaration) {
        final List<Node> childNodes = fieldDeclaration.getChildNodes();
        for (final Node node : childNodes) {
            if (node instanceof VariableDeclarator) {
                return ((VariableDeclarator) node).getNameAsString();
            }
        }
        return null;
    }

    /**
     * 获取字段类型
     */
    private static String getFieldType(final FieldDeclaration fieldDeclaration) {
        final List<Node> childNodes = fieldDeclaration.getChildNodes();
        for (final Node node : childNodes) {
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
    private static String[] paramTypeToStrings(final List<Type> paramTypes) {
        return paramTypes.stream().map(paramType -> paramType.asString()).toArray(String[]::new);
    }

}
