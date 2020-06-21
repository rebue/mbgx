package rebue.mbgx.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import rebue.mbgx.generator.ExistSelectiveMethodGenerator;

/**
 * 给Mapper及其XML文件加上existByPrimaryKey()方法的插件
 * TODO MBG : 判断如果本element已经被修改过，将不重复生成
 *
 * @author zbz
 */
public class ExistSelectivePlugin extends PluginAdapter {

    @Override
    public boolean validate(final List<String> paramList) {
        return true;
    }

    @Override
    public boolean clientGenerated(final Interface interfaze, final IntrospectedTable introspectedTable) {
        final AbstractJavaMapperMethodGenerator methodGenerator = new ExistSelectiveMethodGenerator();
        methodGenerator.setContext(context);
        methodGenerator.setIntrospectedTable(introspectedTable);
        methodGenerator.addInterfaceElements(interfaze);
        return super.clientGenerated(interfaze, introspectedTable);
    }

}
