package rebue.mbgx.plugin;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;

import java.util.List;

/**
 * 给Mapper类加上继承的父接口的插件
 * XXX MBG : 注意：此插件只处理表有且仅有一个主键的情况
 *
 * @author zbz
 */
public class MapperRootIntfacePlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> paramList) {
        return true;
    }

    @Override
    public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
        // 此插件只处理表有且仅有一个主键的情况
        if (introspectedTable.getPrimaryKeyColumns().size() != 1)
            return false;

        // 获取MybatisBaseMapper类型
        FullyQualifiedJavaType fullyQualifiedJavaType = new FullyQualifiedJavaType("MybatisBaseMapper");
        interfaze.addImportedType(new FullyQualifiedJavaType("rebue.robotech.mapper.MybatisBaseMapper"));

        // 添加实体类型的泛型
        FullyQualifiedJavaType parameterType = introspectedTable.getRules().calculateAllFieldsClass();
        interfaze.addImportedType(parameterType);
        fullyQualifiedJavaType.addTypeArgument(parameterType);

        // 添加ID类型的泛型
        parameterType = getPrimaryKeyType(introspectedTable);
        interfaze.addImportedType(parameterType);
        fullyQualifiedJavaType.addTypeArgument(parameterType);

        // 继承MybatisBaseMapper
        interfaze.addSuperInterface(fullyQualifiedJavaType);
        return true;
    }

    /**
     * 获取主键所对应的Java类型
     *
     * @param introspectedTable
     * @return
     */
    private FullyQualifiedJavaType getPrimaryKeyType(IntrospectedTable introspectedTable) {
        return introspectedTable.getPrimaryKeyColumns().get(0).getFullyQualifiedJavaType();
    }

}
