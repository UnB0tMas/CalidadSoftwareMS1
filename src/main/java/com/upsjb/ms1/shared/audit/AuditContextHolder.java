package com.upsjb.ms1.shared.audit;

public final class AuditContextHolder {

    private static final ThreadLocal<AuditContext> HOLDER = new ThreadLocal<>();

    private AuditContextHolder() {
    }

    public static void set(AuditContext context) {
        HOLDER.set(context == null ? AuditContext.empty() : context);
    }

    public static AuditContext get() {
        AuditContext context = HOLDER.get();
        return context == null ? AuditContext.empty() : context;
    }

    public static String getRequestIdOrNull() {
        return get().requestId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}