/**
 * 测试包
 *
 * @author zbz
 */
package mbgx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 供解析测试的类
 *
 * @author zbz
 * @mbg.removedMember _log
 */
@Slf4j
@Service
public class Hello implements A {

    /**
     * 名称
     *
     * @ignore
     * @mbg.generated
     */
    private String name = "李四";

    /**
     * 构造方法
     */
    public Hello() {
        super();
    }

    /**
     * 构造方法
     */
    public Hello(final String name) {
        super();
        log.info(String.format("Hello, %s", this.name));
        this.name = name;
    }

    /**
     * 测试hello方法
     */
    @Override
    public String hello(final String name) {
        log.info(String.format("Hello, %s", name));
        return "ok";
    }

    /**
     * 主方法
     *
     * @ignore
     * @mbg.generated
     */
    public static void main(final String[] args) {
        final Hello hello = new Hello();
        final String world = "world";
        System.out.println(hello.hello(world));
    }

}

/**
 * A接口
 *
 * @author zbz
 */
interface A {
    /**
     * hello方法
     *
     * @param name 名称
     * @return 内容
     */
    String hello(String name);
}
