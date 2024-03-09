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
public class MapperRootInterfacePlugin extends PluginAdapter {

    @Override
    public boolean validate(final List<String> paramList) {
        return true;
    }

    @Override
    public boolean clientGenerated(final Interface interfaze, final IntrospectedTable introspectedTable) {
        // 此插件只处理表有且仅有一个主键的情况
        if (introspectedTable.getPrimaryKeyColumns().size() != 1) {
            return false;
        }

        // 获取MapperRootInterface类型
        final FullyQualifiedJavaType fullyQualifiedJavaType = new FullyQualifiedJavaType("MapperRootInterface");
        interfaze.addImportedType(new FullyQualifiedJavaType("rebue.robotech.mybatis.MapperRootInterface"));

        // 添加实体类型的泛型
        FullyQualifiedJavaType parameterType = introspectedTable.getRules().calculateAllFieldsClass();
        interfaze.addImportedType(parameterType);
        fullyQualifiedJavaType.addTypeArgument(parameterType);

        // 添加ID类型的泛型
        parameterType = getPrimaryKeyType(introspectedTable);
        interfaze.addImportedType(parameterType);
        fullyQualifiedJavaType.addTypeArgument(parameterType);

        // 继承MapperRootInterface
        interfaze.addSuperInterface(fullyQualifiedJavaType);
        return true;
    }

    /**
     * 获取主键所对应的Java类型
     *
     * @param introspectedTable
     * @return
     */
    private FullyQualifiedJavaType getPrimaryKeyType(final IntrospectedTable introspectedTable) {
        return introspectedTable.getPrimaryKeyColumns().get(0).getFullyQualifiedJavaType();
    }

}
