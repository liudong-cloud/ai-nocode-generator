package com.liud.ainocodegenerator.core.diagnostics;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 工具调用参数诊断工具
 * 用于在不完整输出超长源码的前提下记录参数摘要，方便排查非法 JSON arguments。
 */
public final class ToolArgumentsDiagnostics {

    private static final int PREVIEW_EDGE_LENGTH = 120;

    private ToolArgumentsDiagnostics() {
    }

    public static String summarizeRawArguments(String arguments) {
        if (arguments == null) {
            return "args=null";
        }
        StringBuilder summary = new StringBuilder();
        summary.append("argsLength=").append(arguments.length())
                .append(", newlineCount=").append(StrUtil.count(arguments, '\n'))
                .append(", startsWithBrace=").append(arguments.stripLeading().startsWith("{"))
                .append(", endsWithBrace=").append(arguments.stripTrailing().endsWith("}"));
        try {
            JSONObject argumentObj = JSONUtil.parseObj(arguments);
            summary.append(", jsonValid=true")
                    .append(", keys=").append(argumentObj.keySet())
                    .append(", payload=").append(summarizeArgumentObject(argumentObj));
        } catch (Exception e) {
            summary.append(", jsonValid=false")
                    .append(", parseError=").append(e.getClass().getSimpleName())
                    .append(':').append(StrUtil.blankToDefault(e.getMessage(), "<no-message>"))
                    .append(", rawPreview=").append(preview(arguments));
        }
        return summary.toString();
    }

    public static String summarizeWriteFileInvocation(String relativeFilePath, String content) {
        return "relativeFilePath=" + StrUtil.blankToDefault(relativeFilePath, "<missing>")
                + ", contentLength=" + (content == null ? -1 : content.length())
                + ", contentPreview=" + preview(content);
    }

    public static String summarizeWriteFileChunkInvocation(String relativeFilePath,
                                                           String chunkContent,
                                                           Integer chunkIndex,
                                                           Boolean lastChunk) {
        return "relativeFilePath=" + StrUtil.blankToDefault(relativeFilePath, "<missing>")
                + ", chunkIndex=" + chunkIndex
                + ", lastChunk=" + lastChunk
                + ", chunkLength=" + (chunkContent == null ? -1 : chunkContent.length())
                + ", chunkPreview=" + preview(chunkContent);
    }

    public static String summarizeArgumentObject(JSONObject argumentObj) {
        String relativeFilePath = argumentObj.getStr("relativeFilePath");
        if (argumentObj.containsKey("chunkContent") || argumentObj.containsKey("chunkIndex") || argumentObj.containsKey("lastChunk")) {
            return summarizeWriteFileChunkInvocation(
                    relativeFilePath,
                    argumentObj.getStr("chunkContent"),
                    argumentObj.getInt("chunkIndex"),
                    argumentObj.getBool("lastChunk")
            );
        }
        return summarizeWriteFileInvocation(relativeFilePath, argumentObj.getStr("content"));
    }

    public static String summarizeText(String text) {
        return "length=" + (text == null ? -1 : text.length())
                + ", preview=" + preview(text);
    }

    private static String preview(String text) {
        if (text == null) {
            return "<null>";
        }
        String normalized = text
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
        if (normalized.length() <= PREVIEW_EDGE_LENGTH * 2) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_EDGE_LENGTH)
                + " ... "
                + normalized.substring(normalized.length() - PREVIEW_EDGE_LENGTH);
    }
}


