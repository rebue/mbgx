package rebue.mbgx.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemarksUtil {
    private final static Logger _log = LoggerFactory.getLogger(RemarksUtil.class);

    /**
     * 通过备注得到标题（有些标题是名称+（1-xxx,2-xxx），去掉后面的内容来取得名称）
     * 
     * @param remarks
     *            字段或表的备注
     * @return
     */
    public static String getTitleByRemarks(String remarks) {
        _log.debug("getTitleByRemarks: " + remarks);
        if (StringUtils.isBlank(remarks))
            return "";
        int i = 1;
        for (; i < remarks.length(); i++) {
            char ch = remarks.charAt(i);
            if (ch == '(' || ch == ' ' || ch == '（' || ch == '\n' || ch == '\r') {
                break;
            }
        }
        return remarks.substring(0, i);
    }

    /**
     * 获取根据换行符拼接的备注
     * 
     * @param remarks
     * @return
     */
    public static String getSplitJointRemarks(String remarks) {
        _log.trace("getSplitJointRemarks: " + remarks);

        // 按行分割
        String[] remarksArr = remarks.split("[\\n\\r]");

        // 去除空行
        List<String> remarkList = new ArrayList<>();
        for (int i = 0; i < remarksArr.length; i++) {
            if (!StringUtils.isBlank(remarksArr[i])) {
                remarkList.add(remarksArr[i]);
            }
        }

        String sRemarks = "";
        if (remarkList.isEmpty())
            sRemarks = "";
        else if (remarkList.size() == 1)
            sRemarks = remarks;
        else {
            for (int i = 0; i < remarkList.size(); i++) {
                // 如果是第一行
                if (i == 0)
                    sRemarks += remarkList.get(i) + "\\n\"\n";
                // 如果是最后一行
                else if (i == remarkList.size() - 1)
                    sRemarks += "             +\"" + remarkList.get(i);
                // 如果是中间行
                else
                    sRemarks += "             +\"" + remarkList.get(i) + "\\n\"\n";
            }
        }

        return "\"" + sRemarks + "\"";
    }

}
