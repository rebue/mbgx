package rebue.mbgx.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.codegen.mybatis3.javamapper.elements.AbstractJavaMapperMethodGenerator;

import rebue.mbgx.generator.DeleteSelectiveMethodGenerator;

/**
 * 给Mapper及其XML文件加上selectSelective()方法的插件
 * 在配置context的属性targetRuntime="MyBatis3"的方式下
 * 并不会生成selectSelective方法，本插件补充上这个方法
 * TODO MBG : 判断如果本element已经被修改过，将不重复生成
 *
 * @author zbz
 */
public class DeleteSelectivePlugin extends PluginAdapter {

    @Override
    public boolean validate(final List<String> paramList) {
        return true;
    }

    @Override
    public boolean clientGenerated(final Interface interfaze, final IntrospectedTable introspectedTable) {
        final AbstractJavaMapperMethodGenerator methodGenerator = new DeleteSelectiveMethodGenerator();
        methodGenerator.setContext(context);
        methodGenerator.setIntrospectedTable(introspectedTable);
        methodGenerator.addInterfaceElements(interfaze);
        return super.clientGenerated(interfaze, introspectedTable);
    }

}
