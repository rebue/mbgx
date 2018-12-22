package rebue.mbgx.plugin;

import lombok.Data;

/**
 * 模板的配置
 * CodeGenByBeetlPlugin插件读取模板的配置，保存于此对象中
 * 
 * @author zbz
 *
 */
@Data
public class TemplateCfg {
    /**
     * 模板的名称
     */
    private String  templateName;
    /**
     * 要生成文件的目录
     */
    private String  targetDir;
    /**
     * 要生成的文件
     */
    private String  targetFile;
    /**
     * 覆盖原文件前是否备份
     */
    private Boolean backup;
    /**
     * 中间表是否生成目标文件
     */
    private Boolean isGenTargetOnMiddleTable;

}