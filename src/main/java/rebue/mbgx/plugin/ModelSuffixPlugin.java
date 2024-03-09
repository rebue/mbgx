package rebue.mbgx.plugin;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;

import java.util.List;

/**
 * 给Model类加上后缀的插件
 *
 * @author zbz
 */
public class ModelSuffixPlugin extends PluginAdapter {

    private static final String MODEL_SUFFIX_PROPERTY = "modelSuffix";

    @Override
    public boolean validate(List<String> paramList) {
        return true;
    }

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        String suffix = properties.getProperty(MODEL_SUFFIX_PROPERTY);
        if (suffix == null)
            suffix = "";
        introspectedTable.setBaseRecordType(introspectedTable.getBaseRecordType() + suffix);
    }

}
