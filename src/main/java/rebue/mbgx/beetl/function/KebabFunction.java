package rebue.mbgx.beetl.function;

import org.beetl.core.Context;
import org.beetl.core.Function;

/**
 * 将驼峰格式转换为羊肉串格式
 * 例如：helloWorld 变成 hello-world
 *
 * @author zbz
 */
public class KebabFunction implements Function {

    @Override
    public Object call(final Object[] paras, final Context ctx) {
        if (paras == null || paras.length != 1 || !(paras[0] instanceof String)) {
            final String msg = "参数不正确(String)";
            throw new IllegalArgumentException(msg);
        }
        final String        str = (String) paras[0];
        final StringBuilder sb  = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            final char ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i != 0) {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

}
