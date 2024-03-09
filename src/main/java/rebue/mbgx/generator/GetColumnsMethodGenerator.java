package rebue.mbgx.generator;

import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import java.util.Set;
import java.util.TreeSet;

public class GetColumnsMethodGenerator extends AbstractJavaMapperMethodGenerator {

    @Override
    public void addInterfaceElements(final Interface interfaze) {
        final FullyQualifiedJavaType arrayItemJavaType = new FullyQualifiedJavaType("org.mybatis.dynamic.sql.BasicColumn");

        final Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
        importedTypes.add(arrayItemJavaType);

        final Method method = new Method("getColumns");                                                  //$NON-NLS-1$
        method.setDefault(true);
        method.setVisibility(JavaVisibility.PUBLIC);

        final FullyQualifiedJavaType returnType = new FullyQualifiedJavaType(arrayItemJavaType + "[]");
        // returnType.addTypeArgument(arrayItemJavaType);
        method.setReturnType(returnType);

        method.addBodyLine("return selectList;");  //$NON-NLS-1$

        context.getCommentGenerator().addGeneralMethodComment(method, introspectedTable);

        if (context.getPlugins().clientSelectAllMethodGenerated(method, interfaze, introspectedTable)) {
            interfaze.addImportedTypes(importedTypes);
            interfaze.addMethod(method);
        }
    }

}
