package rebue.mbgx.plugin;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.Plugin;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import rebue.mbgx.util.RemarksUtils;

import java.util.List;

/**
 * 给Model类加上Swagger注解的插件
 */
public class SwaggerPlugin extends PluginAdapter {

    @Override
    public boolean validate(final List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(final TopLevelClass topLevelClass, final IntrospectedTable introspectedTable) {
//        topLevelClass.addImportedType("io.swagger.annotations.ApiModel");
//        topLevelClass.addAnnotation(
//                "@ApiModel(value = \"" + topLevelClass.getType().getShortName() + "\", description = " + RemarksUtils.getSplitJointRemarks(introspectedTable.getRemarks()) + ")");
        topLevelClass.addImportedType("io.swagger.v3.oas.annotations.media.Schema");
        topLevelClass.addAnnotation("@Schema(description = " + RemarksUtils.getSplitJointRemarks(introspectedTable.getRemarks()) + ")");
        return true;
    }

    @Override
    public boolean modelFieldGenerated(final Field field, final TopLevelClass topLevelClass, final IntrospectedColumn introspectedColumn, final IntrospectedTable introspectedTable,
                                       final Plugin.ModelClassType modelClassType) {
//        topLevelClass.addImportedType("io.swagger.annotations.ApiModelProperty");
//        field.addAnnotation("@ApiModelProperty(value = " + RemarksUtils.getSplitJointRemarks(introspectedColumn.getRemarks()) + ")");
        field.addAnnotation("@Schema(description = " + RemarksUtils.getSplitJointRemarks(introspectedColumn.getRemarks()) + ")");
        return true;
    }

}
