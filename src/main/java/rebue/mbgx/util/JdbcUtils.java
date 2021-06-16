package rebue.mbgx.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.JDBCConnectionConfiguration;
import org.mybatis.generator.internal.util.JavaBeansUtil;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rebue.mbgx.po.ColumnPo;
import rebue.mbgx.po.ForeignKeyPo;
import rebue.mbgx.po.TablePo;

@Slf4j
public class JdbcUtils {

    /**
     * 是否已经初始化
     */
    private static boolean                  _isInit         = false;

    /**
     * 表的List
     */
    @Getter
    private final static List<TablePo>      tableList       = new LinkedList<>();
    /**
     * 中间表的List(item为表名字符串)
     */
    @Getter
    private final static List<String>       middleTableList = new LinkedList<>();
    /**
     * 外键的List
     */
    @Getter
    private final static List<ForeignKeyPo> foreignKeyList  = new LinkedList<>();

    public static void init(final Context context) {
        if (!_isInit) {
            try {
                final JDBCConnectionConfiguration conf = context.getJdbcConnectionConfiguration();
                log.info("注册数据库驱动");
                Class.forName(conf.getDriverClass());
                log.info("获取数据库连接");
                try (Connection conn = DriverManager.getConnection(conf.getConnectionURL(), conf.getUserId(), conf.getPassword())) {
                    final String           catalog           = conn.getCatalog();
                    final DatabaseMetaData databaseMetaData  = conn.getMetaData();
                    final ResultSet        databaseResultSet = databaseMetaData.getTables(catalog, null, null, new String[] { "TABLE"
                    });
                    log.info("开始遍历表");
                    while (databaseResultSet.next()) {
                        final TablePo table = new TablePo();
                        tableList.add(table);
                        table.setName(databaseResultSet.getString("TABLE_NAME"));

                        log.info("获取{}表的列结果集", table.getName());
                        final ResultSet columnResultSet = databaseMetaData.getColumns(catalog, null, table.getName(), null);

                        while (columnResultSet.next()) {
                            final ColumnPo column = new ColumnPo();
                            column.setName(columnResultSet.getString("COLUMN_NAME"));
                            column.setRemark(columnResultSet.getString("REMARKS"));
                            column.setTitle(RemarksUtils.getTitleByRemarks(column.getRemark()));
                            table.getColumns().add(column);
                        }

                        log.info("判断{}是否是中间表", table.getName());
                        if (isMiddleTable(table)) {
                            log.info("{}是中间表", table.getName());
                            table.setIsMiddleTable(true);
                            middleTableList.add(table.getName());
                        }

                        log.info("获取{}表的外键", table.getName());
                        final ResultSet foreignKeyResultSet = databaseMetaData.getImportedKeys(catalog, null, table.getName());
                        log.info("开始遍历{}表的外键", table.getName());
                        while (foreignKeyResultSet.next()) {
                            final ForeignKeyPo foreignKey = new ForeignKeyPo();
                            foreignKey.setFkTableName(table.getName());
                            foreignKey.setFkClassName(JavaBeansUtil.getCamelCaseString(foreignKey.getFkTableName(), true));
                            foreignKey.setFkFieldName(foreignKeyResultSet.getString("FKCOLUMN_NAME"));
                            final String fkBeanName = JavaBeansUtil.getCamelCaseString(table.getName(), false);
                            foreignKey.setFkBeanName(fkBeanName);

                            for (final ColumnPo column : table.getColumns()) {
                                if (column.getName().equalsIgnoreCase(foreignKey.getFkFieldName())) {
                                    // 获取字段标题
                                    String title = column.getTitle();
                                    if (title.endsWith("ID")) {
                                        title = title.substring(0, title.length() - 2);
                                    }
                                    foreignKey.setTitle(title);
                                    break;
                                }
                            }

                            foreignKey.setPkTableName(foreignKeyResultSet.getString("PKTABLE_NAME"));
                            foreignKey.setPkClassName(JavaBeansUtil.getCamelCaseString(foreignKey.getPkTableName(), true));
                            foreignKey.setPkFieldName(foreignKeyResultSet.getString("PKCOLUMN_NAME"));
                            final String pkBeanName = JavaBeansUtil.getCamelCaseString(foreignKey.getFkFieldName(), false);
                            foreignKey.setPkBeanName(pkBeanName.substring(0, pkBeanName.length() - 2));

                            // 先默认设置外键表不是中间表
                            foreignKey.setIsMiddleTableOnFk(false);
                            foreignKeyList.add(foreignKey);
                            log.debug("foreignKeyInfo: {}", foreignKey);
                        }
                    }
                }
                _isInit = true;
            } catch (final ClassNotFoundException | SQLException e) {
                final String msg = "初始化JDBC连接出错";
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
    }

    /**
     * 判断是否是中间表（目前根据遍历所有字段属性名，如果都是以Id结尾，那么是中间表，否则只要有一个不是，都不是中间表；只有一个ID字段，没有其它字段的也不是中间表）
     */
    private static Boolean isMiddleTable(final TablePo table) {
        if (table.getColumns().size() == 1) {
            return false;
        }
        for (final ColumnPo column : table.getColumns()) {
            if (!column.getName().equals("ID") && !column.getName().endsWith("_ID")) {
                return false;
            }
        }
        return true;
    }

}
