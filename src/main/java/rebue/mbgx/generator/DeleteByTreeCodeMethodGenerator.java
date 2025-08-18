package rebue.mbgx.generator;

import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import java.util.Set;
import java.util.TreeSet;

public class DeleteByTreeCodeMethodGenerator extends AbstractJavaMapperMethodGenerator {
    public DeleteByTreeCodeMethodGenerator() {
    }

    @Override
    public void addInterfaceElements(Interface interfaze) {
        this.introspectedTable.getColumn("TREE_CODE").ifPresent(seqNoIntrospectedColumn -> {
            Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
            importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
            importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isGreaterThan"));
            importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isBetween"));
            Method method = new Method("deleteByTreeCode");

            method.setDefault(true);
            method.setVisibility(JavaVisibility.PUBLIC);
            FullyQualifiedJavaType returnType = FullyQualifiedJavaType.getIntInstance();
            method.setReturnType(returnType);
            FullyQualifiedJavaType paramType = FullyQualifiedJavaType.getStringInstance();
            method.addParameter(new Parameter(paramType, "tc"));
            method.addBodyLine("""
                    return delete(d -> d.where(treeCode, isLike(tc + "%")));
                    """);
            this.context.getCommentGenerator().addGeneralMethodComment(method, this.introspectedTable);
            if (this.context.getPlugins().clientSelectAllMethodGenerated(method, interfaze, this.introspectedTable)) {
                interfaze.addImportedTypes(importedTypes);
                interfaze.addMethod(method);
            }
        });
    }
}
