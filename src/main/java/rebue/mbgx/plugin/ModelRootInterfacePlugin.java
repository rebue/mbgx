package rebue.mbgx.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * 给Model类加上继承的父接口的插件
 * XXX MBG : 注意：此插件只处理表有且仅有一个主键的情况
 *
 * @author zbz
 */
public class ModelRootInterfacePlugin extends PluginAdapter {

    @Override
    public boolean validate(final List<String> paramList) {
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(final TopLevelClass topLevelClass, final IntrospectedTable introspectedTable) {
        // 此插件只处理表有且仅有一个主键的情况
        if (introspectedTable.getPrimaryKeyColumns().size() != 1) {
            return false;
        }

        // 获取ModelRootInterface类型
        final FullyQualifiedJavaType fullyQualifiedJavaType = new FullyQualifiedJavaType("Mo");
        topLevelClass.addImportedType(new FullyQualifiedJavaType("rebue.robotech.mo.Mo"));

        // 添加ID类型的泛型
        final FullyQualifiedJavaType idType = getPrimaryKeyType(introspectedTable);
        final FullyQualifiedJavaType parameterType = idType;
        topLevelClass.addImportedType(parameterType);
        fullyQualifiedJavaType.addTypeArgument(parameterType);

        // 继承ModelRootInterface
        topLevelClass.addSuperInterface(fullyQualifiedJavaType);

        final Method method = new Method("getIdType");
        method.addJavaDocLine("/**");
        method.addJavaDocLine("* 获取ID的类型");
        method.addJavaDocLine("*");
        method.addJavaDocLine("* @mbg.generated 自动生成，如需修改，请删除本行");
        method.addJavaDocLine("*/");
        method.addAnnotation("@Override");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(new FullyQualifiedJavaType("java.lang.String"));
        method.addBodyLine("return \"" + idType.getShortName() + "\";");
        topLevelClass.addMethod(method);
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
