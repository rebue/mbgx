package rebue.mbgx.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RemarksUtils {
    private final static Logger _log = LoggerFactory.getLogger(RemarksUtils.class);

    /**
     * 通过备注得到标题（有些标题是名称+（1-xxx,2-xxx），去掉后面的内容来取得名称）
     *
     * @param remarks 字段或表的备注
     * @return
     */
    public static String getTitleByRemarks(final String remarks) {
        _log.debug("getTitleByRemarks: " + remarks);
        if (StringUtils.isBlank(remarks)) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < remarks.length(); i++) {
            final char ch = remarks.charAt(i);
            if (ch == '(' || ch == ' ' || ch == '（' || ch == '\n' || ch == '\r') {
                break;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * 获取根据换行符拼接的备注
     *
     * @param remarks
     * @return
     */
    public static String getSplitJointRemarks(final String remarks) {
        _log.trace("getSplitJointRemarks: " + remarks);

        // 按行分割
        final String[] remarksArr = remarks.split("[\\n\\r]");

        // 去除空行
        final List<String> remarkList = new ArrayList<>();
        for (final String element : remarksArr) {
            if (!StringUtils.isBlank(element)) {
                remarkList.add(element);
            }
        }

        String sRemarks = "";
        if (remarkList.isEmpty()) {
            sRemarks = "";
        } else if (remarkList.size() == 1) {
            sRemarks = remarks;
        } else {
            for (int i = 0; i < remarkList.size(); i++) {
                // 如果是第一行
                if (i == 0) {
                    sRemarks += remarkList.get(i) + "\\n\"\n";
                } else if (i == remarkList.size() - 1) {
                    sRemarks += "             +\"" + remarkList.get(i);
                    // 如果是中间行
                } else {
                    sRemarks += "             +\"" + remarkList.get(i) + "\\n\"\n";
                }
            }
        }

        return "\"" + sRemarks + "\"";
    }

}
