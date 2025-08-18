package rebue.mbgx.generator;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import java.util.Set;
import java.util.TreeSet;

public class LogicDeleteSelectiveMethodGenerator extends AbstractJavaMapperMethodGenerator {

    public LogicDeleteSelectiveMethodGenerator() {
    }

    @Override
    public void addInterfaceElements(Interface interfaze) {
        // 检查是否存在逻辑删除标记字段（假设使用IS_DELETED）
        this.introspectedTable.getColumn("IS_DELETED").ifPresent(deletedColumn -> {
            Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();

            // 添加必要的静态导入
            importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.*"));
            importedTypes.add(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Mapper"));

            // 创建方法
            Method method = new Method("logicDeleteSelective");
            method.setDefault(true);
            method.setVisibility(JavaVisibility.PUBLIC);
            method.setReturnType(FullyQualifiedJavaType.getIntInstance());

            // 添加参数
            FullyQualifiedJavaType recordType = new FullyQualifiedJavaType(
                    introspectedTable.getBaseRecordType());
            method.addParameter(new Parameter(recordType, "record"));

            // 构建方法体
            StringBuilder body = new StringBuilder();
            body.append("return update(c -> c.set(isDeleted).equalTo(true)\n")
                    .append("        .where(");

            // 为每个字段添加条件
            boolean first = true;
            for (IntrospectedColumn column : introspectedTable.getAllColumns()) {
                if (!column.isIdentity()) {  // 排除自增ID
                    if (!first) {
                        body.append("\n        .and(");
                    } else {
                        first = false;
                    }

                    String fieldName = column.getJavaProperty();
                    body.append(column.getJavaProperty())
                            .append(", isEqualToWhenPresent(record::get")
                            .append(Character.toUpperCase(fieldName.charAt(0)))
                            .append(fieldName.substring(1))
                            .append("))");
                }
            }
            body.append(");");

            method.addBodyLine(body.toString());

            // 添加方法注释
            this.context.getCommentGenerator().addGeneralMethodComment(method, this.introspectedTable);

            // 检查插件是否允许添加方法
            if (this.context.getPlugins().clientUpdateByPrimaryKeySelectiveMethodGenerated(
                    method, interfaze, this.introspectedTable)) {
                interfaze.addImportedTypes(importedTypes);
                interfaze.addMethod(method);
            }
        });
    }
}