package rebue.mbgx.generator;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class UpdateTreeCodePrefixMethodGenerator extends AbstractJavaMapperMethodGenerator {
    public UpdateTreeCodePrefixMethodGenerator() {
    }

    @Override
    public void addInterfaceElements(Interface interfaze) {
        this.introspectedTable.getColumn("TREE_CODE").ifPresent(seqNoIntrospectedColumn -> {
            Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
            importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
            importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isGreaterThan"));
            importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isBetween"));
            Method method = new Method("updateTreeCodePrefix");

            method.setDefault(true);
            method.setVisibility(JavaVisibility.PUBLIC);
            FullyQualifiedJavaType returnType = FullyQualifiedJavaType.getIntInstance();
            method.setReturnType(returnType);
            FullyQualifiedJavaType paramType1 = FullyQualifiedJavaType.getStringInstance();
            method.addParameter(new Parameter(paramType1, "tc"));
            method.addParameter(new Parameter(paramType1, "lastTc"));
            method.addBodyLine("""
                    return update(c -> c.set(treeCode).equalToConstant("CONCAT('" + lastTc + "',SUBSTRING(TREE_CODE," + (tc.length() + 1) + "))")
                            .where(treeCode, isLike(tc + "%")).and(treeCode, isNotEqualTo(tc)));
                    """);

            Method method2 = new Method("updateTreeCodePrefix");

            method2.setDefault(true);
            method2.setVisibility(JavaVisibility.PUBLIC);
            method2.setReturnType(returnType);
            List<IntrospectedColumn> introspectedColumns = this.introspectedTable.getPrimaryKeyColumns();
            IntrospectedColumn       introspectedColumn  = introspectedColumns.get(0);
            FullyQualifiedJavaType   paramType2          = FullyQualifiedJavaType.getNewListInstance();
            paramType2.addTypeArgument(introspectedColumn.getFullyQualifiedJavaType());
            method2.addParameter(new Parameter(paramType2, "ids"));
            method2.addParameter(new Parameter(paramType1, "beforeTc"));
            method2.addParameter(new Parameter(paramType1, "lastTc"));
            method2.addBodyLine("""
                    return update(c -> c.set(treeCode).equalToConstant("CONCAT('" + lastTc + "',SUBSTRING(TREE_CODE," + (beforeTc.length() + 1) + "))")
                            .where(id, isIn(ids)));
                    """);
            this.context.getCommentGenerator().addGeneralMethodComment(method, this.introspectedTable);
            this.context.getCommentGenerator().addGeneralMethodComment(method2, this.introspectedTable);
            if (this.context.getPlugins().clientSelectAllMethodGenerated(method, interfaze, this.introspectedTable)) {
                interfaze.addImportedTypes(importedTypes);
                interfaze.addMethod(method);
                interfaze.addMethod(method2);
            }
        });
    }
}
