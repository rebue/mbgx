package rebue.mbgx.custom;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.CommentGenerator;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.InnerEnum;
import org.mybatis.generator.api.dom.java.JavaElement;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.config.MergeConstants;

/**
 * 自定义的注释生成器
 * 
 * 启用方式：在mbg的xml配置文件中如下:
 * 
 * <commentGenerator type="rebue.mbgx.generator.MyCommentGenerator">
 * ....
 * ....
 * </commentGenerator>
 * 
 * @author zbz
 *
 */
public class CommentGeneratorEx implements CommentGenerator {

    /**
     * 从该配置中的任何属性添加此实例的属性CommentGenerator配置。
     * 这个方法将在任何其他方法之前被调用。
     */
    @Override
    public void addConfigurationProperties(Properties properties) {
    }

    /**
     * 给成员变量(与数据库表字段相对应的属性字段)添加注释
     */
    @Override
    public void addFieldComment(Field field, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
        addJavadocOfField(field, introspectedTable, introspectedColumn);
    }

    /**
     * 给成员变量(非数据库表字段的属性字段)添加注释
     */
    @Override
    public void addFieldComment(Field field, IntrospectedTable introspectedTable) {
        addJavadocOnlyTag(field);
    }

    /**
     * 给Model类添加注释
     */
    @Override
    public void addModelClassComment(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        addJavadocOfTable(topLevelClass, introspectedTable);
    }

    /**
     * 给类添加注释
     */
    @Override
    public void addClassComment(InnerClass innerClass, IntrospectedTable introspectedTable) {
        addJavadocOfTable(innerClass, introspectedTable);
    }

    /**
     * 给类添加注释
     */
    @Override
    public void addClassComment(InnerClass innerClass, IntrospectedTable introspectedTable, boolean markAsDoNotDelete) {
        addClassComment(innerClass, introspectedTable);
    }

    /**
     * 给枚举添加注释
     */
    @Override
    public void addEnumComment(InnerEnum innerEnum, IntrospectedTable introspectedTable) {
    }

    /**
     * 给get方法添加注释
     */
    @Override
    public void addGetterComment(Method method, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
        addJavadocOfField(method, introspectedTable, introspectedColumn);
    }

    /**
     * 给set方法添加注释
     */
    @Override
    public void addSetterComment(Method method, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
        addJavadocOfField(method, introspectedTable, introspectedColumn);
    }

    /**
     * 给方法添加注释
     */
    @Override
    public void addGeneralMethodComment(Method method, IntrospectedTable introspectedTable) {
        addJavadocOnlyTag(method);
    }

    /**
     * 给Java文件添加注释
     */
    @Override
    public void addJavaFileComment(CompilationUnit compilationUnit) {

    }

    /**
     * 在Mapper的XML文件中给每个元素结点添加注释
     */
    @Override
    public void addComment(XmlElement xmlElement) {
        xmlElement.addElement(new TextElement("<!--")); //$NON-NLS-1$

        StringBuilder sb = new StringBuilder();
        sb.append("  WARNING - "); //$NON-NLS-1$
        sb.append(MergeConstants.NEW_ELEMENT_TAG);
        xmlElement.addElement(new TextElement(sb.toString()));
        xmlElement.addElement(new TextElement("  This element is automatically generated by MyBatis Generator, do not modify.")); //$NON-NLS-1$

        xmlElement.addElement(new TextElement("-->")); //$NON-NLS-1$
    }

    /**
     * 给Mapper的XML文件添加注释
     */
    @Override
    public void addRootComment(XmlElement rootElement) {

    }

    /**
     * 添加只有自动生成注解的文档注释
     */
    private void addJavadocOnlyTag(JavaElement javaElement) {
        javaElement.addJavaDocLine("/**");
        addCommentLinesOfTag(javaElement);
        javaElement.addJavaDocLine(" */");
    }

    /**
     * 添加表的文档注释
     */
    private void addJavadocOfTable(JavaElement javaElement, IntrospectedTable introspectedTable) {
        String remarks = introspectedTable.getRemarks();

        javaElement.addJavaDocLine("/**");

        addCommentLinesOfRemark(javaElement, remarks);

        javaElement.addJavaDocLine("");

        StringBuilder sb = new StringBuilder();
        sb.append("数据库表: ");
        sb.append(introspectedTable.getFullyQualifiedTable());
        javaElement.addJavaDocLine(sb.toString());

        javaElement.addJavaDocLine("");

        addCommentLinesOfTag(javaElement);

        javaElement.addJavaDocLine("");

        javaElement.addJavaDocLine("*/");
    }

    /**
     * 添加字段的文档注释
     */
    private void addJavadocOfField(JavaElement javaElement, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
        String remarks = introspectedColumn.getRemarks();

        javaElement.addJavaDocLine("/**");

        addCommentLinesOfRemark(javaElement, remarks);

        javaElement.addJavaDocLine("");

        StringBuilder sb = new StringBuilder();
        sb.append("数据库字段: ");
        sb.append(introspectedTable.getFullyQualifiedTable());
        sb.append('.');
        sb.append(introspectedColumn.getActualColumnName());
        javaElement.addJavaDocLine(sb.toString());

        javaElement.addJavaDocLine("");

        addCommentLinesOfTag(javaElement);

        javaElement.addJavaDocLine("");

        javaElement.addJavaDocLine("*/");
    }

    /**
     * 在注释中添加备注的注释行
     */
    private void addCommentLinesOfRemark(JavaElement javaElement, String remarks) {
        if (!StringUtils.isBlank(remarks)) {
            String[] remarkLines = remarks.split(System.getProperty("line.separator"));
            for (String remarkLine : remarkLines) {
                javaElement.addJavaDocLine(remarkLine);
            }
        }
    }

    /**
     * 在注释中添加自动生成的注解的注释行
     */
    private void addCommentLinesOfTag(JavaElement javaElement) {
        javaElement.addJavaDocLine(MergeConstants.NEW_ELEMENT_TAG + " 自动生成，如需修改，请删除本行");
    }

}
