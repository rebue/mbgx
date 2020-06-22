package rebue.mbgx.custom;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.ProgressCallback;

/**
 * 用日志记录进度
 *
 * @author zbz
 */
@Slf4j
public class ProgressCallbackEx implements ProgressCallback {

    @Override
    public void introspectionStarted(final int totalTasks) {
        log.info(StringUtils.rightPad("*********************************************************", 180));
        log.info(StringUtils.rightPad("MBGX开始执行任务: 总任务数-" + totalTasks, 180));
        log.info(StringUtils.rightPad("*********************************************************", 180));
    }

    @Override
    public void generationStarted(final int totalTasks) {
        log.info(StringUtils.rightPad("*********************************************************", 180));
        log.info(StringUtils.rightPad("开始生成代码: 任务数-" + totalTasks, 180));
        log.info(StringUtils.rightPad("*********************************************************", 180));
    }

    @Override
    public void saveStarted(final int totalTasks) {
        log.info(StringUtils.rightPad("*********************************************************", 180));
        log.info(StringUtils.rightPad("开始保存文件: 任务数-" + totalTasks, 180));
        log.info(StringUtils.rightPad("*********************************************************", 180));
    }

    @Override
    public void startTask(final String taskName) {
        log.info(StringUtils.rightPad("*********************************************************", 180));
        log.info(StringUtils.rightPad("开始执行任务: 任务名称-" + taskName, 180));
        log.info(StringUtils.rightPad("*********************************************************", 180));
    }

    @Override
    public void done() {
        log.info(StringUtils.rightPad("*********************************************************", 180));
        log.info(StringUtils.rightPad("MBGX所有任务执行完成！！！", 180));
        log.info(StringUtils.rightPad("*********************************************************", 180));
    }

    @Override
    public void checkCancel() throws InterruptedException {
        log.info(StringUtils.rightPad("*********************************************************", 180));
        log.info(StringUtils.rightPad("检查是否取消", 180));
        log.info(StringUtils.rightPad("*********************************************************", 180));
    }

}
