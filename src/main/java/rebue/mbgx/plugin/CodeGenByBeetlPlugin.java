package rebue.mbgx.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.io.Files;

import rebue.mbgx.util.MergeJavaFileUtil;
import rebue.mbgx.util.RemarksUtil;

/**
 * 利用beetl生成代码的插件
 * 
 * @author zbz
 */
public class CodeGenByBeetlPlugin extends PluginAdapter {
    private final static Logger _log                     = LoggerFactory.getLogger(CodeGenByBeetlPlugin.class);

    /**
     * beetl的配置文件（位于classpath下的路径）
     */
    private static final String BEETL_CFG_FILE           = "beetlCfgFile";
    /**
     * beetl的模板文件（位于模板目录下的路径），多个文件用逗号相隔
     */
    private static final String BEETL_TEMPLATES_CFG_FILE = "templatesCfgFile";
    /**
     * beetl模板生成文件的模块路径（用在模板的配置文件中指定java生成文件的路径）
     */
    private static final String BEETL_MODULE_PATH        = "beetlModulePath";
    /**
     * beetl模板生成文件的模块名称（用在模板配置文件中指定jsp/js/css等生成文件的路径）
     */
    private static final String BEETL_MODULE_NAME        = "beetlModuleName";

    /**
     * 用来获取beetl模板的groupTemplate
     */
    private GroupTemplate       groupTemplate;

    /**
     * 模块的包，用来注入模板，获取beetlModulePath后将/替换为.就可以得到
     */
    private String              modulePackage;

    /**
     * 配置模板
     */
    private Template            cfgTemplate;

    private String              _catalog;
    private DatabaseMetaData    _metaData;

