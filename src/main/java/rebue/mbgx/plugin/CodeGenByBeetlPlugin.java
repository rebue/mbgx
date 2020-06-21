package rebue.mbgx.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.config.JDBCConnectionConfiguration;
import org.mybatis.generator.internal.util.JavaBeansUtil;

import com.alibaba.fastjson.JSON;
import com.google.common.io.Files;

import lombok.extern.slf4j.Slf4j;
import rebue.mbgx.util.JavaSourceUtils;
import rebue.mbgx.util.MergeJavaFileUtils;
import rebue.mbgx.util.PathUtils;
import rebue.mbgx.util.RemarksUtils;

/**
 * 利用beetl生成代码的插件
 *
 * @author zbz
 */
@Slf4j
public class CodeGenByBeetlPlugin extends PluginAdapter {

    /**
     * beetl的配置文件（位于classpath下的路径）
     */
    private static final String        BEETL_CFG_FILE           = "beetlCfgFile";
    /**
     * beetl的模板文件（位于模板目录下的路径），多个文件用逗号相隔
     */
    private static final String        BEETL_TEMPLATES_CFG_FILE = "templatesCfgFile";
    /**
     * beetl模板生成文件的模块路径（用在模板的配置文件中指定java生成文件的路径）
     */
    private static final String        BEETL_MODULE_PATH        = "beetlModulePath";
    /**
     * beetl模板生成文件的模块名称（用在模板配置文件中指定jsp/js/css等生成文件的路径）
     */
    private static final String        BEETL_MODULE_NAME        = "beetlModuleName";
    /**
     * 中间表的List(item为表名字符串)
     */
    private final List<String>         _middleTableList         = new LinkedList<>();
    /**
     * 外键的List
     */
    private final List<ForeignKeyInfo> _foreignKeyList          = new LinkedList<>();
    /**
     * 用来获取beetl模板的groupTemplate
     */
    private GroupTemplate              _groupTemplate;
    /**
     * 模块的包，用来注入模板，获取beetlModulePath后将/替换为.就可以得到
     */
    private String                     _modulePackage;

