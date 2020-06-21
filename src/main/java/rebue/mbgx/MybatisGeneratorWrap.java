package rebue.mbgx;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.exception.InvalidConfigurationException;
import org.mybatis.generator.exception.XMLParserException;

import lombok.extern.slf4j.Slf4j;
import rebue.mbgx.custom.ProgressCallbackEx;
import rebue.mbgx.custom.ShellCallbackEx;

@Slf4j
public class MybatisGeneratorWrap {
    /**
     * @param overwrite
     *            是否覆盖原来的文件，<br>
     *            true，直接覆盖已经存在的原文件<br>
     *            false，不覆盖原文件，但是会添加一个文件，是原来文件名+".1"(.2.3.4.5一直排下去)
     * @param sPropFiles
     *            properties文件的路径，如果有多个，可以用逗号分隔
     */
    public static void gen(final boolean overwrite, final String sPropFiles)
            throws IOException, XMLParserException, InvalidConfigurationException, SQLException, InterruptedException {
        // 获取类路径（配置文件在此路径下）
        final String sClassPath = MybatisGeneratorWrap.class.getClassLoader().getResource("").getPath();

        // 读取属性文件
        final Properties properties = new Properties();
        for (final String sPropFilePath : sPropFiles.split(",")) {
            try (FileInputStream fileInputStream = new FileInputStream(sClassPath + sPropFilePath)) {
                properties.load(fileInputStream);
            }
        }

        final List<String> warnings = new ArrayList<>();
        final ConfigurationParser parser = new ConfigurationParser(properties, warnings);
        final Configuration config = parser.parseConfiguration(new File(sClassPath + "conf/mbg-comm.xml"));
        final ShellCallback callback = new ShellCallbackEx(overwrite);
        final MyBatisGenerator generator = new MyBatisGenerator(config, callback, warnings);
        generator.generate(new ProgressCallbackEx());

        if (warnings.size() > 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append("\r\n********************************** 请注意: 警告有 ");
            sb.append(warnings.size());
            sb.append(" 条 **********************************");
            for (final String warning : warnings) {
                sb.append("\r\n");
                sb.append(warning);
            }
            sb.append("\r\n********************************************************************");
            log.warn(sb.toString());
        }
    }

}
