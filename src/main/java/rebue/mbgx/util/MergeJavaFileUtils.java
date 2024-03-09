package rebue.mbgx.util;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MergeJavaFileUtils {

    /**
     * 合并Java代码 将已存在的Java文件中的手工添加的部分合并进新模板的Java代码
     *
     * @param newFileSource               新代码文件的内容
     * @param existingFileFullPath        已存在的代码文件的全路径
     * @param autoGenTags                 标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @param removedMemberTags           标识要删除成员的注解(将此数组中的任意注解加上成员名称放在类或接口的Javadoc注释中表示此成员不要自动生成)
     * @param dontOverWriteFileTags       标识不要重写此文件的注解(放在最上方的文档注释中)
     * @param dontOverWriteAnnotationTags 不覆盖注解
     * @param dontOverWriteExtendsTags    不覆盖extends
     * @param dontOverWriteImplementsTags 不覆盖implements
     * @return 合并后的新内容
     * @throws FileNotFoundException
     */
    public static String merge(final String newFileSource, final String existingFileFullPath, final String[] autoGenTags, final String[] removedMemberTags,
                               final String[] dontOverWriteFileTags, final String[] dontOverWriteAnnotationTags, final String[] dontOverWriteExtendsTags,
                               final String[] dontOverWriteImplementsTags)
            throws FileNotFoundException {
        return merge(newFileSource, new File(existingFileFullPath), autoGenTags, removedMemberTags, dontOverWriteFileTags,
                dontOverWriteAnnotationTags, dontOverWriteExtendsTags, dontOverWriteImplementsTags);
    }

    /**
     * 合并Java代码
     *
     * @param newFileSource               新代码文件的内容
     * @param existingFile                已存在的代码文件
     * @param autoGenTags                 标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @param removedMemberTags           标识要删除成员的注解(将此数组中的任意注解加上成员名称放在类或接口的Javadoc注释中表示此成员不要自动生成)
     * @param dontOverWriteFileTags       标识不要重写此文件的注解(放在最上方的文档注释中)
     * @param dontOverWriteAnnotationTags 不覆盖注解
     * @param dontOverWriteExtendsTags    不覆盖extends
     * @param dontOverWriteImplementsTags 不覆盖implements
     * @return 合并后的新内容
     * @throws FileNotFoundException
     */
    public static String merge(final String newFileSource, final File existingFile, final String[] autoGenTags, final String[] removedMemberTags,
                               final String[] dontOverWriteFileTags, final String[] dontOverWriteAnnotationTags, final String[] dontOverWriteExtendsTags,
                               final String[] dontOverWriteImplementsTags)
            throws FileNotFoundException {
        log.info("合并JAVA代码: 已存在的文件-{}", existingFile.getAbsolutePath());
        final CompilationUnit newCompilationUnit      = StaticJavaParser.parse(JavaParserUtils.format(newFileSource));
        final CompilationUnit existingCompilationUnit = StaticJavaParser.parse(existingFile);
        LexicalPreservingPrinter.setup(existingCompilationUnit);    // 已存在的代码需要保留原来格式
        return mergeCompilationUnit(newCompilationUnit, existingCompilationUnit, autoGenTags, removedMemberTags, dontOverWriteFileTags,
                dontOverWriteAnnotationTags, dontOverWriteExtendsTags, dontOverWriteImplementsTags);
    }

    /**
     * 合并Java代码
     *
     * @param newCompilationUnit          新代码的编译器
     * @param oldCompilationUnit          已存在代码的编译器
     * @param autoGenTags                 标识自动生成的代码的注解(将此数组中的任意注解放在节点的Javadoc注释中表示此成员是自动生成的)
     * @param removedMemberTags           标识要删除成员的注解(将此数组中的任意注解加上成员名称放在类或接口的Javadoc注释中表示此成员不要自动生成)
     * @param dontOverWriteFileTags       标识不要重写此文件的注解(放在最上方的文档注释中)
     * @param dontOverWriteAnnotationTags 不覆盖注解
     * @param dontOverWriteExtendsTags    不覆盖extends
     * @param dontOverWriteImplementsTags 不覆盖implements
     * @return 合并后的内容
     */
    private static String mergeCompilationUnit(final CompilationUnit newCompilationUnit, final CompilationUnit oldCompilationUnit, final String[] autoGenTags,
                                               final String[] removedMemberTags, final String[] dontOverWriteFileTags,
                                               final String[] dontOverWriteAnnotationTags, final String[] dontOverWriteExtendsTags,
                                               final String[] dontOverWriteImplementsTags) {
        final Optional<Comment> oldCommentOfPackage = oldCompilationUnit.getComment();
        // 如果旧注释中含有不覆盖的注解，才用新代码的注释
        if (oldCommentOfPackage.isPresent() && hasTag(oldCommentOfPackage.get(), dontOverWriteFileTags)) {
            return oldCompilationUnit.toString();
        }

        log.info("判断是否替换代码开头的注释");
        // 如果新代码有注释
        newCompilationUnit.getComment().ifPresent(newComment -> {
            // 如果旧代码中有注释，判断是否需要用新代码的注释来代替
            if (oldCommentOfPackage.isPresent()) {
                // 如果是JavaDoc注释，且含有自动生成的注解，才用新代码的注释
                if (oldCommentOfPackage.get().isJavadocComment() && hasTag(oldCommentOfPackage.get(), autoGenTags)) {
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
        OUTLOOP:
        for (final ImportDeclaration newImport : newCompilationUnit.getImports()) {
            for (final ImportDeclaration oldImport : oldCompilationUnit.getImports()) {
                if (oldImport.getName().equals(newImport.getName())) {
                    continue OUTLOOP;
                }
            }
            oldCompilationUnit.getImports().add(newImport);
        }

        log.info("合并类或接口，遍历新代码中的类和接口");
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
                    // 如果新代码有注释，且旧代码含有自动生成的注解，直接使用新代码的注释，但是添加旧代码手工添加的注解
                    if (newClassOrInterface.getComment().isPresent() && hasTag(oldComment.asJavadocComment(), autoGenTags)) {
                        oldClassOrInterface.setComment(mergeJavadocTags(oldComment.asJavadocComment(), newClassOrInterface.getComment().get().asJavadocComment()));
                    }
                }
            });

            // 获取要移除的成员列表
            final List<String> removedMembers = new LinkedList<>();
            oldClassOrInterface.getComment().ifPresent(comment -> comment.ifJavadocComment(javadocComment -> {
                final List<JavadocBlockTag> javadocTags = getTags(javadocComment);
                for (final JavadocBlockTag javadocTag : javadocTags) {
                    log.info("获取要移除的成员列表");
                    for (final String tag : removedMemberTags) {
                        if (javadocTag.getTagName().equals(tag.substring(1))) {
                            removedMembers.addAll(Arrays.asList(javadocTag.getContent().toText().replace(" ", "").replace("，", ",").split("[,;]")));
                            break;
                        }
                    }
                }
            }));

            log.info("使用新的类或接口的代码替换旧代码");
            // 判断是否覆盖旧代码中的注解
            if (!hasTag(oldClassOrInterface, dontOverWriteAnnotationTags)) {
                oldClassOrInterface.setAnnotations(newClassOrInterface.getAnnotations());
            }
            // 判断是否覆盖extends
            if (!hasTag(oldClassOrInterface, dontOverWriteExtendsTags)) {
                oldClassOrInterface.setExtendedTypes(newClassOrInterface.getExtendedTypes());
            }
            // 判断是否覆盖implements
            if (!hasTag(oldClassOrInterface, dontOverWriteImplementsTags)) {
                oldClassOrInterface.setImplementedTypes(newClassOrInterface.getImplementedTypes());
            }
            oldClassOrInterface.setInterface(newClassOrInterface.isInterface());
            oldClassOrInterface.setPrivate(newClassOrInterface.isPrivate());
            oldClassOrInterface.setProtected(newClassOrInterface.isProtected());
            oldClassOrInterface.setPublic(newClassOrInterface.isPublic());
            oldClassOrInterface.setAbstract(newClassOrInterface.isAbstract());
            oldClassOrInterface.setFinal(newClassOrInterface.isFinal());
            oldClassOrInterface.setStatic(newClassOrInterface.isStatic());
            oldClassOrInterface.setModifiers(newClassOrInterface.getModifiers());
            oldClassOrInterface.setName(newClassOrInterface.getName());
            oldClassOrInterface.setTypeParameters(newClassOrInterface.getTypeParameters());

            log.info("合并类或接口的成员");
            final NodeList<BodyDeclaration<?>> newMembers = newClassOrInterface.getMembers();

            log.info("旧类或接口中删除在新类或接口中已经不存在的自动生成的成员");
            final NodeList<BodyDeclaration<?>> oldMembers      = oldClassOrInterface.getMembers();
            final List<BodyDeclaration<?>>     toRemoveMembers = new LinkedList<>();
            for (final BodyDeclaration<?> oldMember : oldMembers) {
                // 如果没有注释，或不是javadoc注释，或不包含自动生成注解，则不删除此成员
                final Optional<Comment> oldCommentOptional = oldMember.getComment();
                if (!oldCommentOptional.isPresent() || !oldCommentOptional.get().isJavadocComment() || !hasTag(oldCommentOptional.get().asJavadocComment(), autoGenTags)) {
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
                            || !hasTag(oldCommentOptional.get().asJavadocComment(), autoGenTags)) {
                        continue;
                    }

                    // 将旧注释中手工添加的注解加入新注释中
                    newMember.setComment(mergeJavadocTags(oldCommentOptional.get().asJavadocComment(), newMember.getComment().get().asJavadocComment()));

                    // 判断是否不要覆盖旧成员的注解
                    if (hasTag(newMember, dontOverWriteAnnotationTags)) {
                        newMember.setAnnotations(oldField.getAnnotations());
                    }

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
                            || !hasTag(oldCommentOptional.get().asJavadocComment(), autoGenTags)) {
                        continue;
                    }

                    // 将旧注释中手工添加的注解加入新注释中
                    newMember.setComment(mergeJavadocTags(oldCommentOptional.get().asJavadocComment(), newMember.getComment().get().asJavadocComment()));

                    // 判断是否不要覆盖旧成员的注解
                    if (hasTag(newMember, dontOverWriteAnnotationTags)) {
                        newMember.setAnnotations(oldCallable.getAnnotations());
                    }

                    // 替换方法
                    oldClassOrInterface.replace(oldCallable, newMember);
                }
            }
        }

        // 移除没有用的import
        JavaParserUtils.removeUnusedImports(oldCompilationUnit);

        // 返回源代码
        // return JdtUtils.format(JavaParserUtils.print(oldCompilationUnit));
