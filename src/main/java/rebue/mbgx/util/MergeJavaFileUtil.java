package rebue.mbgx.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Field;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.Method;
import org.jboss.forge.roaster.model.source.FieldHolderSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MemberHolderSource;
import org.jboss.forge.roaster.model.source.MemberSource;
import org.jboss.forge.roaster.model.source.MethodHolderSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class MergeJavaFileUtil {

    /**
     * 将旧代码中手工添加(非自动生成)的代码添加到新的代码中
     * 
     * @param newFileSource
     *            新代码文件的内容
     * @param oldFile
     *            旧代码文件
     * @param javadocTags
     *            标识自动生成的代码的注解(此注解放在成员的JavaDoc中表示此成员是自动生成的)
     * @return 合并后的新内容
     * @throws FileNotFoundException
     * @throws BadLocationException
     * @throws CoreException
     * @throws MalformedTreeException
     * @throws OperationCanceledException
     */
    public static String merge(String newFileSource, File oldFile, String[] javadocTags)
            throws FileNotFoundException, OperationCanceledException, MalformedTreeException, CoreException, BadLocationException {
        System.out.println(oldFile.getAbsolutePath());
        JavaType<?> javaType = Roaster.parse(oldFile);
        if (javaType.isClass()) {
            JavaClassSource oldJavaClassSource = Roaster.parse(JavaClassSource.class, oldFile);
            JavaClassSource newJavaClassSource = Roaster.parse(JavaClassSource.class, newFileSource);
            if (copyMembers(oldJavaClassSource, newJavaClassSource, javadocTags)) {
                copyImports(oldJavaClassSource, newJavaClassSource);
                newFileSource = oldJavaClassSource.toUnformattedString();
            }
            return format(newFileSource);
        } else if (javaType.isInterface()) {
            JavaInterfaceSource oldJavaInterfaceSource = Roaster.parse(JavaInterfaceSource.class, oldFile);
            JavaInterfaceSource newJavaInterfaceSource = Roaster.parse(JavaInterfaceSource.class, newFileSource);
            if (copyMembers(oldJavaInterfaceSource, newJavaInterfaceSource, javadocTags)) {
                copyImports(oldJavaInterfaceSource, newJavaInterfaceSource);
                newFileSource = newJavaInterfaceSource.toUnformattedString();
            }
            return format(newFileSource);
        }
        return newFileSource;
    }

    /**
     * 将旧代码中非自动生成的成员（手工添加的成员代码）添加到新的代码中
     * 
     * @param oldJavaSource
     *            旧代码（其中可能有手工添加的成员代码）
     * @param newJavaSource
     *            新代码
     * @param javadocTags
     *            用于识别是自动生成，还是手工添加的成员的注解（自动生成的成员的Java Doc中会有这类注解，而手工添加的没有）
     * @return 旧代码是否有手工添加的成员，如果没有，返回false，不需要保留和解析旧的代码，直接用新代码覆盖
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static boolean copyMembers(MemberHolderSource<?> oldJavaSource, MemberHolderSource<?> newJavaSource, String[] javadocTags) {
        boolean isModifiedOfOld = false;
        for (Object item : newJavaSource.getMembers()) {
            MemberSource<?, ?> newMember = (MemberSource<?, ?>) item;
            if (newMember instanceof Field) {
                Field<?> newField = (Field<?>) newMember;
                FieldHolderSource<?> oldFieldHolderSource = ((FieldHolderSource<?>) oldJavaSource);
                // 如果旧代码中有相同的字段
                if (!oldFieldHolderSource.getFields().isEmpty() && oldFieldHolderSource.hasField(newField.getName())) {
                    FieldSource<?> oldFieldSource = oldFieldHolderSource.getField(newField.getName());
                    JavaDocSource<?> javaDoc = oldFieldSource.getJavaDoc();
                    if (hasTag(javaDoc, javadocTags)) {
                        oldFieldHolderSource.removeField((Field) oldFieldSource);
                    } else {
                        isModifiedOfOld = true;
                        oldFieldHolderSource.removeField((Field) oldFieldSource);
                        oldFieldHolderSource.addField(oldFieldSource.toString());
                        continue;
                    }
                }
                oldFieldHolderSource.addField(newField.toString());
            } else if (newMember instanceof Method) {
                MethodHolderSource<?> oldMethodHolderSource = ((MethodHolderSource<?>) oldJavaSource);
                Method<?, ?> newMethod = (Method<?, ?>) newMember;
                // 如果旧代码中有相同的方法
                MethodSource<?> oldMethodSource = getMethodByName(oldMethodHolderSource, newMethod.getName());
                if (oldMethodSource != null) {
                    JavaDocSource<?> javaDoc = oldMethodSource.getJavaDoc();
                    if (hasTag(javaDoc, javadocTags)) {
                        oldMethodHolderSource.removeMethod((Method) oldMethodSource);
                    } else {
                        isModifiedOfOld = true;
//                        oldMethodHolderSource.removeMethod((Method) oldMethodSource);
//                        oldMethodHolderSource.addMethod(oldMethodSource);
                        continue;
                    }
                }
                oldMethodHolderSource.addMethod(newMethod);
            }
        }

        return isModifiedOfOld;
    }

//    private static String getMethodText(MethodSource<?> oldMethodSource, int startPos, int length) {
//        char[] dest = new char[length];
//        System.arraycopy(oldMethodSource.getOrigin().toUnformattedString().toCharArray(), startPos, dest, 0, length);
//        return new String(dest);
//    }

    /**
     * 将旧代码中非自动生成的import（手工添加的import代码）添加到新的代码中
     * 
     * @param oldJavaSource
     * @param newJavaSource
     */
    private static void copyImports(JavaSource<?> oldJavaSource, JavaSource<?> newJavaSource) {
        for (Import imprt : newJavaSource.getImports()) {
            oldJavaSource.addImport(imprt);
        }
    }

    /**
     * 判断在Java Doc中是否有指定的注解
     * 
     * @param javaDoc
     * @param javadocTags
     * @return
     */
    private static boolean hasTag(JavaDocSource<?> javaDoc, String[] javadocTags) {
        for (String tagName : javadocTags) {
            if (javaDoc.getTags(tagName).size() > 0)
                return true;
        }
        return false;
    }

    private static MethodSource<?> getMethodByName(MethodHolderSource<?> methodHolderSource, String methodName) {
        for (MethodSource<?> methodSoruce : methodHolderSource.getMethods()) {
            if (methodSoruce.getName().equals(methodName)) {
                return methodSoruce;
            }
        }
        return null;
    }

    /**
     * 格式化Java源代码
     * 
     * @param sourceCode
     *            源代码内容
     * @throws CoreException
     * @throws OperationCanceledException
     * @throws BadLocationException
     * @throws MalformedTreeException
     */
    public static String format(String sourceCode) throws OperationCanceledException, CoreException, MalformedTreeException, BadLocationException {
        IDocument doc = new Document(sourceCode);
        CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(null);
        TextEdit textEdit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS, sourceCode, 0, sourceCode.length(), 0, null);
        try {
            textEdit.apply(doc);
        } catch (MalformedTreeException e) {
            e.printStackTrace();
            return null;
        } catch (BadLocationException e) {
            e.printStackTrace();
            return null;
        }

        return doc.get();

//        ASTParser parser = ASTParser.newParser(AST.JLS10);
//        parser.setKind(ASTParser.K_COMPILATION_UNIT);
//        parser.setSource(doc.get().toCharArray());
//        CompilationUnit node = (CompilationUnit) parser.createAST(null);
//
////        IJavaElement element = node.getJavaElement();
//        IJavaElement element = JavaCore.create(file);
//
////        element = ((ICompilationUnit) element).getWorkingCopy(null);
////        ((ICompilationUnit) element).getBuffer().setContents(doc.get());
////        final CompilationUnit node = (CompilationUnit) parser.createAST(new NullProgressMonitor());
////        OrganizeImportsOperation organizeImportsOperation = new OrganizeImportsOperation((ICompilationUnit) element, node, true, false, false, new IChooseImportQuery() {
//        OrganizeImportsOperation organizeImportsOperation = new OrganizeImportsOperation(node.get, node, false, false, false, null);
//        textEdit = organizeImportsOperation.createTextEdit(null);
//        textEdit.apply(doc);
//        return doc.get();
    }

    public static void organizeImports(String source, String filePath) {
        IJavaProject project = null;
        IPath genSourceFolderPath = new Path(filePath);
        for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            if (p.isOpen()) {
                // 查找工程的实际路径是否在源代码生成路径上，如果是，则就是该工程
                if (p.getLocation().isPrefixOf(genSourceFolderPath)) {
                    project = JavaCore.create(p);
                }
            }
        }
        if (project == null) {
            throw new RuntimeException("无法生成源码文件:" + filePath);
        }

        IPath javaFilePath = new Path(filePath).makeRelativeTo(project.getProject().getLocation());
        IFile javaFile = project.getProject().getFile(javaFilePath);
        IJavaElement element = JavaCore.create(javaFile);

