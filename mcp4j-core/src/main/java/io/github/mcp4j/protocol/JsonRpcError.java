package io.github.mcp4j.protocol;

/**
 * JSON-RPC 2.0 error object.
 */
public record JsonRpcError(int code, String message) {

    public static final int PARSE_ERROR      = -32700;
    public static final int INVALID_REQUEST  = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS   = -32602;
    public static final int INTERNAL_ERROR   = -32603;

    public static JsonRpcError parseError(String detail) {
        return new JsonRpcError(PARSE_ERROR, "Parse error: " + detail);
    }

    public static JsonRpcError methodNotFound(String method) {
        return new JsonRpcError(METHOD_NOT_FOUND, "Method not found: " + method);
    }

    public static JsonRpcError invalidParams(String detail) {
        return new JsonRpcError(INVALID_PARAMS, "Invalid params: " + detail);
    }

    public static JsonRpcError internalError(String detail) {
        return new JsonRpcError(INTERNAL_ERROR, "Internal error: " + detail);
    }
}