    @Override
    public boolean validate(final List<String> paramList) {
        try {
            log.info("1. 取beetl的配置");
            final Configuration cfg = new Configuration();
            final String beetlCfgFile = properties.getProperty(BEETL_CFG_FILE);
            if (beetlCfgFile != null) {
                cfg.add(beetlCfgFile);
            }
            log.info("2. 通过配置生成GroupTemplate的实例，用来获取模板");
            _groupTemplate = new GroupTemplate(cfg);
            log.info("3. 初始化JDBC连接");
            initJdbcConn();
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * 初始化JDBC连接
     */
    private void initJdbcConn() {
        try {
            final JDBCConnectionConfiguration conf = context.getJdbcConnectionConfiguration();
            log.info("注册数据库驱动");
            Class.forName(conf.getDriverClass());
            log.info("获取数据库连接");
            try (final Connection conn = DriverManager.getConnection(conf.getConnectionURL(), conf.getUserId(), conf.getPassword())) {
                final String catalog = conn.getCatalog();
                final DatabaseMetaData databaseMetaData = conn.getMetaData();
                final ResultSet databaseResultSet = databaseMetaData.getTables(catalog, null, null, new String[] { "TABLE" });
                log.info("开始遍历表");
                while (databaseResultSet.next()) {
                    final String tableName = databaseResultSet.getString("TABLE_NAME");
                    log.info("判断{}是否是中间表", tableName);
                    final ResultSet columnResultSet = databaseMetaData.getColumns(catalog, null, tableName, null);
                    if (isMiddleTable(tableName, columnResultSet)) {
                        log.info("{}是中间表", tableName);
                        _middleTableList.add(tableName);
                    }

                    log.info("获取{}表的外键", tableName);
                    final ResultSet foreignKeyResultSet = databaseMetaData.getImportedKeys(catalog, null, tableName);
                    log.info("开始遍历{}表的外键", tableName);
                    while (foreignKeyResultSet.next()) {
                        final ForeignKeyInfo foreignKey = new ForeignKeyInfo();
                        foreignKey.setFkTableName(tableName);
                        foreignKey.setFkClassName(JavaBeansUtil.getCamelCaseString(foreignKey.getFkTableName(), true) + "Jo");
                        foreignKey.setFkFieldName(foreignKeyResultSet.getString("FKCOLUMN_NAME"));
                        final String fkBeanName = JavaBeansUtil.getCamelCaseString(tableName, false);
                        foreignKey.setFkBeanName(fkBeanName);

                        foreignKey.setPkTableName(foreignKeyResultSet.getString("PKTABLE_NAME"));
                        foreignKey.setPkClassName(JavaBeansUtil.getCamelCaseString(foreignKey.getPkTableName(), true) + "Jo");
                        foreignKey.setPkFieldName(foreignKeyResultSet.getString("PKCOLUMN_NAME"));
                        final String pkBeanName = JavaBeansUtil.getCamelCaseString(foreignKey.getFkFieldName(), false);
                        foreignKey.setPkBeanName(pkBeanName.substring(0, pkBeanName.length() - 2));

                        // 先默认设置外键表不是中间表
                        foreignKey.setIsMiddleTableOnFk(false);
                        _foreignKeyList.add(foreignKey);
                        log.debug("foreignKeyInfo: {}", foreignKey);
                    }
                }
            }
        } catch (final ClassNotFoundException | SQLException e) {
            final String msg = "初始化JDBC连接出错";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * 获取需要生成代码的模板的配置模板
     */
    private Template getCfgTemplate() {
        try {
            // 1.先取beelt的模板的配置文件路径
            final String templatesCfgFile = properties.getProperty(BEETL_TEMPLATES_CFG_FILE);
            if (templatesCfgFile == null || templatesCfgFile.trim().isEmpty()) {
                throw new RuntimeException("没有配置“" + BEETL_TEMPLATES_CFG_FILE + "”选项");
            }
            // 2.通过beetl获取配置模板
            final GroupTemplate groupTemplate = new GroupTemplate(Configuration.defaultConfiguration());
            final Template cfgTemplate = groupTemplate.getTemplate(templatesCfgFile);
            // 3.读取beetl模板生成文件的模块路径（用在模板的配置文件中指定java生成文件的路径），并注入到配置模板中
            final String beetlModulePath = properties.getProperty(BEETL_MODULE_PATH);
            cfgTemplate.binding("modulePath", beetlModulePath);
            // 4.计算出模块的包，准备在需要生成代码的模板中注入
            _modulePackage = beetlModulePath.replace('/', '.');
            // 5.读取beetl模板生成文件的模块名称（用在模板配置文件中指定jsp/js/css等生成文件的路径），准备在需要生成代码的模板中注入
            cfgTemplate.binding("moduleName", properties.getProperty(BEETL_MODULE_NAME));
            return cfgTemplate;
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean modelBaseRecordClassGenerated(final TopLevelClass topLevelClass, final IntrospectedTable introspectedTable) {
        final String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
        final boolean isMiddleTable = _middleTableList.contains(tableName);
        log.info("1. 通过配置模板获取生成代码模板的配置参数");
        // 1.1.获取当前表的实体类简称并注入配置模板
        final String entityName = JavaBeansUtil.getCamelCaseString(tableName, true);
        // 1.2.获取需要生成代码的模板的配置模板
        final Template cfgTemplate = getCfgTemplate();
        // 1.3.注入参数到配置模板
        cfgTemplate.binding("entityName", entityName);
        cfgTemplate.binding("entitySimpleName", removeFirstWord(entityName));
        // 1.4.返回配置模板的渲染结果
        final String json = cfgTemplate.render();
        // 1.5.解析模板的配置（json格式）
        final List<TemplateCfg> templateCfgs = JSON.parseArray(json, TemplateCfg.class);// 注意TemplateCfg不能是内部类

        log.info("2. 获取ID类型,(String,Long,Array-组合主键)");
        String idType = null;
        if (introspectedTable.getPrimaryKeyColumns().size() > 1) {
            idType = "Array";
        }
        final Map<String, String> ids = new LinkedHashMap<>();
        for (final IntrospectedColumn column : introspectedTable.getPrimaryKeyColumns()) {
            final String name = column.getJavaProperty();
            final String type = column.getFullyQualifiedJavaType().toString();
            ids.put(name, type);
            if (idType == null) {
                idType = column.getFullyQualifiedJavaType().getShortName();
            }
        }

        log.info("3. 准备实体的属性信息");
        final List<PropInfo> props = new ArrayList<>();
        for (int i = 0; i < introspectedTable.getAllColumns().size(); i++) {
            final IntrospectedColumn column = introspectedTable.getAllColumns().get(i);
            final Field field = topLevelClass.getFields().get(i);
            final PropInfo propInfo = new PropInfo();
            propInfo.setCode(field.getName());
            propInfo.setRemark(column.getRemarks());
            propInfo.setName(RemarksUtils.getTitleByRemarks(propInfo.getRemark()));
            propInfo.setSourceCode(column.getActualColumnName());
            String typeName = field.getType().getShortName();
            if (typeName.equals("Date")) {
                if (propInfo.getCode().endsWith("Time")) {
                    typeName = "Time";
                }
            }
            propInfo.setLength(column.getLength());
            propInfo.setScale(column.getScale());
            propInfo.setType(typeName);
            propInfo.setIsNullable(column.isNullable());
            propInfo.setDefaultValue(column.getDefaultValue() == null ? null : column.getDefaultValue().trim());
            props.add(propInfo);
        }

        log.info("4. 设置外键和关联表");
        final List<ForeignKeyInfo> fkFks = new LinkedList<>();
        final List<ForeignKeyInfo> fkPks = new LinkedList<>();
        for (final ForeignKeyInfo foreignKey : _foreignKeyList) {
            // 如果当前表是外键的外键表
            if (foreignKey.getFkTableName().equals(tableName)) {
                if (foreignKey.getIsNullable() == null) {
                    // 查找外键的字段属性
                    for (final PropInfo prop : props) {
                        if (prop.getSourceCode().equals(foreignKey.getFkFieldName())) {
                            // 如果该属性是外键，先从属性中移除
                            props.remove(prop);
                            // 获取是否可空
                            foreignKey.setIsNullable(prop.getIsNullable());
                            // 获取字段标题
                            String title = prop.getName();
                            if (title.endsWith("ID")) {
                                title = title.substring(0, title.length() - 2);
                            }
                            foreignKey.setTitle(title);
                            // 外键的外键表是否是中间表
                            foreignKey.setIsMiddleTableOnFk(_middleTableList.contains(foreignKey.getFkTableName()));
                            break;
                        }
                    }
                }
                fkFks.add(foreignKey);
            }
            // 如果当前表是外键的主键表
            else if (foreignKey.getPkTableName().equals(tableName)) {
                fkPks.add(foreignKey);
            }
        }

        log.info("类文件注释:{}", topLevelClass.getFileCommentLines());

        log.info("5. 遍历模板文件配置，建立模板实例、注入参数、渲染结果并输出到文件");
        for (final TemplateCfg templateCfg : templateCfgs) {
            log.debug("5.1. 根据配置要求判断中间表是否生成目标文件(默认不生成)");
            Boolean isGenTargetOnMiddleTable = templateCfg.getIsGenTargetOnMiddleTable();
            if (isGenTargetOnMiddleTable == null) {
                isGenTargetOnMiddleTable = false;
            }
            if (!isGenTargetOnMiddleTable && isMiddleTable) {
                continue;
            }

            log.debug("5.2. 建立模板实例");
            final Template template = _groupTemplate.getTemplate(templateCfg.getTemplateName());
            log.debug("5.3. 注入参数");
            template.binding("isGenTargetOnMiddleTable", isGenTargetOnMiddleTable); // 中间表是否生成目标文件
            template.binding("pojo", topLevelClass);
            template.binding("table", introspectedTable);
            template.binding("tableName", tableName);
            template.binding("props", props);                                       // 表所有字段的属性信息
            template.binding("fkFks", fkFks);                                       // 外键的外键信息列表
            template.binding("fkPks", fkPks);                                       // 外键的主键信息列表
            template.binding("modulePackage", _modulePackage);
            template.binding("moduleName", properties.getProperty(BEETL_MODULE_NAME));
            template.binding("entityName", entityName);
            template.binding("entityNamePrefix", getFirstWord(entityName)); // 实体名称的前缀，一般是系统的简称
            template.binding("entitySimpleName", removeFirstWord(entityName));
            template.binding("entityTitle", RemarksUtils.getTitleByRemarks(introspectedTable.getRemarks()));
            template.binding("entityRemarks", RemarksUtils.getSplitJointRemarks(introspectedTable.getRemarks()));
            template.binding("moClassFullName", topLevelClass.getType().getFullyQualifiedName());
            template.binding("moClassShortName", topLevelClass.getType().getShortName());
            template.binding("idType", idType);
            template.binding("ids", ids);
            log.debug("5.4. 渲染结果");
            String sTarget = template.render();

            log.info("5.5. 将渲染结果输出到文件");
            String targetDir = templateCfg.getTargetDir();
            if (!PathUtils.isAbsPath(targetDir)) {
                targetDir = Paths.get(CodeGenByBeetlPlugin.class.getResource("/").getPath(), targetDir).toString();
            }
            final File targetFile = new File(targetDir, templateCfg.getTargetFile());
            try {
                log.info("5.5.1. 判断目标文件是否存在");
                if (targetFile.exists()) {
                    log.info("5.5.2. 目标文件存在");
                    log.info("5.5.2.1 判断如果文件内容相同，不用写直接返回");
                    if (contentEquals(targetFile, sTarget.getBytes(StandardCharsets.UTF_8))) {
                        return true;
                    }
                    log.info("5.5.2.2 按配置要求是否进行备份");
                    if (templateCfg.getBackup()) {
                        Files.copy(targetFile, new File(targetFile.getCanonicalPath() + "." + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))));
                    }
                    log.info("5.5.2.3 如果是java文件，那么合并文件");
                    if (targetFile.getName().endsWith(".java")) {
                        sTarget = MergeJavaFileUtils.merge(sTarget, targetFile, new String[] { "@ibatorgenerated", "@abatorgenerated", "@mbggenerated", "@mbg.generated" });
                    }
                } else {
                    log.info("5.5.2. 目标文件不存在");
                    targetFile.getParentFile().mkdirs();
                    targetFile.createNewFile();
                    sTarget = JavaSourceUtils.removeUnusedImports(sTarget);
                }

                log.info("5.5.3. 输出到文件");
                try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
                    osw.write(sTarget);
                }
            } catch (final IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return true;

    }

    /**
     * 判断是否是中间表（目前根据遍历所有字段属性名，如果都是以Id结尾，那么是中间表，否则只要有一个不是，都不是中间表）
     */
    private Boolean isMiddleTable(final String tableName, final ResultSet columnResultSet) throws SQLException {
        while (columnResultSet.next()) {
            final String columnName = columnResultSet.getString("COLUMN_NAME");
            if (!columnName.equals("ID") && !columnName.endsWith("_ID")) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param sCamelCase
     *            要处理的字符串（必须按驼峰的命名风格）
     * @return 得到第一个单词
     */
    private Object getFirstWord(final String sCamelCase) {
        int i = 1;
        for (; i < sCamelCase.length(); i++) {
            final char ch = sCamelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                break;
            }
        }
        return StringUtils.left(sCamelCase, i);
    }

    /**
     * @param sCamelCase
     *            要处理的字符串（必须按驼峰的命名风格）
     * @return 去掉第一个单词
     */
    private String removeFirstWord(final String sCamelCase) {
        int i = 1;
        for (; i < sCamelCase.length(); i++) {
            final char ch = sCamelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                break;
            }
        }
        return sCamelCase.substring(i);
    }

    /**
     * TODO MBG : 比较两个文件的内容是否相同
     *
     * @param file1
     *            第一个文件
     * @param file2
     *            第二个文件的字节
     * @return
     */
    private boolean contentEquals(final File file1, final byte[] file2) {
        return false;
    }

}
