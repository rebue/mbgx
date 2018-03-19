package rebue.mbgx.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.Plugin;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import rebue.mbgx.util.RemarksUtil;

/**
 * 给Model类加上Swagger注解的插件
 */
public class SwaggerPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        topLevelClass.addImportedType("io.swagger.annotations.ApiModel");
        topLevelClass.addAnnotation("@ApiModel(value = \"" + topLevelClass.getType().getShortName()
                + "\", description = " + RemarksUtil.getSplitJointRemarks(introspectedTable.getRemarks()) + ")");
        return true;
    }

    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
            IntrospectedTable introspectedTable, Plugin.ModelClassType modelClassType) {
        topLevelClass.addImportedType("io.swagger.annotations.ApiModelProperty");
        field.addAnnotation(
                "@ApiModelProperty(value = " + RemarksUtil.getSplitJointRemarks(introspectedColumn.getRemarks()) + ")");
        return true;
    }

}
