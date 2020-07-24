package rebue.mbgx.po;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

/**
 * 表信息
 */
@Data
public class TablePo {
    /**
     * 表名
     */
    private String name;

    /**
     * 是否中间表
     */
    private Boolean isMiddleTable = false;

    /**
     * 字段列表
     */
    private List<ColumnPo> columns = new LinkedList<>();

    /**
     * 外键列表
     */
    private List<ForeignKeyPo> foreignKeys = new LinkedList<>();
}
