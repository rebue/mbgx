package rebue.mbgx.generator;

import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

public class SelectOneMethodGenerator extends AbstractJavaMapperMethodGenerator {

    @Override
    public void addInterfaceElements(final Interface interfaze) {
        final Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
        importedTypes.add(FullyQualifiedJavaType.getNewListInstance());

        final Method method = new Method("selectOne");                                                  //$NON-NLS-1$
        method.setDefault(true);
        method.setVisibility(JavaVisibility.PUBLIC);

        final FullyQualifiedJavaType returnType = new FullyQualifiedJavaType("java.util.Optional");
        final FullyQualifiedJavaType optionalType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());

        importedTypes.add(optionalType);
        returnType.addTypeArgument(optionalType);
        method.setReturnType(returnType);

        method.addParameter(new Parameter(optionalType, "record"));                                         //$NON-NLS-1$

        importedTypes.add(new FullyQualifiedJavaType("java.util.Objects"));                             //$NON-NLS-1$
        method.addBodyLine("return selectOne(c ->");                                                       //$NON-NLS-1$
        method.addBodyLine("    c.where(id, isEqualTo(record::getId).when(Objects::nonNull))");         //$NON-NLS-1$
        for (final IntrospectedColumn column : introspectedTable.getNonPrimaryKeyColumns()) {
            method.addBodyLine("    .and(" + column.getJavaProperty() + ", isEqualTo(record::get" + StringUtils.capitalize(column.getJavaProperty()) + ").when(Objects::nonNull))"); //$NON-NLS-1$
        }
        method.addBodyLine(");");

        context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);

        if (context.getPlugins().clientSelectAllMethodGenerated(method, interfaze, introspectedTable)) {
            interfaze.addImportedTypes(importedTypes);
            interfaze.addMethod(method);
        }
    }

}
