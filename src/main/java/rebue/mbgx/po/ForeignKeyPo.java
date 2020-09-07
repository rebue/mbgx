package rebue.mbgx.po;

import lombok.Data;

/**
 * 外键信息
 * Jdbc初始化时，将解析表的外键信息信息保存到这个对象中
 * 
 * @author zbz
 *
 */
@Data
public class ForeignKeyPo {

    /**
     * 本表名称
     */
    private String  fkTableName;

    /**
     * 本表类名
     */
    private String  fkClassName;

    /**
     * 本表Bean变量的名称
     */
    private String  fkBeanName;

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

    /**
     * 外键说明的标题
     */
    private String  title;

    /**
     * 外键是否可空
     */
    private Boolean isNullable;

    /**
     * 外键的外键表是否是中间表
     */
    private Boolean isMiddleTableOnFk;

}
