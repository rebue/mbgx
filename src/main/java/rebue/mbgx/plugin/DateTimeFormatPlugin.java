package rebue.mbgx.plugin;

import java.sql.Types;
import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.Plugin;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * <pre>
 * 给Date类型的属性加上@DateTimeFormat注解的插件
 * 如果字段名以DAY结束，那么添加@DateTimeFormat(pattern="yyyy-MM-dd")
 * 如果字段名以_TIME结束，那么添加@DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
 * </pre>
 * 
 * @author zbz
 */
public class DateTimeFormatPlugin extends PluginAdapter {
    private static final String DAY_SUFFIX_PROPERTY  = "daySuffix";
    private static final String TIME_SUFFIX_PROPERTY = "timeSuffix";

    private static String[]     mDaySuffixs;
    private static String[]     mTimeSuffixs;

    @Override
    public boolean validate(List<String> paramList) {
        return true;
    }

    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
            IntrospectedTable introspectedTable, Plugin.ModelClassType modelClassType) {
        if (Types.DATE == introspectedColumn.getJdbcType()) {
            addDateAnnotations(field, topLevelClass);
        } else if (Types.TIMESTAMP == introspectedColumn.getJdbcType()) {
            if (mDaySuffixs == null) {
                String suffix = properties.getProperty(DAY_SUFFIX_PROPERTY);
                mDaySuffixs = suffix != null ? suffix.split(",") : new String[] { "DAY", "_DATE" };
            }
            if (mTimeSuffixs == null) {
                String suffix = properties.getProperty(TIME_SUFFIX_PROPERTY);
                mTimeSuffixs = suffix != null ? suffix.split(",") : new String[] { "_TIME" };
            }

            if (isEndsWith(introspectedColumn.getActualColumnName(), mDaySuffixs)) {
                addDateAnnotations(field, topLevelClass);
            } else if (isEndsWith(introspectedColumn.getActualColumnName(), mTimeSuffixs)) {
                addTimeStampAnnotations(field, topLevelClass);
            }
        }
        return true;
    }

    private void addTimeStampAnnotations(Field field, TopLevelClass topLevelClass) {
        topLevelClass.addImportedType("org.springframework.format.annotation.DateTimeFormat");
//        topLevelClass.addImportedType("com.alibaba.fastjson.annotation.JSONField");
        topLevelClass.addImportedType("com.fasterxml.jackson.annotation.JsonFormat");
        field.addAnnotation("@DateTimeFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")");
//        field.addAnnotation("@JSONField(format = \"yyyy-MM-dd HH:mm:ss\")");
        // 由fastjson改为jackson
        field.addAnnotation(
                "@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = \"yyyy-MM-dd HH:mm:ss\", timezone = \"GMT+8\")");
    }

    private void addDateAnnotations(Field field, TopLevelClass topLevelClass) {
        topLevelClass.addImportedType("org.springframework.format.annotation.DateTimeFormat");
//        topLevelClass.addImportedType("com.alibaba.fastjson.annotation.JSONField");
        topLevelClass.addImportedType("com.fasterxml.jackson.annotation.JsonFormat");
        field.addAnnotation("@DateTimeFormat(pattern = \"yyyy-MM-dd\")");
//        field.addAnnotation("@JSONField(format = \"yyyy-MM-dd\")");
        // 由fastjson改为jackson
        field.addAnnotation(
                "@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = \"yyyy-MM-dd\", timezone = \"GMT+8\")");
    }

    private boolean isEndsWith(String str, String[] suffixs) {
        for (String suffix : suffixs) {
            if (str.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

}
