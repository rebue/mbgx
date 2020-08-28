package rebue.mbgx.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MergeJavaFileUtils {

    /**
     * 合并Java代码 将已存在的Java文件中的手工添加的部分合并进新模板的Java代码
     *
     * @param newFileSource              新代码文件的内容
     * @param existingFileFullPath       已存在的代码文件的全路径
     * @param javadocTagsOfAutoGen       标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @param javadocTagsOfRemovedMember 标识要删除成员的注解(将此数组中的任意注解加上成员名称放在类或接口的Javadoc注释中表示此成员不要自动生成)
     * @return 合并后的新内容
     */
    public static String merge(final String newFileSource, final String existingFileFullPath, final String[] javadocTagsOfAutoGen, final String[] javadocTagsOfRemovedMember)
            throws FileNotFoundException {
        return merge(newFileSource, new File(existingFileFullPath), javadocTagsOfAutoGen, javadocTagsOfRemovedMember);
    }

    /**
     * 合并Java代码
     *
     * @param newFileSource              新代码文件的内容
     * @param existingFile               已存在的代码文件
     * @param javadocTagsOfAutoGen       标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @param javadocTagsOfRemovedMember 标识要删除成员的注解(将此数组中的任意注解加上成员名称放在类或接口的Javadoc注释中表示此成员不要自动生成)
     * @return 合并后的新内容
     */
    public static String merge(final String newFileSource, final File existingFile, final String[] javadocTagsOfAutoGen,
            final String[] javadocTagsOfRemovedMember) throws FileNotFoundException {
        log.info("合并JAVA代码: 已存在的文件-{}", existingFile.getAbsolutePath());
        final CompilationUnit newCompilationUnit      = StaticJavaParser.parse(newFileSource);
        final CompilationUnit existingCompilationUnit = StaticJavaParser.parse(existingFile);
        return mergeCompilationUnit(newCompilationUnit, existingCompilationUnit, javadocTagsOfAutoGen, javadocTagsOfRemovedMember);
    }

    /**
     * 合并Java代码
     *
     * @param newCompilationUnit         新代码的编译器
     * @param oldCompilationUnit         已存在代码的编译器
     * @param javadocTagsOfAutoGen       标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @param javadocTagsOfRemovedMember 标识要删除成员的注解(将此数组中的任意注解加上成员名称放在类或接口的Javadoc注释中表示此成员不要自动生成)
     * @return 合并后的内容
     */
    private static String mergeCompilationUnit(final CompilationUnit newCompilationUnit, final CompilationUnit oldCompilationUnit, final String[] javadocTagsOfAutoGen,
            final String[] javadocTagsOfRemovedMember) {
        log.info("判断是否替换代码开头的注释");
        // 如果新代码有注释
        newCompilationUnit.getComment().ifPresent(newComment -> {
            final Optional<Comment> oldComment = oldCompilationUnit.getComment();
            // 如果旧代码中有注释，判断是否需要用新代码的注释来代替
            if (oldComment.isPresent()) {
                // 如果是JavaDoc注释，且含有自动生成的注解，才用新代码的注释
                if (oldComment.get().isJavadocComment() && hasTag(oldComment.get(), javadocTagsOfAutoGen)) {
                    oldCompilationUnit.setComment(newComment);
                }
            }
            // 如果旧代码中有没有注释，直接使用新代码的注释
            else {
                oldCompilationUnit.setComment(newComment);
            }
        });

        log.info("使用新的PackageDeclaration");
        newCompilationUnit.getPackageDeclaration().ifPresent(newPpackageDeclaration -> {
            oldCompilationUnit.setPackageDeclaration(newPpackageDeclaration);
        });

        log.info("合并imports");
        oldCompilationUnit.getImports().addAll(newCompilationUnit.getImports());

        log.info("合并类或接口");
        final List<ClassOrInterfaceDeclaration> classOrInterfaces = newCompilationUnit.findAll(ClassOrInterfaceDeclaration.class);
        for (final ClassOrInterfaceDeclaration newClassOrInterface : classOrInterfaces) {
            // 新的类或接口的名称
            final String                                classOrInterfaceName        = newClassOrInterface.getNameAsString();
            // 根据新类或接口获取旧类或接口
            final Optional<ClassOrInterfaceDeclaration> oldClassOrInterfaceOptional = newClassOrInterface.isInterface()
                    ? oldCompilationUnit.getInterfaceByName(classOrInterfaceName)
                    : oldCompilationUnit.getClassByName(classOrInterfaceName);

            // 如果旧代码没有此类或接口，则添加此类或接口
            if (!oldClassOrInterfaceOptional.isPresent()) {
                log.info("添加新类或接口: {}", classOrInterfaceName);
                oldCompilationUnit.addType(newClassOrInterface);
                continue;
            }

            // 否则说明新代码有此类或接口，进行合并
            log.info("开始合并类或接口: {}", classOrInterfaceName);
            final ClassOrInterfaceDeclaration oldClassOrInterface = oldClassOrInterfaceOptional.get();

            log.info("合并类或接口的javadoc注释");
            // 如果新代码有注释
            newClassOrInterface.getComment().ifPresent(newComment -> {
                final Optional<Comment> oldCommentOptional = oldClassOrInterface.getComment();
                // 如果旧代码中有注释，判断是否需要用新代码的注释来代替
                if (oldCommentOptional.isPresent()) {
                    // 如果是JavaDoc注释，且含有自动生成的注解，才用新代码的注释
                    oldCommentOptional.get().ifJavadocComment(oldJavadocComment -> {
                        if (newComment.isJavadocComment() && hasTag(oldJavadocComment, javadocTagsOfAutoGen)) {
                            oldClassOrInterface.setComment(mergeJavadocComment(oldJavadocComment, newComment.asJavadocComment()));
                        }
                    });
                }
                // 如果旧代码中没有注释，直接使用新代码的注释
                else {
                    oldClassOrInterface.setComment(newComment);
                }
            });

            log.info("获取要移除的成员列表");
            final List<String> removedMembers = new LinkedList<>();
            oldClassOrInterface.getComment().ifPresent(comment -> {
                comment.ifJavadocComment(javadocComment -> {
                    final List<JavadocBlockTag> javadocTags = getTags(javadocComment);
                    for (final JavadocBlockTag javadocTag : javadocTags) {
                        for (final String tag : javadocTagsOfRemovedMember) {
                            if (javadocTag.getTagName().equals(tag.substring(1))) {
                                javadocTag.getName().ifPresent(name -> {
                                    removedMembers.add(name);
                                });
                                break;
                            }
                        }
                    }
                });
            });

            log.info("合并类或接口的注解");
            oldClassOrInterface.setAnnotations(newClassOrInterface.getAnnotations());

            log.info("合并类或接口的成员");
            final NodeList<BodyDeclaration<?>> newMembers = newClassOrInterface.getMembers();
            for (final BodyDeclaration<?> newMember : newMembers) {
//                // 是否替换或添加此成员
//                final Optional<Comment> oldCommentOptional = oldMember.getComment();
//                // 如果没有注释，或不是javadoc注释，或不包含自动生成注解，则不替换此成员
//                if (!oldCommentOptional.isPresent() || !oldCommentOptional.get().isJavadocComment() || !hasTag(oldCommentOptional.get().asJavadocComment(), javadocTagsOfAutoGen)) {
//                    continue;
//                }

                // 如果是字段
                if (newMember.isFieldDeclaration()) {
                    // 已存在的字段
                    final FieldDeclaration newField     = newMember.asFieldDeclaration();
                    // 获取字段的类型
                    final String           newFieldType = getFieldType(newField);
                    // 获取字段的名称
                    final String           newFieldName = getFieldName(newField);
                    log.info("当前成员是字段: {} {}", newFieldType, newFieldName);

                    if (removedMembers.contains(newFieldName)) {
                        log.info("此字段已在类或接口的javadoc注解中声明删除: {}", newFieldName);
                        continue;
                    }

                    // 旧代码中的字段
                    final Optional<FieldDeclaration> oldFieldOptional = oldClassOrInterface.getFieldByName(newFieldName);
                    if (!oldFieldOptional.isPresent()) {
                        log.info("此字段在旧代码中不存在，直接添加: {}", newFieldName);
                        oldClassOrInterface.addMember(newMember);
                        continue;
                    }

                    // 如果没有注释，或不是javadoc注释，或不包含自动生成注解，则不替换此成员
                    final Optional<Comment> oldCommentOptional = oldFieldOptional.get().getComment();
                    if (!oldCommentOptional.isPresent() || !oldCommentOptional.get().isJavadocComment()
                            || !hasTag(oldCommentOptional.get().asJavadocComment(), javadocTagsOfAutoGen)) {
                        continue;
                    }

                    // 替换字段
                    oldClassOrInterface.replace(oldFieldOptional.get(), newMember);
                }
                // 如果是构造方法
                else if (newMember.isConstructorDeclaration()) {
                    // 获取已存在的构造方法的参数类型
                    final ConstructorDeclaration existConstructor = newMember.asConstructorDeclaration();
                    final List<String>           paramTypeList    = new LinkedList<>();
                    for (final Parameter parameter : existConstructor.getParameters()) {
                        paramTypeList.add(parameter.getTypeAsString());
                    }
                    final String[]                         paramTypes             = paramTypeList.stream().toArray(String[]::new);
                    final Optional<ConstructorDeclaration> newConstructorOptional = newClassOrInterface.getConstructorByParameterTypes(paramTypes);
                    // 如果在新代码中已存在，那么先删除
                    newConstructorOptional.ifPresent(constructor -> {
                        constructor.remove();
                    });
                    // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
                    final ConstructorDeclaration newConstructor = newClassOrInterface.addConstructor();
                    newClassOrInterface.replace(newConstructor, newMember);
                }
                // 如果是方法
                else if (newMember.isMethodDeclaration()) {
                    // 已存在的方法
                    final MethodDeclaration existingMethod     = newMember.asMethodDeclaration();
                    // 获取方法的名称
                    final String            existingMethodName = existingMethod.getNameAsString();
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
                    newClassOrInterface.replace(newMethod, newMember);

                }
            }
        }

        // 移除没有用的import，并返回格式化后的源代码
        return JavaSourceUtils.removeUnusedImports(newCompilationUnit.toString());
    }

//    /**
//     * 合并Java代码 将已存在的Java文件中的手工添加的部分合并进新模板的Java代码
//     *
//     * @param newCompilationUnit      新代码的编译器
//     * @param existingCompilationUnit 已存在代码的编译器
//     * @param javadocTags             标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
//     * @return 合并后的内容
//     */
//    private static String mergeCompilationUnit(final CompilationUnit newCompilationUnit, final CompilationUnit existingCompilationUnit, final String[] javadocTags) {
//        // 是否替换代码开头的javadoc注释
//        existingCompilationUnit.getComment().ifPresent(comment -> {
//            comment.ifJavadocComment(javadocComment -> {
//                if (!hasTag(javadocComment, javadocTags)) {
//                    newCompilationUnit.setComment(comment);
//                }
//            });
//        });
//
//        // 类和接口
//        final List<ClassOrInterfaceDeclaration> classOrInterfaces = existingCompilationUnit.findAll(ClassOrInterfaceDeclaration.class);
//        for (final ClassOrInterfaceDeclaration existingClassOrInterface : classOrInterfaces) {
//            // 已存在的类或接口的名称
//            final String existingClassOrInterfaceName = existingClassOrInterface.getNameAsString();
//            // 根据已存在的类或接口获取新的类或接口
//            final Optional<ClassOrInterfaceDeclaration> newClassOrInterfaceOptional = existingClassOrInterface.isInterface()
//                    ? newCompilationUnit.getInterfaceByName(existingClassOrInterfaceName)
//                            : newCompilationUnit.getClassByName(existingClassOrInterfaceName);
//
//                    // 如果新代码没有此类或接口，则添加此类或接口
//                    if (!newClassOrInterfaceOptional.isPresent()) {
//                        newCompilationUnit.addType(existingClassOrInterface);
//                    } else {
//                        // 否则说明新代码有此类或接口，继续往下判断
//                        final ClassOrInterfaceDeclaration newClassOrInterface = newClassOrInterfaceOptional.get();
//                        // 是否替换类或接口的javadoc注释
//                        existingClassOrInterface.getComment().ifPresent(comment -> {
//                            comment.ifJavadocComment(javadocComment -> {
//                                // 在已存在的类或接口的注释中，如果没有包含自动生成注解，则新代码使用已存在代码的注释
//                                if (!hasTag(javadocComment, javadocTags)) {
//                                    // 替换注释
//                                    newClassOrInterface.setComment(comment);
//                                }
//                                // 在新代码的类或接口中，删除标记有@mbg.removeField的成员
//                                removeTagMembers(javadocComment, newClassOrInterface);
//                            });
//                        });
//
//                        // 是否替换或添加类或接口的成员
//                        final NodeList<BodyDeclaration<?>> existingMembers = existingClassOrInterface.getMembers();
//                        for (final BodyDeclaration<?> existingMember : existingMembers) {
//                            // 是否替换或添加此成员
//                            final Optional<Comment> comment = existingMember.getComment();
//                            // 如果有javadoc注释且包含指定注解，则不替换或添加此成员
//                            if (comment.isPresent() && comment.get() instanceof JavadocComment) {
//                                final JavadocComment javadocComment = (JavadocComment) comment.get();
//                                if (hasTag(javadocComment, javadocTags)) {
//                                    continue;
//                                }
//                            }
//                            // 替换或添加此成员
//                            // 如果是字段
//                            if (existingMember.isFieldDeclaration()) {
//                                // 已存在的字段
//                                final FieldDeclaration           existingField     = existingMember.asFieldDeclaration();
//                                // 获取字段的名称
//                                final String                     existingFieldName = getFieldName(existingField);
//                                // 获取字段的类型
//                                final String                     existingFieldType = getFieldType(existingField);
//                                // 新代码中的字段
//                                final Optional<FieldDeclaration> newFieldOptional  = newClassOrInterface.getFieldByName(existingFieldName);
//                                // 如果在新代码中已存在，那么先删除
//                                newFieldOptional.ifPresent(field -> {
//                                    field.remove();
//                                });
//                                // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
//                                final FieldDeclaration newField = newClassOrInterface.addField(existingFieldType, existingFieldName);
//                                newClassOrInterface.replace(newField, existingMember);
//                            }
//                            // 如果是构造方法
//                            else if (existingMember.isConstructorDeclaration()) {
//                                // 获取已存在的构造方法的参数类型
//                                final ConstructorDeclaration existConstructor = existingMember.asConstructorDeclaration();
//                                final List<String>           paramTypeList    = new LinkedList<>();
//                                for (final Parameter parameter : existConstructor.getParameters()) {
//                                    paramTypeList.add(parameter.getTypeAsString());
//                                }
//                                final String[]                         paramTypes             = paramTypeList.stream().toArray(String[]::new);
//                                final Optional<ConstructorDeclaration> newConstructorOptional = newClassOrInterface.getConstructorByParameterTypes(paramTypes);
//                                // 如果在新代码中已存在，那么先删除
//                                newConstructorOptional.ifPresent(constructor -> {
//                                    constructor.remove();
//                                });
//                                // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
//                                final ConstructorDeclaration newConstructor = newClassOrInterface.addConstructor();
//                                newClassOrInterface.replace(newConstructor, existingMember);
//                            }
//                            // 如果是方法
//                            else if (existingMember.isMethodDeclaration()) {
//                                // 已存在的方法
//                                final MethodDeclaration existingMethod     = existingMember.asMethodDeclaration();
//                                // 获取方法的名称
//                                final String            existingMethodName = existingMethod.getNameAsString();
//                                // 如果有javadoc注释且包含@mbg.overrideByMethodName，则只按方法名称查找并替换新代码中的方法
//                                if (hasTag(existingMethod, new String[] { "@mbg.overrideByMethodName" })) {
//                                    // 新代码中的方法
//                                    final List<MethodDeclaration> newMethods = newClassOrInterface.getMethodsByName(existingMethodName);
//                                    // 如果在新代码中已存在，那么先删除
//                                    if (!newMethodrInterfaces = existingCompilationUnit.findAll(ClassOrInterfaceDeclaration.class);
//    for(final ClassOrInterfaceDeclaration existis.isEmpty()){
//                                        // 删除所有找到的方法
//                                        for (final MethodDeclaration newMethod : newMethods) {
//                                            newMethod.remove();
//                                        }
//                                    }
//                                } else {
//                                    // 新代码中的方法
//                                    final List<MethodDeclaration> newMethods = newClassOrInterface.getMethodsBySignature(existingMethodName,
//                                            paramTypeToStrings(existingMethod.getSignature().getParameterTypes()));
//                                    // 如果在新代码中已存在，那么先删除
//                                    if (!newMethods.isEmpty()) {
//                                        // 删除所有找到的方法
//                                        for (final MethodDeclaration newMethod : newMethods) {
//                                            newMethod.remove();
//                                        }
//                                    }
//                                }
//                                // 由于找不到直接添加当前节点的方法，所以先添加一个新的，再用当前节点替换掉这个节点 ^O^!
//                                final MethodDeclaration newMethod = newClassOrInterface.addMethod(existingMethodName);
//                                newClassOrInterface.replace(newMethod, existingMember);
//                            }
//                        }
//                    }
//        }
//
//        // 合并imports
//        final NodeList<ImportDeclaration> existingImports = existingCompilationUnit.getImports();
//        final NodeList<ImportDeclaration> newImports      = newCompilationUnit.getImports();
//        OUTLOOP: for (final ImportDeclaration existingImport : existingImports) {
//            for (final ImportDeclaration newImport : newImports) {
//                if (newImport.getName().equals(existingImport.getName())) {
//                    continue OUTLOOP;
//                }
//            }
//            newImports.add(existingImport);
//        }
//
//        // 移除没有用的import，并返回格式化后的源代码
//        return JavaSourceUtils.removeUnusedImports(newCompilationUnit.toString());
//    }

    /**
     * 获取Javadoc中的注解
     *
     * @param javadocComment Javadoc的注释
     * @return
     */
    private static List<JavadocBlockTag> getTags(final JavadocComment javadocComment) {
        final Javadoc javadoc = javadocComment.parse();
        return javadoc.getBlockTags();
    }

    /**
     * 判断JavaDoc的Tag列表里面是否有指定的注解
     *
     * @param javadocTags JavaDoc的Tag列表
     * @param tags        判断是否包含的注解
     * @return 是否有Javadoc注释，且里面包含指定的注解
     */
    private static boolean hasTag(final List<JavadocBlockTag> javadocTags, final String[] tags) {
        for (final JavadocBlockTag javadocTag : javadocTags) {
            for (final String tag : tags) {
                if (javadocTag.getTagName().equals(tag.substring(1))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断Javadoc的注释中有没有包含指定的注解
     *
     * @param javadocComment Javadoc的注释
     * @param tags           判断是否包含的注解
     * @return 是否包含
     */
    private static boolean hasTag(final JavadocComment javadocComment, final String[] tags) {
        final List<JavadocBlockTag> javadocTags = getTags(javadocComment);
        return hasTag(javadocTags, tags);
    }

    /**
     * 判断节点是否有Javadoc注释，且里面包含指定的注解
     *
     * @param node 节点
     * @param tags 判断是否包含的注解
     * @return 是否有Javadoc注释，且里面包含指定的注解
     */
    private static boolean hasTag(final Node node, final String[] tags) {
        final Optional<Comment> comment = node.getComment();
        if (comment.isPresent() && comment.get().isJavadocComment()) {
            final JavadocComment javadocComment = (JavadocComment) comment.get();
            return hasTag(javadocComment, tags);
        }
        return false;
    }

    /**
     * 合并Javadoc注解
     *
     * @param srcJavadocComment 源注解
     * @param dstJavadocComment 目的注解
     * @return 合并后的注释
     */
    private static JavadocComment mergeJavadocComment(final JavadocComment srcJavadocComment, final JavadocComment dstJavadocComment) {
        final Javadoc srcJavadoc = srcJavadocComment.parse();
        final Javadoc dstJavadoc = dstJavadocComment.parse();

        // 如果目的注释有手工添加的注解，则保留下来
        final List<JavadocBlockTag> srcJavadocTags = srcJavadoc.getBlockTags();
        final List<JavadocBlockTag> dstJavadocTags = dstJavadoc.getBlockTags();
        OUTLOOP: for (final JavadocBlockTag srcJavadocTag : srcJavadocTags) {
            for (final JavadocBlockTag dstJavadocTag : dstJavadocTags) {
                if (srcJavadocTag.getTagName().equals(dstJavadocTag.getTagName())) {
                    continue OUTLOOP;
                }
            }
            dstJavadocTags.add(srcJavadocTag);
        }
        return dstJavadoc.toComment();
    }

    /**
     * 在新代码的类或接口中，删除标记有@mbg.removeField的成员
     *
     * @param javadocComment   Javadoc的注释
     * @param classOrInterface 查找的类或接口
     */
    private static void removeTagMembers(final JavadocComment javadocComment, final ClassOrInterfaceDeclaration classOrInterface) {
        final Javadoc               javadoc   = javadocComment.parse();
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
     * @param paramTypes 参数列表
     * @return String[]
     */
    private static String[] paramTypeToStrings(final List<Type> paramTypes) {
        return paramTypes.stream().map(Type::asString).toArray(String[]::new);
    }

}
