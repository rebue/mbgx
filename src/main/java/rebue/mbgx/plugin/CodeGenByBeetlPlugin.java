package rebue.mbgx.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.text.edits.MalformedTreeException;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.internal.util.JavaBeansUtil;

import com.alibaba.fastjson.JSON;
import com.google.common.io.Files;

import rebue.mbgx.PropInfo;
import rebue.mbgx.TemplateCfg;
import rebue.mbgx.util.MergeJavaFileUtil;
import rebue.mbgx.util.RemarksUtil;

/**
 * 利用beetl生成代码的插件
 * 
 * @author zbz
 */
public class CodeGenByBeetlPlugin extends PluginAdapter {

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

    @Override
    public boolean validate(List<String> paramList) {
        if (cfgTemplate == null) {
            try {
                // 1.取beetl的配置
                Configuration cfg;
                cfg = new Configuration();
                String beetlCfgFile = properties.getProperty(BEETL_CFG_FILE);
                if (beetlCfgFile != null) {
                    cfg.add(beetlCfgFile);
                }
                // 2.通过配置生成GroupTemplate的实例，用来获取模板
                groupTemplate = new GroupTemplate(cfg);
                // 3.获取需要生成代码的模板的配置模板
                getCfgTemplate();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    private void getCfgTemplate() throws IOException {
        // 1.先取beelt的模板的配置文件路径
        String templatesCfgFile = properties.getProperty(BEETL_TEMPLATES_CFG_FILE);
        if (templatesCfgFile == null || templatesCfgFile.trim().isEmpty()) {
            throw new RuntimeException("没有配置“" + BEETL_TEMPLATES_CFG_FILE + "”选项");
        }
        // 2.通过beetl获取配置模板
        GroupTemplate groupTemplate = new GroupTemplate(Configuration.defaultConfiguration());
        cfgTemplate = groupTemplate.getTemplate(templatesCfgFile);
        // 3.读取beetl模板生成文件的模块路径（用在模板的配置文件中指定java生成文件的路径），并注入到配置模板中
        String beetlModulePath = properties.getProperty(BEETL_MODULE_PATH);
        cfgTemplate.binding("modulePath", beetlModulePath);
        // 4.计算出模块的包，准备在需要生成代码的模板中注入
        modulePackage = beetlModulePath.replace('/', '.');
        // 5.读取beetl模板生成文件的模块名称（用在模板配置文件中指定jsp/js/css等生成文件的路径），准备在需要生成代码的模板中注入
        cfgTemplate.binding("moduleName", properties.getProperty(BEETL_MODULE_NAME));
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        // 1.通过配置模板获取生成代码模板的配置参数
        // 1.1.获取当前表的实体类简称并注入配置模板
        String entityName = JavaBeansUtil.getCamelCaseString(introspectedTable.getFullyQualifiedTable().getIntrospectedTableName(), true);
        cfgTemplate.binding("entityName", entityName);
        cfgTemplate.binding("entitySimpleName", removeFirstWord(entityName));
        // 1.2.返回配置模板的渲染结果
        String json = cfgTemplate.render();
        // 1.3.解析模板的配置（json格式）
        List<TemplateCfg> templateCfgs = JSON.parseArray(json, TemplateCfg.class);// 注意TemplateCfg不能是内部类

        // 2. 获取ID类型,(String,Long,Array-组合主键)
        String idType = null;
        if (introspectedTable.getPrimaryKeyColumns().size() > 1) {
            idType = "Array";
        }
        Map<String, String> ids = new LinkedHashMap<>();
        for (IntrospectedColumn column : introspectedTable.getPrimaryKeyColumns()) {
            String name = column.getJavaProperty();
            String type = column.getFullyQualifiedJavaType().toString();
            ids.put(name, type);
            if (idType == null)
                idType = column.getFullyQualifiedJavaType().getShortName();
        }

        // 3.准备实体的属性信息
        List<PropInfo> props = new ArrayList<>();
        for (int i = 0; i < introspectedTable.getAllColumns().size(); i++) {
            IntrospectedColumn column = introspectedTable.getAllColumns().get(i);
            Field field = topLevelClass.getFields().get(i);
            PropInfo propInfo = new PropInfo();
            propInfo.setCode(field.getName());
            String fieldRemark = column.getRemarks();
            propInfo.setName(RemarksUtil.getTitleByRemarks(fieldRemark));
            String typeName = field.getType().getShortName();
            if (typeName.equals("Date")) {
                if (propInfo.getCode().endsWith("Time")) {
                    typeName = "Time";
                }
            }
            propInfo.setType(typeName);
            propInfo.setIsNullable(column.isNullable());
            propInfo.setDefaultValue(column.getDefaultValue() == null ? null : column.getDefaultValue().trim());
            props.add(propInfo);
        }

        // 4.建立模板实例、注入参数并渲染结果
        for (TemplateCfg templateCfg : templateCfgs) {
            Template template = groupTemplate.getTemplate(templateCfg.getTemplateName());
            template.binding("pojo", topLevelClass);
            template.binding("table", introspectedTable);
            template.binding("props", props);
            template.binding("modulePackage", modulePackage);
            template.binding("moduleName", properties.getProperty(BEETL_MODULE_NAME));
            template.binding("entityName", entityName);
            template.binding("entityNamePrefix", getFirstWord(entityName));     // 实体名称的前缀，一般是系统的简称
            template.binding("entitySimpleName", removeFirstWord(entityName));
            template.binding("entityTitle", RemarksUtil.getTitleByRemarks(introspectedTable.getRemarks()));
            template.binding("entityRemarks", RemarksUtil.getSplitJointRemarks(introspectedTable.getRemarks()));
            template.binding("moClassFullName", topLevelClass.getType().getFullyQualifiedName());
            template.binding("moClassShortName", topLevelClass.getType().getShortName());
            template.binding("idType", idType);
            template.binding("ids", ids);
            String sTarget = template.render();

            // 根据配置要求判断中间表是否生成目标文件(默认不生成)
            Boolean isDo = templateCfg.getIsGenTargetOnMiddleTable();
            if (isMiddleTable(topLevelClass) && (isDo == null || !isDo)) {
                continue;
            }

            // 5.将渲染结果输出到文件
            File targetFile = new File(templateCfg.getTargetDir(), templateCfg.getTargetFile());
            try {
                // 5.1.如果文件存在
                if (targetFile.exists()) {
                    // 5.2.如果文件内容相同，不用写直接返回
                    if (contentEquals(targetFile, sTarget.getBytes("utf-8"))) {
                        return true;
                    }
                    // 5.3.按配置要求是否进行备份
                    if (templateCfg.getBackup()) {
                        Files.copy(targetFile,
                                new File(targetFile.getCanonicalPath().toString() + "." + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))));
                    }
                    // 5.4.如果是java文件，那么合并文件
                    if (targetFile.getName().endsWith(".java")) {
                        sTarget = MergeJavaFileUtil.merge(sTarget, targetFile, new String[] { "@ibatorgenerated", "@abatorgenerated", "@mbggenerated", "@mbg.generated" });
                    }
                } else {
                    targetFile.getParentFile().mkdirs();
                    targetFile.createNewFile();
                }

                // 5.5.输出文件
                try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(targetFile), "utf-8")) {
                    osw.write(sTarget);
                }

//                MergeJavaFileUtil.organizeImports(sTarget, targetFile.getAbsolutePath());
//            } catch (IOException | OperationCanceledException | MalformedTreeException | CoreException | BadLocationException e) {
            } catch (IOException | OperationCanceledException | MalformedTreeException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return true;

    }

    /**
     * 判断是否是中间表（目前根据遍历所有字段属性名，如果都是以Id结尾，那么是中间表，否则只要有一个不是，都不是中间表）
     */
    private Boolean isMiddleTable(TopLevelClass topLevelClass) {
        for (Field field : topLevelClass.getFields()) {
            if (!(field.getName().equals("id")) && !field.getName().endsWith("Id") && !(field.getName().equals("serialVersionUID"))) {
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
    private Object getFirstWord(String sCamelCase) {
        int i = 1;
        for (; i < sCamelCase.length(); i++) {
            char ch = sCamelCase.charAt(i);
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
    private String removeFirstWord(String sCamelCase) {
        int i = 1;
        for (; i < sCamelCase.length(); i++) {
            char ch = sCamelCase.charAt(i);
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
    private boolean contentEquals(File file1, byte[] file2) {
        return false;
    }

}
