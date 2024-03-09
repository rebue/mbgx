package rebue.mbgx.util;

import org.mybatis.generator.api.IntrospectedColumn;

import java.sql.Types;

public class BooleanUtils {
    /**
     * 由于数据库中的字段类型一般没有或不用Boolean型<br>
     * 而java的pojo类的属性一般又直接用Boolean定义<br>
     * 所以这里做了一个规范,判断什么样的字段是属于Boolean型的
     */
    public static boolean isBooleanColumn(final IntrospectedColumn column) {
        switch (column.getJdbcType()) {
            case Types.CHAR:
            case Types.TINYINT:
                // if (column.getLength() == 1 &&
                // column.getActualColumnName().startsWith("IS_")) {
                // 上面判断字段的长度，如果是在mysql下有bug，mysql会取出长度为3，而不是1
                if (column.getActualColumnName().startsWith("IS_")) {
                    return true;
                }
        }
        return false;

    }
}
