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
            Method method = new Method("updateSeqNo");

            method.setDefault(true);
            method.setVisibility(JavaVisibility.PUBLIC);
            FullyQualifiedJavaType returnType = FullyQualifiedJavaType.getIntInstance();
            method.setReturnType(returnType);
            FullyQualifiedJavaType param1Type = seqNoIntrospectedColumn.getFullyQualifiedJavaType();
            FullyQualifiedJavaType param2Type = FullyQualifiedJavaType.getStringInstance();
            method.addParameter(new Parameter(param1Type, "start"));
            method.addParameter(new Parameter(param1Type, "end"));
            method.addParameter(new Parameter(param2Type, "move"));
            method.addParameter(new Parameter(param2Type, "group"));
            method.addBodyLine("""
                    return update(c -> c.set(seqNo).equalToConstant(seqNo.name() + move + "  1")
                            .where(seqNo, isBetween(start).and(end)));
                    """);
            Method method2 = new Method("updateSeqNo");
            method2.setDefault(true);
            method2.setVisibility(JavaVisibility.PUBLIC);
            method2.setReturnType(returnType);
            method2.addParameter(new Parameter(param1Type, "start"));
            method2.addParameter(new Parameter(param2Type, "move"));
            method2.addParameter(new Parameter(param2Type, "group"));
            method2.addBodyLine("""
                    return update(c -> c.set(seqNo).equalToConstant(seqNo.name() + move + "  1")
                            .where(seqNo, isGreaterThan(start)));
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
