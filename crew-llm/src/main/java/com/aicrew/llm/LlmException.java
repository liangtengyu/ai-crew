package com.aicrew.llm;

/** 大模型调用异常（含 HTTP 状态与响应片段） */
public class LlmException extends RuntimeException {

    private final int status;

    public LlmException(String message) {
        this(message, 0, null);
    }

    public LlmException(String message, int status, String body) {
        super(status > 0 ? message + " [HTTP " + status + "] " + snippet(body) : message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    private static String snippet(String body) {
        if (body == null) {
            return "";
        }
        String s = body.replaceAll("\\s+", " ").trim();
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
