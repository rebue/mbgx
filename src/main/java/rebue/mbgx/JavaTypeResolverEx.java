package rebue.mbgx;

import java.sql.Types;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.internal.types.JavaTypeResolverDefaultImpl;

import rebue.mbgx.util.BooleanUtil;

/**
 * 扩展了默认的Java类型解析器<br>
 * 如果数据库字段类型为char(1)--oracle，或TINYINT(1)--mysql，且名字以"IS_"开头，那么将Model类的相应属性映射成Boolean类型
 * 
 * @author zbz
 *
 */
public class JavaTypeResolverEx extends JavaTypeResolverDefaultImpl {

	@Override
	protected FullyQualifiedJavaType overrideDefaultType(IntrospectedColumn column,
			FullyQualifiedJavaType defaultType) {
		FullyQualifiedJavaType answer = super.overrideDefaultType(column, defaultType);

		if(BooleanUtil.isBooleanColumn(column)) {
			answer = typeMap.get(Types.BOOLEAN).getFullyQualifiedJavaType();
		}
		
		return answer;
	}

}
