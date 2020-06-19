package com.apt.compiler;

/**
 * @ProjectName: AptJavaDemo
 * @Package: com.apt.compiler
 * @ClassName: StringUtils
 * @Description: java类作用描述
 * @Author: Jeffray
 * @CreateDate: 2020/6/19 10:28
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/6/19 10:28
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class StringUtils {
    //将首字母转为小写
    public static String toLowerCaseFirstChar(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }
        if (Character.isLowerCase(text.charAt(0))) {
            return text;
        }
        return String.valueOf(Character.toLowerCase(text.charAt(0))) + text.substring(1);
    }

    //将首字母转为大写
    public static String toUpperCaseFirstChar(String text) {
        if (Character.isUpperCase(text.charAt(0))) {
            return text;
        } else {
            return String.valueOf(Character.toUpperCase(text.charAt(0))) + text.substring(1);
        }
    }
}
