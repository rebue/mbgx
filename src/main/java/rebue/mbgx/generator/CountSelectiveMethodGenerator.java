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

public class CountSelectiveMethodGenerator extends AbstractJavaMapperMethodGenerator {

    @Override
    public void addInterfaceElements(final Interface interfaze) {
        final Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
//        importedTypes.add(FullyQualifiedJavaType.getNewListInstance());

        final Method method = new Method("countSelective");                                     //$NON-NLS-1$
        method.setDefault(true);
        method.setVisibility(JavaVisibility.PUBLIC);

        final FullyQualifiedJavaType listType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());

        importedTypes.add(listType);
        method.setReturnType(new FullyQualifiedJavaType("long"));                               //$NON-NLS-1$

        method.addParameter(new Parameter(listType, "record"));                                 //$NON-NLS-1$

        method.addBodyLine("return count(c ->");                                                //$NON-NLS-1$
        method.addBodyLine("    c.where(id, isEqualToWhenPresent(record::getId))"); //$NON-NLS-1$
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
