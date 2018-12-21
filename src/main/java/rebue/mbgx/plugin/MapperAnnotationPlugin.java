package rebue.mbgx.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rebue.mbgx.util.RemarksUtils;

/**
 * 给Mapper类加上@Mapper的插件
 * 
 * @author zbz
 */
public class MapperAnnotationPlugin extends PluginAdapter {

    private final static Logger _log = LoggerFactory.getLogger(MapperAnnotationPlugin.class);

    @Override
    public boolean validate(List<String> paramList) {
        return true;
    }

    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        _log.info("\n{}\n{}:{}\n{}\n", "-----------------------------------------------",//
                introspectedTable.getFullyQualifiedTableNameAtRuntime(), //
                RemarksUtils.getTitleByRemarks(introspectedTable.getRemarks()),//
                "-----------------------------------------------");
        interfaze.addImportedType(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Mapper"));
        interfaze.addAnnotation("@Mapper");
        return true;
    }

}
