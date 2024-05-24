package rebue.mbgx.custom;

import java.sql.Types;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.internal.types.JavaTypeResolverDefaultImpl;

import rebue.mbgx.util.BooleanUtils;

/**
 * 扩展了默认的Java类型解析器<br>
 * 1. 如果数据库字段类型为CHAR--oracle，或TINYINT--mysql和pgsql，且名字以"is_"开头，那么将Model类的相应属性映射成 Boolean 类型
 * 2. 如果数据库字段类型为OTHER，且名字以"coord"结束，那么将Model类的相应属性映射成 PGgeometry 类型
 *
 * @author zbz
 */
public class JavaTypeResolverEx extends JavaTypeResolverDefaultImpl {

    @Override
    protected FullyQualifiedJavaType overrideDefaultType(IntrospectedColumn column,
            FullyQualifiedJavaType defaultType) {
        FullyQualifiedJavaType answer = super.overrideDefaultType(column, defaultType);

        if (BooleanUtils.isBooleanColumn(column)) {
            answer = typeMap.get(Types.BOOLEAN).getFullyQualifiedJavaType();
        } else if (column.getJdbcType() == Types.OTHER) {
            String columnName = column.getActualColumnName();
            if (columnName.endsWith("coord") || columnName.endsWith("location")) {
                answer = new FullyQualifiedJavaType("net.postgis.jdbc.PGgeometry");
            }
        }

        return answer;
    }

}