    @Override
    public boolean validate(final List<String> paramList) {
        if (cfgTemplate == null) {
            try {
                _log.info("1. 取beetl的配置");
                Configuration cfg;
                cfg = new Configuration();
                final String beetlCfgFile = properties.getProperty(BEETL_CFG_FILE);
                if (beetlCfgFile != null) {
                    cfg.add(beetlCfgFile);
                }
                _log.info("2. 通过配置生成GroupTemplate的实例，用来获取模板");
                groupTemplate = new GroupTemplate(cfg);
                _log.info("3. 获取需要生成代码的模板的配置模板");
                getCfgTemplate();
                _log.info("4. 初始化JDBC连接");
                initJdbcConn();
            } catch (final IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    /**
     * 初始化JDBC连接
     */
    private void initJdbcConn() {
        try {
            final JDBCConnectionConfiguration conf = context.getJdbcConnectionConfiguration();
            // 注册数据库驱动
            Class.forName(conf.getDriverClass());
            // 获取数据库连接
            final Connection conn = DriverManager.getConnection(conf.getConnectionURL(), conf.getUserId(), conf.getPassword());
            _catalog = conn.getCatalog();
            _metaData = conn.getMetaData();
        } catch (final ClassNotFoundException | SQLException e) {
            final String msg = "初始化JDBC连接出错";
            _log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * 获取需要生成代码的模板的配置模板
     */
    private void getCfgTemplate() throws IOException {
        // 1.先取beelt的模板的配置文件路径
        final String templatesCfgFile = properties.getProperty(BEETL_TEMPLATES_CFG_FILE);
        if (templatesCfgFile == null || templatesCfgFile.trim().isEmpty()) {
            throw new RuntimeException("没有配置“" + BEETL_TEMPLATES_CFG_FILE + "”选项");
        }
        // 2.通过beetl获取配置模板
        final GroupTemplate groupTemplate = new GroupTemplate(Configuration.defaultConfiguration());
        cfgTemplate = groupTemplate.getTemplate(templatesCfgFile);
        // 3.读取beetl模板生成文件的模块路径（用在模板的配置文件中指定java生成文件的路径），并注入到配置模板中
        final String beetlModulePath = properties.getProperty(BEETL_MODULE_PATH);
        cfgTemplate.binding("modulePath", beetlModulePath);
        // 4.计算出模块的包，准备在需要生成代码的模板中注入
        modulePackage = beetlModulePath.replace('/', '.');
        // 5.读取beetl模板生成文件的模块名称（用在模板配置文件中指定jsp/js/css等生成文件的路径），准备在需要生成代码的模板中注入
        cfgTemplate.binding("moduleName", properties.getProperty(BEETL_MODULE_NAME));
    }

    @Override
    public boolean modelBaseRecordClassGenerated(final TopLevelClass topLevelClass, final IntrospectedTable introspectedTable) {
        final String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
        _log.info("************************************************************************");
        _log.info("* 开始处理{}表                                                *", tableName);
        _log.info("************************************************************************", tableName);
        _log.info("1. 通过配置模板获取生成代码模板的配置参数");
        // 1.1.获取当前表的实体类简称并注入配置模板
        final String entityName = JavaBeansUtil.getCamelCaseString(tableName, true);
        cfgTemplate.binding("entityName", entityName);
        cfgTemplate.binding("entitySimpleName", removeFirstWord(entityName));
        // 1.2.返回配置模板的渲染结果
        final String json = cfgTemplate.render();
        // 1.3.解析模板的配置（json格式）
        final List<TemplateCfg> templateCfgs = JSON.parseArray(json, TemplateCfg.class);// 注意TemplateCfg不能是内部类

        _log.info("2. 获取ID类型,(String,Long,Array-组合主键)");
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

        _log.info("3. 准备实体的属性信息");
        final List<PropInfo> props = new ArrayList<>();
        for (int i = 0; i < introspectedTable.getAllColumns().size(); i++) {
            final IntrospectedColumn column = introspectedTable.getAllColumns().get(i);
            final Field field = topLevelClass.getFields().get(i);
            final PropInfo propInfo = new PropInfo();
            propInfo.setCode(field.getName());
            final String fieldRemark = column.getRemarks();
            propInfo.setName(RemarksUtil.getTitleByRemarks(fieldRemark));
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

        _log.info("4. 获取外键");
        final List<FeignKeyInfo> feignKeys = new ArrayList<>();
        try {
            final ResultSet foreignKeyResultSet = _metaData.getImportedKeys(_catalog, null, tableName);
            while (foreignKeyResultSet.next()) {
                final FeignKeyInfo feignKeyInfo = new FeignKeyInfo();
                feignKeyInfo.setFkFieldName(foreignKeyResultSet.getString("FKCOLUMN_NAME"));
                feignKeyInfo.setPkTableName(foreignKeyResultSet.getString("PKTABLE_NAME"));
                feignKeyInfo.setPkFieldName(foreignKeyResultSet.getString("PKCOLUMN_NAME"));
                feignKeyInfo.setPkClassName(JavaBeansUtil.getCamelCaseString(feignKeyInfo.getPkTableName(), true) + "Jo");
                final String beanName = JavaBeansUtil.getCamelCaseString(feignKeyInfo.getFkFieldName(), false);
                feignKeyInfo.setPkBeanName(beanName.substring(0, beanName.length() - 2));
                // 查找关键字的字段属性，获取是否可空
                for (final PropInfo prop : props) {
                    if (prop.getSourceCode().equals(feignKeyInfo.getFkFieldName())) {
                        feignKeyInfo.setIsNullable(prop.getIsNullable());
                        break;
                    }
                }
                feignKeys.add(feignKeyInfo);
                _log.debug("feignKeyInfo: {}", feignKeyInfo);
            }
        } catch (final SQLException e) {
            final String msg = "获取外键出错";
            _log.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        _log.info("类文件注释:{}", topLevelClass.getFileCommentLines());

        _log.info("5. 建立模板实例、注入参数并渲染结果");
        for (final TemplateCfg templateCfg : templateCfgs) {
            final Template template = groupTemplate.getTemplate(templateCfg.getTemplateName());
            template.binding("pojo", topLevelClass);
            template.binding("table", introspectedTable);
            template.binding("tableName", tableName);
            template.binding("feignKeys", feignKeys);
            template.binding("props", props);
            template.binding("modulePackage", modulePackage);
            template.binding("moduleName", properties.getProperty(BEETL_MODULE_NAME));
            template.binding("entityName", entityName);
            template.binding("entityNamePrefix", getFirstWord(entityName)); // 实体名称的前缀，一般是系统的简称
            template.binding("entitySimpleName", removeFirstWord(entityName));
            template.binding("entityTitle", RemarksUtil.getTitleByRemarks(introspectedTable.getRemarks()));
            template.binding("entityRemarks", RemarksUtil.getSplitJointRemarks(introspectedTable.getRemarks()));
            template.binding("moClassFullName", topLevelClass.getType().getFullyQualifiedName());
            template.binding("moClassShortName", topLevelClass.getType().getShortName());
            template.binding("idType", idType);
            template.binding("ids", ids);
            String sTarget = template.render();

            // 根据配置要求判断中间表是否生成目标文件(默认不生成)
            final Boolean isDo = templateCfg.getIsGenTargetOnMiddleTable();
            if (isMiddleTable(topLevelClass) && (isDo == null || !isDo)) {
                continue;
            }

            _log.info("6. 将渲染结果输出到文件");
            final File targetFile = new File(templateCfg.getTargetDir(), templateCfg.getTargetFile());
            try {
                _log.info("6.1. 如果文件存在");
                if (targetFile.exists()) {
                    _log.info("6.2. 如果文件内容相同，不用写直接返回");
                    if (contentEquals(targetFile, sTarget.getBytes("utf-8"))) {
                        return true;
                    }
                    _log.info("6.3. 按配置要求是否进行备份");
                    if (templateCfg.getBackup()) {
                        Files.copy(targetFile,
                                new File(targetFile.getCanonicalPath().toString() + "." + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))));
                    }
                    _log.info("6.4. 如果是java文件，那么合并文件");
                    if (targetFile.getName().endsWith(".java")) {
                        sTarget = MergeJavaFileUtil.merge(sTarget, targetFile, new String[] { "@ibatorgenerated", "@abatorgenerated", "@mbggenerated", "@mbg.generated" });
                    }
                } else {
                    targetFile.getParentFile().mkdirs();
                    targetFile.createNewFile();
                }

                _log.info("6.5. 输出文件");
                try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(targetFile), "utf-8")) {
                    osw.write(sTarget);
                }

//                MergeJavaFileUtil.organizeImports(sTarget, targetFile.getAbsolutePath());
//            } catch (IOException | OperationCanceledException | MalformedTreeException | CoreException | BadLocationException e) {
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
    private Boolean isMiddleTable(final TopLevelClass topLevelClass) {
        for (final Field field : topLevelClass.getFields()) {
            if (!(field.getName().equals("id")) && !field.getName().endsWith("Id") && !(field.getName().equals("serialVersionUID"))) {
                return false;
            }
        }
        _log.info("中间表的类: {}", topLevelClass.getType().getShortName());
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
