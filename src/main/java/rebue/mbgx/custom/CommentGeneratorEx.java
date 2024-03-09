package rebue.mbgx.custom;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.CommentGenerator;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.config.MergeConstants;

import java.util.Properties;
import java.util.Set;

/**
 * 自定义的注释生成器
 * <p>
 * 启用方式：在mbg的xml配置文件中如下:<br>
 * <p>
 * &lt;commentGenerator type="rebue.mbgx.generator.MyCommentGenerator"&gt;<br>
 * ....<br>
 * ....<br>
 * &lt;/commentGenerator&gt;
 *
 * @author zbz
 */
public class CommentGeneratorEx implements CommentGenerator {

    /**
     * 从该配置中的任何属性添加此实例的属性CommentGenerator配置。
     * 这个方法将在任何其他方法之前被调用。
     */
    @Override
    public void addConfigurationProperties(final Properties properties) {
    }

    /**
     * 给成员变量(与数据库表字段相对应的属性字段)添加注释
     */
    @Override
    public void addFieldComment(final Field field, final IntrospectedTable introspectedTable, final IntrospectedColumn introspectedColumn) {
        addJavadocOfField(field, introspectedTable, introspectedColumn);
    }

    /**
     * 给成员变量(非数据库表字段的属性字段)添加注释
     */
    @Override
    public void addFieldComment(final Field field, final IntrospectedTable introspectedTable) {
        addJavadocOnlyTag(field);
    }

    /**
     * 给Model类添加注释
     */
    @Override
    public void addModelClassComment(final TopLevelClass topLevelClass, final IntrospectedTable introspectedTable) {
        addJavadocOfTable(topLevelClass, introspectedTable);
    }

    /**
     * 给类添加注释
     */
    @Override
    public void addClassComment(final InnerClass innerClass, final IntrospectedTable introspectedTable) {
        addJavadocOfTable(innerClass, introspectedTable);
    }

    /**
     * 给类添加注释
     */
    @Override
    public void addClassComment(final InnerClass innerClass, final IntrospectedTable introspectedTable, final boolean markAsDoNotDelete) {
        addClassComment(innerClass, introspectedTable);
    }

    /**
     * 给枚举添加注释
     */
    @Override
    public void addEnumComment(final InnerEnum innerEnum, final IntrospectedTable introspectedTable) {
    }

    /**
     * 给get方法添加注释
     */
    @Override
    public void addGetterComment(final Method method, final IntrospectedTable introspectedTable, final IntrospectedColumn introspectedColumn) {
        addJavadocOfField(method, introspectedTable, introspectedColumn);
    }

    /**
     * 给set方法添加注释
     */
    @Override
    public void addSetterComment(final Method method, final IntrospectedTable introspectedTable, final IntrospectedColumn introspectedColumn) {
        addJavadocOfField(method, introspectedTable, introspectedColumn);
    }

    /**
     * 给方法添加注释
     */
    @Override
    public void addGeneralMethodComment(final Method method, final IntrospectedTable introspectedTable) {
        addJavadocOnlyTag(method);
    }

    /**
     * 给Java文件添加注释
     */
    @Override
    public void addJavaFileComment(final CompilationUnit compilationUnit) {

    }

    /**
     * 在Mapper的XML文件中给每个元素结点添加注释
     */
    @Override
    public void addComment(final XmlElement xmlElement) {

    }

    /**
     * 给Mapper的XML文件添加注释
     */
    @Override
    public void addRootComment(final XmlElement rootElement) {

    }

    /**
     * 添加只有自动生成注解的文档注释
     */
    private void addJavadocOnlyTag(final JavaElement javaElement) {
        javaElement.addJavaDocLine("/**");
        addCommentLinesOfTag(javaElement);
        javaElement.addJavaDocLine(" */");
    }

    /**
     * 添加表的文档注释
     */
    private void addJavadocOfTable(final JavaElement javaElement, final IntrospectedTable introspectedTable) {
        final String remarks = introspectedTable.getRemarks().replaceAll("\\\\n", "\n");

        javaElement.addJavaDocLine("/**");

        addCommentLinesOfRemark(javaElement, remarks);

        javaElement.addJavaDocLine("*");

//        final StringBuilder sb = new StringBuilder();
//        sb.append("* 数据库表: ");
//        sb.append(introspectedTable.getFullyQualifiedTable());
//        javaElement.addJavaDocLine(sb.toString());
//
//        javaElement.addJavaDocLine("*");

        javaElement.addJavaDocLine("* " + MergeConstants.NEW_ELEMENT_TAG + " 自动生成的注释，如需修改本注释，请删除本行");

        javaElement.addJavaDocLine("*/");
    }

    /**
     * 添加字段的文档注释
     */
    private void addJavadocOfField(final JavaElement javaElement, final IntrospectedTable introspectedTable, final IntrospectedColumn introspectedColumn) {
        final String remarks = introspectedColumn.getRemarks().replaceAll("\\\\n", "\n");

        javaElement.addJavaDocLine("/**");

        addCommentLinesOfRemark(javaElement, remarks);

        javaElement.addJavaDocLine("*");

//        final StringBuilder sb = new StringBuilder();
//        sb.append("* 数据库字段: ");
//        sb.append(introspectedTable.getFullyQualifiedTable());
//        sb.append('.');
//        sb.append(introspectedColumn.getActualColumnName());
//        javaElement.addJavaDocLine(sb.toString());
//
//        javaElement.addJavaDocLine("*");

        addCommentLinesOfTag(javaElement);

        javaElement.addJavaDocLine("*/");
    }

    /**
     * 在注释中添加备注的注释行
     */
    private void addCommentLinesOfRemark(final JavaElement javaElement, final String remarks) {
        if (!StringUtils.isBlank(remarks)) {
            final String[] remarkLines = remarks.split(System.getProperty("line.separator"));
            for (final String remarkLine : remarkLines) {
                javaElement.addJavaDocLine("* " + remarkLine);
            }
        }
    }

    /**
     * 在注释中添加自动生成的注解的注释行
     */
    private void addCommentLinesOfTag(final JavaElement javaElement) {
        javaElement.addJavaDocLine("* " + MergeConstants.NEW_ELEMENT_TAG + " 自动生成，如需修改，请删除本行");
    }

    /**
     * 在一般方法的注释中添加注解(在生成Mapper文件的方法时会调用此方法)
     */
    @Override
    public void addGeneralMethodAnnotation(final Method method, final IntrospectedTable introspectedTable, final Set<FullyQualifiedJavaType> imports) {
        addJavadocOnlyTag(method);
    }

    @Override
    public void addGeneralMethodAnnotation(final Method method, final IntrospectedTable introspectedTable, final IntrospectedColumn introspectedColumn,
                                           final Set<FullyQualifiedJavaType> imports) {
        addJavadocOfField(method, introspectedTable, introspectedColumn);
    }

    @Override
    public void addFieldAnnotation(final Field field, final IntrospectedTable introspectedTable, final Set<FullyQualifiedJavaType> imports) {
        addJavadocOnlyTag(field);
    }

    @Override
    public void addFieldAnnotation(final Field field, final IntrospectedTable introspectedTable, final IntrospectedColumn introspectedColumn,
                                   final Set<FullyQualifiedJavaType> imports) {
        addJavadocOfField(field, introspectedTable, introspectedColumn);
    }

    @Override
    public void addClassAnnotation(final InnerClass innerClass, final IntrospectedTable introspectedTable, final Set<FullyQualifiedJavaType> imports) {
        // TODO Auto-generated method stub

    }

}
