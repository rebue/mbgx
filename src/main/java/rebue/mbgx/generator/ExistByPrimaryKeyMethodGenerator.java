package rebue.mbgx.generator;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ExistByPrimaryKeyMethodGenerator extends AbstractJavaMapperMethodGenerator {

    @Override
    public void addInterfaceElements(final Interface interfaze) {
        final Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
        importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
        importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo"));
        final Method method = new Method("existByPrimaryKey");                      //$NON-NLS-1$
        method.setDefault(true);
        method.setVisibility(JavaVisibility.PUBLIC);

        method.setReturnType(FullyQualifiedJavaType.getBooleanPrimitiveInstance());

        final List<IntrospectedColumn> introspectedColumns = introspectedTable.getPrimaryKeyColumns();
        final boolean                  annotate            = introspectedColumns.size() > 1;
        if (annotate) {
            importedTypes.add(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Param")); //$NON-NLS-1$
        }
        final StringBuilder sb = new StringBuilder();
        for (final IntrospectedColumn introspectedColumn : introspectedColumns) {
            final FullyQualifiedJavaType type = introspectedColumn.getFullyQualifiedJavaType();
            importedTypes.add(type);
            final Parameter parameter = new Parameter(type, introspectedColumn.getJavaProperty() + "_");
            if (annotate) {
                sb.setLength(0);
                sb.append("@Param(\""); //$NON-NLS-1$
                sb.append(introspectedColumn.getJavaProperty());
                sb.append("\")"); //$NON-NLS-1$
                parameter.addAnnotation(sb.toString());
            }
            method.addParameter(parameter);
        }

        method.addBodyLine("return count(c -> c.where(id, isEqualTo(id_))) > 0;");  //$NON-NLS-1$

        context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);

        if (context.getPlugins().clientSelectByPrimaryKeyMethodGenerated(method, interfaze, introspectedTable)) {
            interfaze.addImportedTypes(importedTypes);
            interfaze.addMethod(method);
        }
    }

}
