/**
 * 测试包
 * @author zbz
 */
package mbgx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 供解析测试的类
 * 
 * @author zbz
 *
 */
public class Hello implements A {
    /**
     * 日志
     * 
     */
    private final static Logger _log = LoggerFactory.getLogger(Hello.class);

    /**
     * 名称
     * 
     */
    private String              name = "张三";

    /**
     * 构造方法
     * 
     */
    public Hello() {
        super();
    }

    /**
     * 构造方法
     * 
     */
    public Hello(final String name) {
        super();
        _log.info(String.format("Hello, %s", this.name));
        this.name = name;
    }

    /**
     * 测试hello方法
     */
    @Override
    public String hello(final String name) {
        _log.info(String.format("Hello, %s", name));
        return "ok";
    }

    /**
     * 主方法
     * 
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
 *
 */
interface A {
    /**
     * hello方法
     * 
     * @param name
     *            名称
     * @return 内容
     */
    String hello(String name);
}
