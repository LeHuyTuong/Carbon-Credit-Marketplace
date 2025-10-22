package com.carbonx.marketcarbon.utils;

public final class CodeGenerator {

    private CodeGenerator() {}


//      lấy tối đa 3 ký tự A-Z/0-9 từ name (bỏ dấu/khoảng trắng/ký tự đặc biệt),
//      uppercase + gắn ID phía sau (đảm bảo uniqueness).
//      Ví dụ: "VinFast Việt Nam" + 12 => "VFV12"

    public static String slug3WithId(String name, String fallback, Long id) {
        String base = (name == null || name.isBlank()) ? fallback : name;
        // giữ A-Z/0-9, bỏ ký tự khác
        base = base.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (base.isEmpty()) base = fallback;
        base = base.substring(0, Math.min(3, base.length()));
        return base + id;
    }
}
