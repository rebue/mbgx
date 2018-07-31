package rebue.mbgx.plugin;

/**
 * 属性信息
 * CodeGenByBeetlPlugin插件读取数据库信息时，将解析字段的相关信息保存到这个对象中
 * 
 * @author zbz
 *
 */
public class PropInfo {
    /**
     * 字段的名称
     */
    private String  code;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsNullable() {
        return isNullable;
    }

    public void setIsNullable(Boolean isNullable) {
        this.isNullable = isNullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

}
