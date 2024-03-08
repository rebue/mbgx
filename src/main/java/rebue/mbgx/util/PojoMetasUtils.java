package rebue.mbgx.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.generator.config.Context;
import rebue.wheel.core.db.JdbcUtils;
import rebue.wheel.core.db.meta.DbMeta;
import rebue.wheel.core.db.meta.PojoMeta;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public class PojoMetasUtils {

    /**
     * 是否已经初始化
     */
    private static boolean _isInit = false;

    /**
     * 表的List
     */
    @Getter
    private static List<PojoMeta> pojoMetas;

    public static void init(final Context context) {
        if (!_isInit) {
            try {
                Connection connection = context.getConnection();
//                context.getProperties();
                // TODO 获取table.name的配置
                DbMeta dbMeta = JdbcUtils.getDbMeta(connection, ".*");
                pojoMetas = JdbcUtils.dbMetaToPojoMetas(dbMeta);
                _isInit = true;
            } catch (final SQLException e) {
                final String msg = "初始化JDBC连接出错";
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
    }
}
