package rebue.mbgx.generator;

import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import java.util.Set;
import java.util.TreeSet;

public class UpdateSeqNoMethodGenerator extends AbstractJavaMapperMethodGenerator {
    public UpdateSeqNoMethodGenerator() {
    }

    @Override
    public void addInterfaceElements(Interface interfaze) {
        this.introspectedTable.getColumn("SEQ_NO").ifPresent(seqNoIntrospectedColumn -> {
            Set<FullyQualifiedJavaType> importedTypes = new TreeSet<>();
            importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
            importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isGreaterThan"));
            importedTypes.add(new FullyQualifiedJavaType("static org.mybatis.dynamic.sql.SqlBuilder.isBetween"));

            FullyQualifiedJavaType param1Type = seqNoIntrospectedColumn.getFullyQualifiedJavaType();
            FullyQualifiedJavaType param2Type = FullyQualifiedJavaType.getStringInstance();
            FullyQualifiedJavaType param3Type = new FullyQualifiedJavaType("org.mybatis.dynamic.sql.SqlColumn<Long>");
            FullyQualifiedJavaType param4Type = new FullyQualifiedJavaType("Long");
            FullyQualifiedJavaType returnType = FullyQualifiedJavaType.getIntInstance();

            Method method1 = new Method("updateSeqNo");
            method1.setDefault(true);
            method1.setVisibility(JavaVisibility.PUBLIC);
            method1.setReturnType(returnType);
            method1.addParameter(new Parameter(param1Type, "start"));
            method1.addParameter(new Parameter(param2Type, "move"));
            method1.addBodyLine("""
                    return update(c -> c.set(seqNo).equalToConstant(seqNo.name() + move + "  1")
                            .where(seqNo, isGreaterThan(start)));
                    """);

            Method method2 = new Method("updateSeqNo");
            method2.setDefault(true);
            method2.setVisibility(JavaVisibility.PUBLIC);
            method2.setReturnType(returnType);
            method2.addParameter(new Parameter(param1Type, "start"));
            method2.addParameter(new Parameter(param1Type, "end"));
            method2.addParameter(new Parameter(param2Type, "move"));
            method2.addBodyLine("""
                    return update(c -> c.set(seqNo).equalToConstant(seqNo.name() + move + "  1")
                            .where(seqNo, isBetween(start).and(end)));
                    """);

            Method method3 = new Method("updateSeqNo");
            method3.setDefault(true);
            method3.setVisibility(JavaVisibility.PUBLIC);
            method3.setReturnType(returnType);
            method3.addParameter(new Parameter(param1Type, "start"));
            method3.addParameter(new Parameter(param2Type, "move"));
            method3.addParameter(new Parameter(param3Type, "parentIdName"));
            method3.addParameter(new Parameter(param4Type, "parentIdValue"));
            method3.addBodyLine("""
                    return update(c -> c.set(seqNo).equalToConstant(seqNo.name() + move + "  1")
                            .where(seqNo, isGreaterThan(start)).and(parentIdName, isEqualTo(parentIdValue)));
                    """);

            Method method4 = new Method("updateSeqNo");
            method4.setDefault(true);
            method4.setVisibility(JavaVisibility.PUBLIC);
            method4.setReturnType(returnType);
            method4.addParameter(new Parameter(param1Type, "start"));
            method4.addParameter(new Parameter(param1Type, "end"));
            method4.addParameter(new Parameter(param2Type, "move"));
            method4.addParameter(new Parameter(param3Type, "parentIdName"));
            method4.addParameter(new Parameter(param4Type, "parentIdValue"));
            method4.addBodyLine("""
                    return update(c -> c.set(seqNo).equalToConstant(seqNo.name() + move + "  1")
                            .where(seqNo, isBetween(start).and(end)).and(parentIdName, isEqualTo(parentIdValue)));
                    """);

            this.context.getCommentGenerator().addGeneralMethodComment(method1, this.introspectedTable);
            this.context.getCommentGenerator().addGeneralMethodComment(method2, this.introspectedTable);
            this.context.getCommentGenerator().addGeneralMethodComment(method3, this.introspectedTable);
            this.context.getCommentGenerator().addGeneralMethodComment(method4, this.introspectedTable);
            if (this.context.getPlugins().clientSelectAllMethodGenerated(method1, interfaze, this.introspectedTable)) {
                interfaze.addImportedTypes(importedTypes);
                interfaze.addMethod(method1);
                interfaze.addMethod(method2);
                interfaze.addMethod(method3);
                interfaze.addMethod(method4);
            }
        });
    }
}
