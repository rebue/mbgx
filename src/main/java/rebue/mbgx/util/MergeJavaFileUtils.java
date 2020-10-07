package rebue.mbgx.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MergeJavaFileUtils {

    /**
     * 合并Java代码 将已存在的Java文件中的手工添加的部分合并进新模板的Java代码
     *
     * @param newFileSource
     *            新代码文件的内容
     * @param existingFileFullPath
     *            已存在的代码文件的全路径
     * @param javadocTagsOfAutoGen
     *            标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @param javadocTagsOfRemovedMember
     *            标识要删除成员的注解(将此数组中的任意注解加上成员名称放在类或接口的Javadoc注释中表示此成员不要自动生成)
     * @return 合并后的新内容
     */
    public static String merge(final String newFileSource, final String existingFileFullPath, final String[] javadocTagsOfAutoGen, final String[] javadocTagsOfRemovedMember)
            throws FileNotFoundException {
        return merge(newFileSource, new File(existingFileFullPath), javadocTagsOfAutoGen, javadocTagsOfRemovedMember);
    }

    /**
     * 合并Java代码
     *
     * @param newFileSource
     *            新代码文件的内容
     * @param existingFile
     *            已存在的代码文件
     * @param javadocTagsOfAutoGen
     *            标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @param javadocTagsOfRemovedMember
     *            标识要删除成员的注解(将此数组中的任意注解加上成员名称放在类或接口的Javadoc注释中表示此成员不要自动生成)
     * @return 合并后的新内容
     */
    public static String merge(final String newFileSource, final File existingFile, final String[] javadocTagsOfAutoGen, final String[] javadocTagsOfRemovedMember)
            throws FileNotFoundException {
        log.info("合并JAVA代码: 已存在的文件-{}", existingFile.getAbsolutePath());
        final CompilationUnit newCompilationUnit = StaticJavaParser.parse(newFileSource);
        final CompilationUnit existingCompilationUnit = StaticJavaParser.parse(existingFile);
        return mergeCompilationUnit(newCompilationUnit, existingCompilationUnit, javadocTagsOfAutoGen, javadocTagsOfRemovedMember);
    }

    /**
     * 合并Java代码
     *
     * @param newCompilationUnit
     *            新代码的编译器
     * @param oldCompilationUnit
     *            已存在代码的编译器
     * @param javadocTagsOfAutoGen
     *            标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @param javadocTagsOfRemovedMember
     *            标识要删除成员的注解(将此数组中的任意注解加上成员名称放在类或接口的Javadoc注释中表示此成员不要自动生成)
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
        newCompilationUnit.getPackageDeclaration().ifPresent(oldCompilationUnit::setPackageDeclaration);

        log.info("合并imports");
        OUTLOOP: for (final ImportDeclaration newImport : newCompilationUnit.getImports()) {
            for (final ImportDeclaration oldImport : oldCompilationUnit.getImports()) {
                if (oldImport.getName().equals(newImport.getName())) {
                    continue OUTLOOP;
                }
            }
            oldCompilationUnit.getImports().add(newImport);
        }

        log.info("合并类或接口");
        final List<ClassOrInterfaceDeclaration> classOrInterfaces = newCompilationUnit.findAll(ClassOrInterfaceDeclaration.class);
        for (final ClassOrInterfaceDeclaration newClassOrInterface : classOrInterfaces) {
            // 新的类或接口的名称
            final String classOrInterfaceName = newClassOrInterface.getNameAsString();
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
            // 如果旧代码有注释，判断是否要替换新代码的注释
            oldClassOrInterface.getComment().ifPresent(oldComment -> {
                // 如果旧代码是JavaDoc注释
                if (oldComment.isJavadocComment()) {
                    // 如果新代码没有注释，或旧代码不含有自动生成的注解，直接替换新代码的注释
                    if (!newClassOrInterface.getComment().isPresent() || hasTag(oldComment.asJavadocComment(), javadocTagsOfAutoGen)) {
                        newClassOrInterface.setComment(oldComment);
                    }
                    // 否则使用新代码的注释，但是添加旧代码手工添加的注解
                    else {
                        newClassOrInterface.setComment(mergeJavadocTags(oldComment.asJavadocComment(), newClassOrInterface.getComment().get().asJavadocComment()));
                    }
                }
                // 如果旧代码不是JavaDoc注释，直接替换新代码的注释
                else {
                    newClassOrInterface.setComment(oldComment);
                }
            });

            // 获取要移除的成员列表
            final List<String> removedMembers = new LinkedList<>();
            oldClassOrInterface.getComment().ifPresent(comment -> comment.ifJavadocComment(javadocComment -> {
                final List<JavadocBlockTag> javadocTags = getTags(javadocComment);
                for (final JavadocBlockTag javadocTag : javadocTags) {
                    log.info("获取要移除的成员列表");
                    for (final String tag : javadocTagsOfRemovedMember) {
                        if (javadocTag.getTagName().equals(tag.substring(1))) {
                            removedMembers.add(javadocTag.getContent().toText());
                            break;
                        }
                    }
                }
            }));

            log.info("合并类或接口的注解");
            OUTLOOP: for (final AnnotationExpr newAnnotation : newClassOrInterface.getAnnotations()) {
                for (final AnnotationExpr oldAnnotation : oldClassOrInterface.getAnnotations()) {
                    if (oldAnnotation.getName().equals(newAnnotation.getName())) {
                        continue OUTLOOP;
                    }
                }
                oldClassOrInterface.getAnnotations().add(newAnnotation);
            }

            log.info("使用新的类或接口的代码替换旧代码");
            oldClassOrInterface.setModifiers(newClassOrInterface.getModifiers());
            oldClassOrInterface.setName(newClassOrInterface.getName());
            oldClassOrInterface.setExtendedTypes(newClassOrInterface.getExtendedTypes());
            oldClassOrInterface.setImplementedTypes(newClassOrInterface.getImplementedTypes());
            oldClassOrInterface.setTypeParameters(newClassOrInterface.getTypeParameters());

            log.info("合并类或接口的成员");
            final NodeList<BodyDeclaration<?>> newMembers = newClassOrInterface.getMembers();

            log.info("旧类或接口中删除在新类或接口中已经不存在的自动生成的成员");
            final NodeList<BodyDeclaration<?>> oldMembers = oldClassOrInterface.getMembers();
            final List<BodyDeclaration<?>> toRemoveMembers = new LinkedList<>();
            for (final BodyDeclaration<?> oldMember : oldMembers) {
                // 如果没有注释，或不是javadoc注释，或不包含自动生成注解，则不删除此成员
                final Optional<Comment> oldCommentOptional = oldMember.getComment();
                if (!oldCommentOptional.isPresent() || !oldCommentOptional.get().isJavadocComment() || !hasTag(oldCommentOptional.get().asJavadocComment(), javadocTagsOfAutoGen)) {
                    continue;
                }

                // 如果是字段
                if (oldMember.isFieldDeclaration()) {
                    // 新字段
                    final FieldDeclaration oldField = oldMember.asFieldDeclaration();
                    // 获取字段的名称
                    final String fieldName = getFieldName(oldField);
                    // 新代码中的字段
                    final Optional<FieldDeclaration> newFieldOptional = newClassOrInterface.getFieldByName(fieldName);
                    if (!newFieldOptional.isPresent()) {
                        log.info("此字段在新代码中不存在，将其删除: {}", fieldName);
                        toRemoveMembers.add(oldMember);
                    }
                }
                // 如果是方法(包含构造方法)
                else if (oldMember.isCallableDeclaration()) {
                    // 新方法
                    final CallableDeclaration<?> oldCallable = oldMember.asCallableDeclaration();
                    // 获取新方法的名称
                    final String callableName = oldCallable.getNameAsString();
                    log.info("当前成员是方法: {}", callableName);

                    // 获取旧方法的签名
                    final CallableDeclaration.Signature oldCallableSignature = oldCallable.getSignature();

                    // 获取新方法列表
                    final List<CallableDeclaration<?>> newCallables = newClassOrInterface.getCallablesWithSignature(oldCallableSignature);
                    if (newCallables.isEmpty()) {
                        log.info("此方法在新代码中不存在，将其删除: {}", callableName);
                        toRemoveMembers.add(oldMember);
                    }
                }
            }

            for (final BodyDeclaration<?> removedMember : toRemoveMembers) {
                oldClassOrInterface.remove(removedMember);
            }

            log.info("将新类或接口中的成员合并到旧类或接口中");
            for (final BodyDeclaration<?> newMember : newMembers) {
                // 如果是字段
                if (newMember.isFieldDeclaration()) {
                    // 新字段
                    final FieldDeclaration newField = newMember.asFieldDeclaration();
                    // 获取字段的类型
                    final String newFieldType = getFieldType(newField);
                    // 获取字段的名称
                    final String newFieldName = getFieldName(newField);
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

                    final FieldDeclaration oldField = oldFieldOptional.get();

                    // 如果没有注释，或不是javadoc注释，或不包含自动生成注解，则不替换此成员
                    final Optional<Comment> oldCommentOptional = oldField.getComment();
                    if (!oldCommentOptional.isPresent() || !oldCommentOptional.get().isJavadocComment()
                            || !hasTag(oldCommentOptional.get().asJavadocComment(), javadocTagsOfAutoGen)) {
                        continue;
                    }

                    // 将旧注释中手工添加的注解加入新注释中
                    newMember.setComment(mergeJavadocTags(oldCommentOptional.get().asJavadocComment(), newMember.getComment().get().asJavadocComment()));

                    // 替换字段
                    oldClassOrInterface.replace(oldField, newMember);

                }
                // 如果是方法(包含构造方法)
                else if (newMember.isCallableDeclaration()) {
                    // 新方法
                    final CallableDeclaration<?> newCallable = newMember.asCallableDeclaration();
                    // 获取新方法的名称
                    final String newCallableName = newCallable.getNameAsString();
                    log.info("当前成员是方法: {}", newCallableName);

                    if (removedMembers.contains(newCallableName)) {
                        log.info("此方法已在类或接口的javadoc注解中声明删除: {}", newCallableName);
                        continue;
                    }

                    // 获取新方法的签名
                    final CallableDeclaration.Signature newCallableSignature = newCallable.getSignature();

                    // 获取旧方法列表
                    final List<CallableDeclaration<?>> oldCallables = oldClassOrInterface.getCallablesWithSignature(newCallableSignature);
                    if (oldCallables.isEmpty()) {
                        log.info("此方法在旧代码中不存在，直接添加: {}", newCallableName);
                        oldClassOrInterface.addMember(newMember);
                        continue;
                    }
                    if (oldCallables.size() > 1) {
                        throw new RuntimeException("源代码中出现多个同样签名的方法");
                    }

                    final CallableDeclaration<?> oldCallable = oldCallables.get(0);

                    // 如果没有注释，或不是javadoc注释，或不包含自动生成注解，则不替换此成员
                    final Optional<Comment> oldCommentOptional = oldCallable.getComment();
                    if (!oldCommentOptional.isPresent() || !oldCommentOptional.get().isJavadocComment()
                            || !hasTag(oldCommentOptional.get().asJavadocComment(), javadocTagsOfAutoGen)) {
                        continue;
                    }

                    // 将旧注释中手工添加的注解加入新注释中
                    newMember.setComment(mergeJavadocTags(oldCommentOptional.get().asJavadocComment(), newMember.getComment().get().asJavadocComment()));

                    // 替换方法
                    oldClassOrInterface.replace(oldCallable, newMember);
                }
            }

//            for (final BodyDeclaration<?> oldMember : oldMembers) {
//                oldClassOrInterface.remove(oldMember);
//            }
        }

        // 移除没有用的import，并返回格式化后的源代码
        return JavaSourceUtils.removeUnusedImports(oldCompilationUnit.toString());
    }

    /**
     * 获取Javadoc中的注解
     *
     * @param javadocComment
     *            Javadoc的注释
     * @return Javadoc中的注解列表
     */
    private static List<JavadocBlockTag> getTags(final JavadocComment javadocComment) {
        final Javadoc javadoc = javadocComment.parse();
        return javadoc.getBlockTags();
    }

    /**
     * 判断JavaDoc的Tag列表里面是否有指定的注解
     *
     * @param javadocTags
     *            JavaDoc的Tag列表
     * @param tags
     *            判断是否包含的注解
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
     * @param javadocComment
     *            Javadoc的注释
     * @param tags
     *            判断是否包含的注解
     * @return 是否包含
     */
    private static boolean hasTag(final JavadocComment javadocComment, final String[] tags) {
        final List<JavadocBlockTag> javadocTags = getTags(javadocComment);
        return hasTag(javadocTags, tags);
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
        if (comment.isPresent() && comment.get().isJavadocComment()) {
            final JavadocComment javadocComment = (JavadocComment) comment.get();
            return hasTag(javadocComment, tags);
        }
        return false;
    }

    /**
     * 合并Javadoc注释中的注解(如果目的注释有手工添加的注解，则保留下来)
     *
     * @param srcJavadocComment
     *            源注释
     * @param dstJavadocComment
     *            目的注释
     * @return 合并后的注释
     */
    private static JavadocComment mergeJavadocTags(final JavadocComment srcJavadocComment, final JavadocComment dstJavadocComment) {
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
            dstJavadocTags.add(0, srcJavadocTag);
        }
        return dstJavadoc.toComment();
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

//    /**
//     * 将参数列表转成String[]
//     *
//     * @param paramTypes 参数列表
//     * @return String[]
//     */
//    private static String[] paramTypeToStrings(final List<Type> paramTypes) {
//        return paramTypes.stream().map(Type::asString).toArray(String[]::new);
//    }

}
