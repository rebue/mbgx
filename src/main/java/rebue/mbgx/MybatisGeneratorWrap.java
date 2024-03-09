package rebue.mbgx;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.exception.InvalidConfigurationException;
import org.mybatis.generator.exception.XMLParserException;
import rebue.mbgx.custom.ProgressCallbackEx;
import rebue.mbgx.custom.ShellCallbackEx;
import rebue.mbgx.util.PathUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

@Slf4j
public class MybatisGeneratorWrap {
    private final static String MBGX_CONFIG_FILE = "mbgx-comm.xml";

    /**
     * @param overwrite  是否覆盖原来的文件，<br>
     *                   true，直接覆盖已经存在的原文件<br>
     *                   false，不覆盖原文件，但是会添加一个文件，是原来文件名+".1"(.2.3.4.5一直排下去)
     * @param sPropFiles properties文件的路径，如果有多个，可以用逗号分隔
     */
    public static void gen(final boolean overwrite, final String sPropFiles)
            throws IOException, XMLParserException, InvalidConfigurationException, SQLException, InterruptedException {
        // 获取类路径（配置文件在此路径下）
        final String sClassPath = Objects.requireNonNull(MybatisGeneratorWrap.class.getClassLoader().getResource("")).getPath();
        Path         configDir  = Path.of(sClassPath, "conf");
        // 读取属性文件
        final Properties properties = new Properties();
        for (final String sPropFilePath : sPropFiles.split(",")) {
            try (FileInputStream fileInputStream = new FileInputStream(sClassPath + sPropFilePath)) {
                properties.load(fileInputStream);
            }
        }

        gen(configDir, overwrite, properties);
    }

    /**
     * @param configDir  配置目录路径
     * @param overwrite  是否覆盖原来的文件，<br>
     *                   true，直接覆盖已经存在的原文件<br>
     *                   false，不覆盖原文件，但是会添加一个文件，是原来文件名+".1"(.2.3.4.5一直排下去)
     * @param properties properties列表
     */
    public static void gen(Path configDir, final boolean overwrite, final Properties properties) throws XMLParserException, IOException, InvalidConfigurationException, SQLException, InterruptedException {
        final List<String>        warnings = new ArrayList<>();
        final ConfigurationParser parser   = new ConfigurationParser(properties, warnings);
        final Configuration       config   = parser.parseConfiguration(new File(configDir.resolve(MBGX_CONFIG_FILE).toString()));

        Context commContext = null;
        for (Context context : config.getContexts()) {
            if ("comm".equals(context.getId())) {
                commContext = context;
                break;
            }
        }
        if (commContext == null) throw new RuntimeException(MBGX_CONFIG_FILE + "文件没有id为comm的context");

        // 兼容eclipse和idea的路径问题
        String rootPath      = PathUtils.getProjectRoot(MybatisGeneratorWrap.class);
        String targetProject = Paths.get(rootPath, commContext.getJavaModelGeneratorConfiguration().getTargetProject()).toAbsolutePath().toString();
        commContext.getJavaModelGeneratorConfiguration().setTargetProject(targetProject);
        targetProject = Paths.get(rootPath, commContext.getJavaClientGeneratorConfiguration().getTargetProject()).toAbsolutePath().toString();
        commContext.getJavaClientGeneratorConfiguration().setTargetProject(targetProject);

        final ShellCallback    callback  = new ShellCallbackEx(overwrite);
        final MyBatisGenerator generator = new MyBatisGenerator(config, callback, warnings);
        generator.generate(new ProgressCallbackEx());

        if (warnings.size() > 0) {
            String warn = "********************************** 请注意: 警告有 " + warnings.size() + " 条 **********************************";
            log.warn(StringUtils.rightPad(warn, 180));

            for (final String warning : warnings) {
                log.warn(StringUtils.rightPad(warning, 180));
            }

            warn = "****************************************************************************************";
            log.warn(StringUtils.rightPad(warn, 180));
        }
    }

}
