package rebue.mbgx.plugin;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import rebue.mbgx.util.RemarksUtils;

import java.util.List;

/**
 * 给Model类的属性加上JSR303规范的约束的插件
 *
 * @author zbz
 */
@Slf4j
public class Jsr303Plugin extends PluginAdapter {
    @Override
    public boolean validate(final List<String> paramList) {
        return true;
    }

    @Override
    public boolean modelFieldGenerated(final Field field, final TopLevelClass topLevelClass, final IntrospectedColumn introspectedColumn, final IntrospectedTable introspectedTable,
                                       final ModelClassType modelClassType) {
        // 如果字段要求非空
        if (!introspectedColumn.isNullable()) {
            topLevelClass.addImportedType("rebue.robotech.valid.AddGroup");
            topLevelClass.addImportedType("rebue.robotech.valid.ModifyGroup");

            String groups = "groups = " + (introspectedColumn.isIdentity() ? "ModifyGroup.class" : "{AddGroup.class, ModifyGroup.class}");
            // 如果是字符串类型，添加 @NotBlank 注解
            if (introspectedColumn.isStringColumn()) {
                topLevelClass.addImportedType("javax.validation.constraints.NotBlank");
                field.addAnnotation("@NotBlank(" + groups + ", message = \"" + RemarksUtils.getTitleByRemarks(introspectedColumn.getRemarks()) + "不能为空\")");
            }
            // 如果是非字符串类型，添加 @NotNull 注解
            else {
                topLevelClass.addImportedType("javax.validation.constraints.NotNull");
                field.addAnnotation("@NotNull(" + groups + ", message = \"" + RemarksUtils.getTitleByRemarks(introspectedColumn.getRemarks()) + "不能为空\")");
            }
        }

        // 如果是字符串且有长度限制，添加 @Length 注解
        if (introspectedColumn.isStringColumn() && introspectedColumn.getLength() > 0) {
            topLevelClass.addImportedType("org.hibernate.validator.constraints.Length");
            field.addAnnotation("@Length(max = " + introspectedColumn.getLength() + ", message = \"" + RemarksUtils.getTitleByRemarks(introspectedColumn.getRemarks()) + "的长度不能大于"
                    + introspectedColumn.getLength() + "\")");
        }

        // 如果是无符号类型，添加 @Min(...) 符号，限制为非负数
        if (introspectedColumn.getActualTypeName().contains("UNSIGNED")) {
            topLevelClass.addImportedType("javax.validation.constraints.Min");
            field.addAnnotation("@Min(value = 0, message = \"" + RemarksUtils.getTitleByRemarks(introspectedColumn.getRemarks()) + "不能为非负数\")");
        }

        return true;
    }

}
