package rebue.mbgx.custom;

import java.io.File;
import java.io.FileNotFoundException;

import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;

import rebue.mbgx.util.MergeJavaFileUtils;

/**
 * <pre>
 * 实现了在overwrite=true的方式下可以保留手动修改的代码（包括手动添加的属性和方法）
 * 此方法检查文档注释中是否有@mbg.generated等注解来判断是否是自动生成的属性和方法
 * 欢迎大家踊跃测试并提供宝贵意见
 * </pre>
 * 
 * @author zbz
 *
 */
public class ShellCallbackEx extends DefaultShellCallback {
    public ShellCallbackEx(final boolean overwrite) {
        super(overwrite);
    }

    @Override
    public boolean isMergeSupported() {
        return true;
    }

    @Override
    public String mergeJavaFile(final String newFileSource, final File existingFile, final String[] javadocTags, final String fileEncoding) throws ShellException {
        try {
            return MergeJavaFileUtils.merge(newFileSource, existingFile, javadocTags);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
            throw new ShellException(e);
        }
    }

}
