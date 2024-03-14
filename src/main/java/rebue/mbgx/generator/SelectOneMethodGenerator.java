package rebue.mbgx.generator;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import java.util.Set;
import java.util.TreeSet;

public class SelectOneMethodGenerator extends AbstractJavaMapperMethodGenerator {

    @Override
    public void addInterfaceElements(final Interface interfaze) {
        final Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
        importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
        importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isEqualToWhenPresent"));
        importedTypes.add(new FullyQualifiedJavaType("org.mybatis.dynamic.sql.DerivedColumn"));

        final Method method = new Method("selectOne");                                                  //$NON-NLS-1$
        method.setDefault(true);
        method.setVisibility(JavaVisibility.PUBLIC);

        final FullyQualifiedJavaType returnType   = new FullyQualifiedJavaType("java.util.Optional");
        final FullyQualifiedJavaType optionalType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());

        importedTypes.add(optionalType);
        returnType.addTypeArgument(optionalType);
        method.setReturnType(returnType);

        method.addParameter(new Parameter(optionalType, "record"));                                         //$NON-NLS-1$

        method.addBodyLine("return selectOne(c ->");                                                       //$NON-NLS-1$
        method.addBodyLine("    c.where(DerivedColumn.of(\"1\"), isEqualTo(1)).and(id, isEqualToWhenPresent(record::getId))");         //$NON-NLS-1$
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
