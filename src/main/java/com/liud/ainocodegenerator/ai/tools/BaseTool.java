package com.liud.ainocodegenerator.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;

import java.util.Map;

/**
 * 工具基类
 * 定义所有工具的通用接口
 */
public abstract class BaseTool {

    /**
     * 获取工具的英文名称（对应方法名）
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具支持的所有方法名
     */
    public String[] getSupportedToolNames() {
        return new String[]{getToolName()};
    }

    /**
     * 获取工具的中文显示名称
     *
     * @return 工具中文名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求时的返回值（显示给用户）
     *
     * @return 工具请求显示内容
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 处理工具请求阶段的展示逻辑
     */
    public String handleToolRequest(JSONObject arguments, Map<String, StringBuilder> chunkContentMap) {
        return generateToolRequestResponse();
    }

    /**
     * 生成工具执行结果格式（保存到数据库）
     *
     * @param arguments 工具执行参数
     * @return 格式化的工具执行结果
     */
    public abstract String generateToolExecutedResult(JSONObject arguments);

    /**
     * 处理工具执行完成后的展示逻辑
     */
    public String handleToolExecuted(String result, JSONObject arguments, Map<String, StringBuilder> chunkContentMap) {
        if (!isToolExecutionSuccess(result)) {
            return formatGenericToolExecutedOutput(getToolName(), result);
        }
        return generateToolExecutedResult(arguments);
    }

    protected boolean isToolExecutionSuccess(String result) {
        return StrUtil.isNotBlank(result) && !result.contains("失败");
    }

    protected String formatGenericToolExecutedOutput(String toolName, String result) {
        return "\n\n[工具-" + toolName + "-执行结果] => " + result + "\n\n";
    }

    protected String formatFileCodeOutput(String relativeFilePath, String content) {
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String lang = StrUtil.blankToDefault(suffix, "text");
        return String.format("""


                [源码文件] %s
                ````%s file=%s
                %s
                ````


                """, relativeFilePath, lang, relativeFilePath, content);
    }
}
