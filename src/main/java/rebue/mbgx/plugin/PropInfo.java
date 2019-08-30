package rebue.mbgx.plugin;

import lombok.Data;

/**
 * 属性信息 CodeGenByBeetlPlugin插件读取数据库信息时，将解析字段的相关信息保存到这个对象中
 * 
 * @author zbz
 *
 */
@Data
public class PropInfo {

    /**
     * 字段的名称
     */
    private String  code;

    /**
     * 源字段
     */
    private String  sourceCode;

    /**
     * 字段的注释
     */
    private String  name;

    /**
     * 字段的类型（String/Date/Time...）
     */
    private String  type;

    /**
     * 字段是否可空
     */
    private Boolean isNullable;

    /**
     * 字段默认值
     */
    private String  defaultValue;

    /**
     * 字段长度
     */
    private Integer length;

    /**
     * 字段比例
     */
    private Integer scale;

    /**
     * 备注
     */
    private String  remark;
}
