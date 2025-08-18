package rebue.mbgx.generator;

import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import java.util.Set;
import java.util.TreeSet;

public class UpdateTreeCodeMethodGenerator extends AbstractJavaMapperMethodGenerator {
    public UpdateTreeCodeMethodGenerator() {
    }

    @Override
    public void addInterfaceElements(Interface interfaze) {
        this.introspectedTable.getColumn("TREE_CODE").ifPresent(seqNoIntrospectedColumn -> {
            Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
            importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
            importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isGreaterThan"));
            importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isBetween"));
            Method method = new Method("updateTreeCode");

            method.setDefault(true);
            method.setVisibility(JavaVisibility.PUBLIC);
            FullyQualifiedJavaType returnType = FullyQualifiedJavaType.getIntInstance();
            method.setReturnType(returnType);
            FullyQualifiedJavaType paramType = FullyQualifiedJavaType.getStringInstance();
            method.addParameter(new Parameter(paramType, "start"));
            method.addParameter(new Parameter(paramType, "end"));
            method.addParameter(new Parameter(paramType, "move"));
            method.addParameter(new Parameter(paramType, "level"));
            method.addBodyLine("""
                    return update(c -> c.set(treeCode)
                            .equalToConstant("LPAD(CAST(CAST(TREE_CODE AS UNSIGNED)" + move + "1 AS CHAR), LENGTH(TREE_CODE), '0')")
                            .where(treeCode, isBetween(start).and(end)).and(treeCode, isLike(level)));
                    """);
            this.context.getCommentGenerator().addGeneralMethodComment(method, this.introspectedTable);
            if (this.context.getPlugins().clientSelectAllMethodGenerated(method, interfaze, this.introspectedTable)) {
                interfaze.addImportedTypes(importedTypes);
                interfaze.addMethod(method);
            }
        });
    }
}
