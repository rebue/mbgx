/**
 * 测试包
 *
 * @author zbz
 */
package mbgx;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

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
        // 测试代码中的注释1
        log.info(String.format("Hello, %s", name));
        // 测试代码中的注释2
        return "ok";
        // 测试代码中的注释3
    }

    /**
     * 主方法
     *
     * @ignore
     * @mbg.generated
     */
    public static void main(final String[] args) {
        final Hello  hello = new Hello();
        final String world = "world1";
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
