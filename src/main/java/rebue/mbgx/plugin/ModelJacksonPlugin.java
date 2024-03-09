package rebue.mbgx.plugin;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.util.List;

/**
 * 给Model类加上Jackson注解的插件
 */
public class ModelJacksonPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        topLevelClass.addImportedType("com.fasterxml.jackson.annotation.JsonInclude");
        topLevelClass.addImportedType("com.fasterxml.jackson.annotation.JsonInclude.Include");
        topLevelClass.addAnnotation("@JsonInclude(Include.NON_NULL)");
        return true;
    }

}
