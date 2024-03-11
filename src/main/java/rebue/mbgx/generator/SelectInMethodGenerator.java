package rebue.mbgx.generator;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SelectInMethodGenerator extends AbstractJavaMapperMethodGenerator {

    @Override
    public void addInterfaceElements(final Interface interfaze) {
        final Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
        importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
        importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isIn"));

        final Method method = new Method("selectIn");                                                  //$NON-NLS-1$
        method.setDefault(true);
        method.setVisibility(JavaVisibility.PUBLIC);

        final FullyQualifiedJavaType returnType = FullyQualifiedJavaType.getNewListInstance();
        final FullyQualifiedJavaType listType   = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());

        importedTypes.add(listType);
        returnType.addTypeArgument(listType);
        method.setReturnType(returnType);

        final List<IntrospectedColumn> introspectedColumns = introspectedTable.getPrimaryKeyColumns();
        final IntrospectedColumn       introspectedColumn  = introspectedColumns.get(0);
        final FullyQualifiedJavaType   param1Type          = FullyQualifiedJavaType.getNewListInstance();
        param1Type.addTypeArgument(introspectedColumn.getFullyQualifiedJavaType());
        method.addParameter(new Parameter(param1Type, "ids"));                                         //$NON-NLS-1$

        method.addBodyLine("return select(c -> c.where(id, isIn(ids)));");  //$NON-NLS-1$

        context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);

        if (context.getPlugins().clientSelectAllMethodGenerated(method, interfaze, introspectedTable)) {
            interfaze.addImportedTypes(importedTypes);
            interfaze.addMethod(method);
        }
    }

}
