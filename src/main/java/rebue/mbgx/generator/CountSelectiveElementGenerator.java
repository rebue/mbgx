package rebue.mbgx.generator;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.ListUtilities;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;
import org.mybatis.generator.codegen.mybatis3.xmlmapper.elements.AbstractXmlElementGenerator;

import rebue.mbgx.util.BooleanUtils;

public class CountSelectiveElementGenerator extends AbstractXmlElementGenerator {

    @Override
    public void addElements(XmlElement parentElement) {
        XmlElement answer = new XmlElement("select"); //$NON-NLS-1$

        answer.addAttribute(new Attribute("id", "countSelective")); //$NON-NLS-1$
        answer.addAttribute(new Attribute("resultType", "int"));

        String parameterType = introspectedTable.getBaseRecordType();
        answer.addAttribute(new Attribute("parameterType", parameterType));

        context.getCommentGenerator().addComment(answer);

        StringBuilder sb = new StringBuilder();
        sb.append("select count(1) ");

        answer.addElement(new TextElement(sb.toString()));

        sb.setLength(0);
        sb.append("from "); //$NON-NLS-1$
        sb.append(introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime());
        answer.addElement(new TextElement(sb.toString()));

        XmlElement dynamicElement = new XmlElement("where"); //$NON-NLS-1$
        answer.addElement(dynamicElement);

        // XmlElement trimElement = new XmlElement("trim"); //$NON-NLS-1$
        // trimElement.addAttribute(new Attribute("prefix", "(")); //$NON-NLS-1$
        // //$NON-NLS-2$
        // trimElement.addAttribute(new Attribute("suffix", ")")); //$NON-NLS-1$
        // //$NON-NLS-2$
        // trimElement.addAttribute(new Attribute("prefixOverrides", "and"));
        // //$NON-NLS-1$ //$NON-NLS-2$
        // dynamicElement.addElement(trimElement);

        for (IntrospectedColumn introspectedColumn : ListUtilities.removeGeneratedAlwaysColumns(introspectedTable.getNonPrimaryKeyColumns())) {
            XmlElement isNotNullElement = new XmlElement("if"); //$NON-NLS-1$
            sb.setLength(0);
            sb.append(introspectedColumn.getJavaProperty());
            sb.append(" != null"); //$NON-NLS-1$
            if (introspectedColumn.getFullyQualifiedJavaType().getFullyQualifiedName().equals("java.lang.String")//
                    && !BooleanUtils.isBooleanColumn(introspectedColumn)) {
                sb.append(" and "); //$NON-NLS-1$
                sb.append(introspectedColumn.getJavaProperty());
                sb.append(" != ''"); //$NON-NLS-1$
            }
            isNotNullElement.addAttribute(new Attribute("test", sb.toString())); //$NON-NLS-1$
            dynamicElement.addElement(isNotNullElement);

            sb.setLength(0);
            sb.append("and ");
            sb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            sb.append(" = "); //$NON-NLS-1$
            sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));

            isNotNullElement.addElement(new TextElement(sb.toString()));
        }

        if (context.getPlugins().sqlMapSelectAllElementGenerated(answer, introspectedTable)) {
            parentElement.addElement(answer);
        }
    }

}
