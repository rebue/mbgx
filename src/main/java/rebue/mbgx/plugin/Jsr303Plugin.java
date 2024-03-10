package rebue.mbgx.plugin;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import rebue.mbgx.util.IntrospectedUtils;
import rebue.mbgx.util.RemarksUtils;

import java.util.List;

/**
 * 给Model类的属性加上JSR303规范的约束的插件
 *
 * @author zbz
 */
public class Jsr303Plugin extends PluginAdapter {
    @Override
    public boolean validate(final List<String> paramList) {
        return true;
    }

    @Override
    public boolean modelFieldGenerated(final Field field, final TopLevelClass topLevelClass, final IntrospectedColumn introspectedColumn, final IntrospectedTable introspectedTable,
                                       final ModelClassType modelClassType) {
        String remarks = introspectedColumn.getRemarks().replaceAll("\\\\n", "\n");
        // 如果字段要求非空
        if (!introspectedColumn.isNullable()) {
            topLevelClass.addImportedType("rebue.robotech.valid.AddGroup");
            topLevelClass.addImportedType("rebue.robotech.valid.ModifyGroup");

            // 如果是主键就加入修改组，否则加入添加组(修改时必填主键，而添加时主键可由后端生成不用填)
            final String groups = "groups = " + (IntrospectedUtils.isPrimaryKey(introspectedColumn, introspectedTable) ? "ModifyGroup.class" : "AddGroup.class");
            // 如果是字符串类型，添加 @NotBlank 注解
            if (introspectedColumn.isStringColumn()) {
                topLevelClass.addImportedType("jakarta.validation.constraints.NotBlank");
                field.addAnnotation("@NotBlank(" + groups + ", message = \"" + RemarksUtils.getTitleByRemarks(remarks) + "不能为空\")");
            }
            // 如果是非字符串类型，添加 @NotNull 注解
            else {
                topLevelClass.addImportedType("jakarta.validation.constraints.NotNull");
                field.addAnnotation("@NotNull(" + groups + ", message = \"" + RemarksUtils.getTitleByRemarks(remarks) + "不能为空\")");
            }
        }

        // 如果是字符串且有长度限制，添加 @Length 注解
        if (introspectedColumn.isStringColumn() && introspectedColumn.getLength() > 0) {
            topLevelClass.addImportedType("org.hibernate.validator.constraints.Length");
            field.addAnnotation("@Length(max = " + introspectedColumn.getLength() + ", message = \"" + RemarksUtils.getTitleByRemarks(remarks) + "的长度不能大于"
                    + introspectedColumn.getLength() + "\")");
        }

        // 如果是无符号类型，添加 @Min(...) 符号，限制为非负数
        if (introspectedColumn.getActualTypeName().contains("UNSIGNED")) {
            topLevelClass.addImportedType("jakarta.validation.constraints.PositiveOrZero");
            field.addAnnotation("@PositiveOrZero(message = \"" + RemarksUtils.getTitleByRemarks(remarks) + "不能为负数\")");
        }

        return true;
    }

}
