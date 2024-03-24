package rebue.mbgx.plugin;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import rebue.mbgx.util.PojoMetasUtils;
import rebue.wheel.core.db.meta.ForeignKeyMeta;
import rebue.wheel.core.db.meta.PojoMeta;

import java.util.List;

/**
 * 给Model类加上多对一属性的插件
 */
public class ModelManyToOnePlugin extends PluginAdapter {

    @Override
    public boolean validate(final List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(final TopLevelClass topLevelClass, final IntrospectedTable introspectedTable) {
        PojoMetasUtils.init(context);

        final String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();

        boolean isImport = false;
        for (PojoMeta pojoMeta : PojoMetasUtils.getPojoMetas()) {
            for (ForeignKeyMeta foreignKey : pojoMeta.getTable().getForeignKeys()) {
                if (foreignKey.getFkTableName().equalsIgnoreCase(tableName)) {
                    final FullyQualifiedJavaType clazz = new FullyQualifiedJavaType(topLevelClass.getType().getPackageName()
                            + "." + pojoMeta.getClassName() + "Mo");
                    final Field field = new Field(pojoMeta.getInstanceName(), clazz);
                    field.addJavaDocLine("/**");
                    field.addJavaDocLine("*");
                    field.addJavaDocLine("* " + pojoMeta.getTitle());
                    field.addJavaDocLine("*");
                    field.addJavaDocLine("* @mbg.generated 自动生成的注释，如需修改本注释，请删除本行");
                    field.addJavaDocLine("*/");
                    field.addAnnotation("@Getter");
                    field.addAnnotation("@Setter");
                    field.setVisibility(JavaVisibility.PRIVATE);
                    topLevelClass.addField(field);
                    isImport = true;
                }
            }
        }

        if (isImport) {
            topLevelClass.addImportedType("lombok.Getter");
            topLevelClass.addImportedType("lombok.Setter");
        }

        return true;
    }

}
