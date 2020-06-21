package rebue.mbgx.custom;

import org.mybatis.generator.api.ProgressCallback;

import lombok.extern.slf4j.Slf4j;

/**
 * 用日志记录进度
 * 
 * @author zbz
 *
 */
@Slf4j
public class ProgressCallbackEx implements ProgressCallback {

    @Override
    public void introspectionStarted(final int totalTasks) {
        log.info("************************************************************************");
        log.info("* MBGX开始执行任务: 总任务数-{}                                            *", totalTasks);
        log.info("************************************************************************");
    }

    @Override
    public void generationStarted(final int totalTasks) {
        log.info("************************************************************************");
        log.info("* 开始生成代码: 任务数-{}                                                 *", totalTasks);
        log.info("************************************************************************");
    }

    @Override
    public void saveStarted(final int totalTasks) {
        log.info("************************************************************************");
        log.info("* 开始保存文件: 任务数-{}                                                 *", totalTasks);
        log.info("************************************************************************");
    }

    @Override
    public void startTask(final String taskName) {
        log.info("************************************************************************");
        log.info("* 开始执行任务: 任务名称-{}                                                *", taskName);
        log.info("************************************************************************");
    }

    @Override
    public void done() {
        log.info("************************************************************************");
        log.info("* MBGX所有任务执行完成！！！                                               *");
        log.info("************************************************************************");
    }

    @Override
    public void checkCancel() throws InterruptedException {
        log.info("************************************************************************");
        log.info("* 检查是否取消                                                           *");
        log.info("************************************************************************");
    }

}