//        String lineDelimiter = StubUtility.getLineDelimiterUsed(null);
        String formattedSource = CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, source.toString(), 0, null, (IJavaProject) null);

        ASTParser parser = ASTParser.newParser(AST.JLS10);
        try {
            element = ((ICompilationUnit) element).getWorkingCopy(null);
            ((ICompilationUnit) element).getBuffer().setContents(formattedSource);
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
        parser.setSource((ICompilationUnit) element);

        final CompilationUnit node = (CompilationUnit) parser.createAST(new NullProgressMonitor());
        OrganizeImportsOperation operation = new OrganizeImportsOperation((ICompilationUnit) element, node, true, false, false, new IChooseImportQuery() {
            @Override
            public TypeNameMatch[] chooseImports(TypeNameMatch[][] openChoices, ISourceRange[] ranges) {
                return new TypeNameMatch[0];
            }
        });

        try {
            TextEdit textEdit = operation.createTextEdit(null);
            JavaModelUtil.applyEdit((ICompilationUnit) element, textEdit, false, new NullProgressMonitor());
            formattedSource = ((ICompilationUnit) element).getBuffer().getContents();
            try (InputStream inputStream = new ByteArrayInputStream(formattedSource.getBytes(javaFile.getCharset()))) {
                if (!javaFile.exists()) {
//                    ResourcesUtil.safelyCreateFile(javaFile, inputStream, false, new NullProgressMonitor());
                } else {
                    javaFile.setContents(inputStream, IFile.FORCE, new NullProgressMonitor());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
//        } finally {
//            IOUtils.closeQuietly(inputStream);
        }
    }
}
