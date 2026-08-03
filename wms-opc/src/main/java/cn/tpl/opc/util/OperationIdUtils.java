package cn.tpl.opc.util;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public final class OperationIdUtils {
    public static final int MAX_LENGTH = 64;

    private OperationIdUtils() {
    }

    public static String ensure(String operationId) {
        String normalized = StringUtils.trimToNull(operationId);
        return normalized != null && normalized.length() <= MAX_LENGTH
                ? normalized
                : UUID.randomUUID().toString();
    }
}
