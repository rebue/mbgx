package rebue.mbgx.util;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;

public class IntrospectedUtils {

    /**
     * 判断是否是主键
     */
    public static final boolean isPrimaryKey(final IntrospectedColumn introspectedColumn,
            final IntrospectedTable introspectedTable) {
        for (final IntrospectedColumn item : introspectedTable.getPrimaryKeyColumns()) {
            if (item == introspectedColumn) {
                return true;
            }
        }
        return false;
    }

}
