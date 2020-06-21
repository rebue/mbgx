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
    private static final String DATE_SUFFIX_PROPERTY     = "dateSuffix";
    private static final String DATETIME_SUFFIX_PROPERTY = "datetimeSuffix";

    private static String[]     mDateSuffixs;
    private static String[]     mDatetimeSuffixs;

    @Override
    public boolean validate(final List<String> paramList) {
        return true;
    }

    @Override
    public boolean modelFieldGenerated(final Field field, final TopLevelClass topLevelClass, final IntrospectedColumn introspectedColumn, final IntrospectedTable introspectedTable,
            final Plugin.ModelClassType modelClassType) {
        if (Types.DATE == introspectedColumn.getJdbcType()) {
            addDateAnnotations(field, topLevelClass);
        } else if (Types.TIMESTAMP == introspectedColumn.getJdbcType()) {
            if (mDateSuffixs == null) {
                final String suffix = properties.getProperty(DATE_SUFFIX_PROPERTY);
                mDateSuffixs = suffix != null ? suffix.split(",") : new String[] { "DAY", "_DATE" };
            }
            if (mDatetimeSuffixs == null) {
                final String suffix = properties.getProperty(DATETIME_SUFFIX_PROPERTY);
                mDatetimeSuffixs = suffix != null ? suffix.split(",") : new String[] { "_DATETIME" };
            }

            if (isEndsWith(introspectedColumn.getActualColumnName(), mDateSuffixs)) {
                addDateAnnotations(field, topLevelClass);
            } else if (isEndsWith(introspectedColumn.getActualColumnName(), mDatetimeSuffixs)) {
                addTimeStampAnnotations(field, topLevelClass);
            }
        }
        return true;
    }

    private void addTimeStampAnnotations(final Field field, final TopLevelClass topLevelClass) {
        topLevelClass.addImportedType("org.springframework.format.annotation.DateTimeFormat");
//        topLevelClass.addImportedType("com.alibaba.fastjson.annotation.JSONField");
        topLevelClass.addImportedType("com.fasterxml.jackson.annotation.JsonFormat");
        field.addAnnotation("@DateTimeFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")");
//        field.addAnnotation("@JSONField(format = \"yyyy-MM-dd HH:mm:ss\")");
        // 由fastjson改为jackson
        field.addAnnotation("@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = \"yyyy-MM-dd HH:mm:ss\", timezone = \"GMT+8\")");
    }

    private void addDateAnnotations(final Field field, final TopLevelClass topLevelClass) {
        topLevelClass.addImportedType("org.springframework.format.annotation.DateTimeFormat");
//        topLevelClass.addImportedType("com.alibaba.fastjson.annotation.JSONField");
        topLevelClass.addImportedType("com.fasterxml.jackson.annotation.JsonFormat");
        field.addAnnotation("@DateTimeFormat(pattern = \"yyyy-MM-dd\")");
//        field.addAnnotation("@JSONField(format = \"yyyy-MM-dd\")");
        // 由fastjson改为jackson
        field.addAnnotation("@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = \"yyyy-MM-dd\", timezone = \"GMT+8\")");
    }

    private boolean isEndsWith(final String str, final String[] suffixs) {
        for (final String suffix : suffixs) {
            if (str.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

}
