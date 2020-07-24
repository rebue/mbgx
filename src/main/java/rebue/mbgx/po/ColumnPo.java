package rebue.mbgx.po;

import lombok.Data;

/**
 * 字段
 */
@Data
public class ColumnPo {
    /**
     * 字段名称
     */
    private String name;
    /**
     * 字段标题
     */
    private String title;
    /**
     * 字段备注
     */
    private String remark;
}
