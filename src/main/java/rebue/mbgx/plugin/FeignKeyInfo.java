package rebue.mbgx.plugin;

import lombok.Data;

/**
 * 外键信息 CodeGenByBeetlPlugin插件读取数据库信息时，将解析表的外键信息信息保存到这个对象中
 * 
 * @author zbz
 *
 */
@Data
public class FeignKeyInfo {

    /**
     * 本表外键字段的名称
     */
    private String  fkFieldName;

    /**
     * 关联表名
     */
    private String  pkTableName;

    /**
     * 关联表类名
     */
    private String  pkClassName;

    /**
     * 关联表Bean变量的名称
     */
    private String  pkBeanName;

    /**
     * 关联表字段的名称
     */
    private String  pkFieldName;

    private Boolean isNullable;

}
