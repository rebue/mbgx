package rebue.mbgx.generator;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import java.util.Set;
import java.util.TreeSet;

public class DeleteSelectiveMethodGenerator extends AbstractJavaMapperMethodGenerator {

    @Override
    public void addInterfaceElements(final Interface interfaze) {
        final Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
        importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
        importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isEqualToWhenPresent"));

        final Method method = new Method("deleteSelective");                                            //$NON-NLS-1$
        method.setDefault(true);
        method.setVisibility(JavaVisibility.PUBLIC);

        final FullyQualifiedJavaType returnType = new FullyQualifiedJavaType("int");
        final FullyQualifiedJavaType recordType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());

        method.setReturnType(returnType);
        method.addParameter(new Parameter(recordType, "record"));                                         //$NON-NLS-1$

        method.addBodyLine("return delete(c ->");                                                       //$NON-NLS-1$
        method.addBodyLine("    c.where(id, isEqualToWhenPresent(record::getId))");         //$NON-NLS-1$
        for (final IntrospectedColumn column : introspectedTable.getNonPrimaryKeyColumns()) {
            method.addBodyLine("    .and(" + column.getJavaProperty() + ", isEqualToWhenPresent(record::get" + StringUtils.capitalize(column.getJavaProperty()) + "))"); //$NON-NLS-1$
        }
        method.addBodyLine(");");

        context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);

        if (context.getPlugins().clientSelectAllMethodGenerated(method, interfaze, introspectedTable)) {
            interfaze.addImportedTypes(importedTypes);
            interfaze.addMethod(method);
        }
    }

}
