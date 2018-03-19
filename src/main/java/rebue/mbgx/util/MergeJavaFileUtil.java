package rebue.mbgx.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Field;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.Method;
import org.jboss.forge.roaster.model.Parameter;
import org.jboss.forge.roaster.model.source.FieldHolderSource;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MemberHolderSource;
import org.jboss.forge.roaster.model.source.MemberSource;
import org.jboss.forge.roaster.model.source.MethodHolderSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeJavaFileUtil {
    private final static Logger _log = LoggerFactory.getLogger(MergeJavaFileUtil.class);

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
     */
    public static String merge(String newFileSource, File oldFile, String[] javadocTags) throws FileNotFoundException {
        JavaType<?> javaType = Roaster.parse(oldFile);
        if (javaType.isClass()) {
            JavaClassSource oldJavaClassSource = Roaster.parse(JavaClassSource.class, oldFile);
            JavaClassSource newJavaClassSource = Roaster.parse(JavaClassSource.class, newFileSource);
            if (copyMembers(oldJavaClassSource, newJavaClassSource, javadocTags)) {
                copyImports(oldJavaClassSource, newJavaClassSource);
                newFileSource = newJavaClassSource.toString();
            }
            return newFileSource;
        } else if (javaType.isInterface()) {
            JavaInterfaceSource oldJavaInterfaceSource = Roaster.parse(JavaInterfaceSource.class, oldFile);
            JavaInterfaceSource newJavaInterfaceSource = Roaster.parse(JavaInterfaceSource.class, newFileSource);
            if (copyMembers(oldJavaInterfaceSource, newJavaInterfaceSource, javadocTags)) {
                copyImports(oldJavaInterfaceSource, newJavaInterfaceSource);
                newFileSource = newJavaInterfaceSource.toString();
            }
            return newFileSource;
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
    private static boolean copyMembers(MemberHolderSource<?> oldJavaSource, MemberHolderSource<?> newJavaSource,
            String[] javadocTags) {
        // 将旧代码中非自动生成的成员（手工添加的成员代码）放入数组中
        List<MemberSource<?, ?>> newMembers = new ArrayList<>();
        for (Object item : oldJavaSource.getMembers()) {
            // 判断在Java Doc中是否有指定的注解，没有则认为是手工添加的代码
            MemberSource<?, ?> member = (MemberSource<?, ?>) item;
            JavaDocSource<?> javaDoc = member.getJavaDoc();
            if (!hasTag(javaDoc, javadocTags)) {
                newMembers.add(member);
            }
        }
        // 如果旧代码中没有非自动生成的成员（手工添加的成员代码），不需要保留和解析旧的代码，直接用新代码覆盖，所以返回false
        if (newMembers.size() == 0)
            return false;
        // 将旧代码中非自动生成的成员（手工添加的成员代码）添加到新的代码中
        for (MemberSource<?, ?> memberSource : newMembers) {
            if (memberSource instanceof Field) {
                // 旧代码中的字段
                Field<?> oldField = (Field<?>) memberSource;
                _log.debug("copyMembers: " + oldField.getName() + "," + oldField.getStringInitializer());
                FieldHolderSource<?> newFieldHolderSource = ((FieldHolderSource<?>) newJavaSource);
                // 判断新的自动生成的字段如果在旧代码中已经被人手工重写，那么从新代码中移走自动生成的这个字段，让手工重写的字段添加到新代码中
                if (!newFieldHolderSource.getFields().isEmpty() && newFieldHolderSource.hasField(oldField.getName())) {
                    // 新代码中的字段
                    Field newField = newFieldHolderSource.getField(oldField.getName());
                    newFieldHolderSource.removeField(newField);
                }
                newFieldHolderSource.addField(oldField.toString());
            } else if (memberSource instanceof Method) {
                // 旧代码中的方法
                Method<?, ?> oldMethod = (Method<?, ?>) memberSource;
                _log.debug("copyMembers: " + oldMethod.getName() + "," + oldMethod.getParameters());
                MethodHolderSource<?> newMethodHolderSource = ((MethodHolderSource<?>) newJavaSource);
                // 判断新的自动生成的方法如果在旧代码中已经被人手工重写，那么从新代码中移走自动生成的这个方法，让手工重写的方法添加到新代码中
                if (!newMethodHolderSource.getMethods().isEmpty()
                        && newMethodHolderSource.hasMethodSignature(oldMethod)) {
                    String[] params = new String[oldMethod.getParameters().size()];
                    int i = 0;
                    for (Parameter<?> paramType : oldMethod.getParameters()) {
                        params[i] = paramType.getType().getQualifiedName();
                        i++;
                    }
                    // 新代码中的方法
                    MethodSource newMethod = newMethodHolderSource.getMethod(oldMethod.getName(), params);
                    newMethodHolderSource.removeMethod(newMethod);
                }
                newMethodHolderSource.addMethod(oldMethod);
            }
        }
        return true;
    }

    /**
     * 将旧代码中非自动生成的import（手工添加的import代码）添加到新的代码中
     * 
     * @param oldJavaSource
     * @param newJavaSource
     */
    private static void copyImports(JavaSource<?> oldJavaSource, JavaSource<?> newJavaSource) {
        for (Import imprt : oldJavaSource.getImports()) {
            newJavaSource.addImport(imprt);
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
}
