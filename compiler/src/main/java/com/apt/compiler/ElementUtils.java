package com.apt.compiler;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * @ProjectName: AptJavaDemo
 * @Package: com.apt.compiler
 * @ClassName: ElementUtils
 * @Description: java类作用描述
 * @Author: Jeffray
 * @CreateDate: 2020/6/19 10:28
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/6/19 10:28
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class ElementUtils {
    //获取包名
    public static String getPackageName(Elements elementUtils, TypeElement typeElement) {
        return elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
    }

    //获取顶层类类名
    public static String getEnclosingClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString();
    }

    //获取静态内部类类名
    public static String getStaticClassName(TypeElement typeElement) {
        return getEnclosingClassName(typeElement) + "Holder";
    }
}