//        return JdtUtils.format(oldCompilationUnit.toString());
        // return GoogleJavaFormatUtils.format(JavaParserUtils.print(oldCompilationUnit));
        return oldCompilationUnit.toString();
    }

    /**
     * 获取Javadoc中的注解
     *
     * @param javadocComment Javadoc的注释
     * @return Javadoc中的注解列表
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
     * 合并Javadoc注释中的注解(如果目的注释有手工添加的注解，则保留下来)
     *
     * @param srcJavadocComment 源注释
     * @param dstJavadocComment 目的注释
     * @return 合并后的注释
     */
    private static JavadocComment mergeJavadocTags(final JavadocComment srcJavadocComment, final JavadocComment dstJavadocComment) {
        final Javadoc srcJavadoc = srcJavadocComment.parse();
        final Javadoc dstJavadoc = dstJavadocComment.parse();

        // 如果目的注释有手工添加的注解，则保留下来
        final List<JavadocBlockTag> srcJavadocTags = srcJavadoc.getBlockTags();
        final List<JavadocBlockTag> dstJavadocTags = dstJavadoc.getBlockTags();
        OUTLOOP:
        for (final JavadocBlockTag srcJavadocTag : srcJavadocTags) {
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

    // /**
    // * 将参数列表转成String[]
    // *
    // * @param paramTypes 参数列表
    // * @return String[]
    // */
    // private static String[] paramTypeToStrings(final List<Type> paramTypes) {
    // return paramTypes.stream().map(Type::asString).toArray(String[]::new);
    // }

}
