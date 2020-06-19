package rebue.mbgx.plugin;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;
import org.mybatis.generator.codegen.mybatis3.xmlmapper.elements.AbstractXmlElementGenerator;
import rebue.mbgx.generator.ExistByPrimaryKeyElementGenerator;
import rebue.mbgx.generator.ExistByPrimaryKeyMethodGenerator;

import java.util.List;

/**
 * <pre>
 * 给Mapper及其XML文件加上existByPrimaryKey()方法的插件
 * </pre>
 *
 * @author zbz
 */
public class ExistByPrimaryKeyPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> paramList) {
        return true;
    }

    @Override
    public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
        AbstractJavaMapperMethodGenerator methodGenerator = new ExistByPrimaryKeyMethodGenerator();
        methodGenerator.setContext(context);
        methodGenerator.setIntrospectedTable(introspectedTable);
        methodGenerator.addInterfaceElements(interfaze);
        return super.clientGenerated(interfaze, introspectedTable);
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        AbstractXmlElementGenerator elementGenerator = new ExistByPrimaryKeyElementGenerator();
        elementGenerator.setContext(context);
        elementGenerator.setIntrospectedTable(introspectedTable);
        elementGenerator.addElements(document.getRootElement());
        return super.sqlMapDocumentGenerated(document, introspectedTable);
    }

}
