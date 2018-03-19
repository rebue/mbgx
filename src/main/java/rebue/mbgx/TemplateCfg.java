package rebue.mbgx;

public class TemplateCfg {
	/**
	 * 模板的名称
	 */
	private String templateName;
	/**
	 * 要生成文件的目录
	 */
	private String targetDir;
	/**
	 * 要生成的文件
	 */
	private String targetFile;
	/**
	 * 覆盖原文件前是否备份
	 */
	private Boolean backup;
	
	/**
	 * 中间表是否生成目标文件
	 */
	private Boolean isGenTargetOnMiddleTable;

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public String getTargetDir() {
		return targetDir;
	}

	public void setTargetDir(String targetDir) {
		this.targetDir = targetDir;
	}

	public String getTargetFile() {
		return targetFile;
	}

	public void setTargetFile(String targetFile) {
		this.targetFile = targetFile;
	}

	public Boolean getBackup() {
		return backup;
	}

	public void setBackup(Boolean backup) {
		this.backup = backup;
	}

	public Boolean getIsGenTargetOnMiddleTable() {
		return isGenTargetOnMiddleTable;
	}

	public void setIsGenTargetOnMiddleTable(Boolean isGenTargetOnMiddleTable) {
		this.isGenTargetOnMiddleTable = isGenTargetOnMiddleTable;
	}

}