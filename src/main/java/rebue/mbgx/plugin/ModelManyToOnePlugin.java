package rebue.mbgx.plugin;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import rebue.mbgx.po.ForeignKeyPo;
import rebue.mbgx.util.JdbcUtils;

import java.util.List;

/**
 * 给Model类加上多对一属性的插件
 */
@Slf4j
public class ModelManyToOnePlugin extends PluginAdapter {

    @Override
    public boolean validate(final List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(final TopLevelClass topLevelClass, final IntrospectedTable introspectedTable) {
        JdbcUtils.init(context);

        String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();

        topLevelClass.addImportedType("lombok.Getter");
        for (final ForeignKeyPo foreignKey : JdbcUtils.getForeignKeyList()) {
            if (foreignKey.getFkTableName().equalsIgnoreCase(tableName)) {
                FullyQualifiedJavaType clazz = new FullyQualifiedJavaType(topLevelClass.getType().getPackageName()
                        + "." + foreignKey.getPkClassName() + "Mo");
                final Field field = new Field(foreignKey.getPkBeanName(), clazz);
                field.addJavaDocLine("/**");
                field.addJavaDocLine("*");
                field.addJavaDocLine("* " + foreignKey.getTitle());
                field.addJavaDocLine("*");
                field.addJavaDocLine("* @mbg.generated 自动生成的注释，如需修改本注释，请删除本行");
                field.addJavaDocLine("*/");
                field.addAnnotation("@Getter");
                field.setVisibility(JavaVisibility.PRIVATE);
                topLevelClass.addField(field);
            }
        }

        return true;
    }

}
