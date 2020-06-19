package com.apt.compiler;

import com.apt.annotation.ARouter;
import com.apt.annotation.BindView;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理（新增annotation module）
@SupportedAnnotationTypes({"com.apt.annotation.ARouter", "com.apt.annotation.BindView"})
// 指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
// 注解处理器接收的参数
@SupportedOptions("content")
public class ARouterProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Messager messager;
    private Filer filer;

    // 该方法主要用于一些初始化的操作，通过该方法的参数ProcessingEnvironment可以获取一些列有用的工具类
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        // 父类受保护属性，可以直接拿来使用。
        // 其实就是init方法的参数ProcessingEnvironment
        // processingEnv.getMessager(); //参考源码64行
        elementUtils = processingEnvironment.getElementUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();

        // 通过ProcessingEnvironment去获取build.gradle传过来的参数
        String content = processingEnvironment.getOptions().get("content");
        // 有坑：Diagnostic.Kind.ERROR，异常会自动结束，不像安卓中Log.e那么好使
        messager.printMessage(Diagnostic.Kind.NOTE, content);
    }

    /**
     * 相当于main函数，开始处理注解
     * 注解处理器的核心方法，处理具体的注解，生成Java文件
     *
     * @param set              使用了支持处理注解的节点集合（类 上面写了注解）
     * @param roundEnvironment 当前或是之前的运行环境,可以通过该对象查找找到的注解。
     * @return true 表示后续处理器不会再处理（已经处理完成）
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) {
            return false;
        }

        // 获取所有带ARouter注解的 类节点
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(ARouter.class);
        // 遍历所有类节点
        for (Element element : elements) {
            messager.printMessage(Diagnostic.Kind.NOTE, "element有：" + element.toString() + "::" + element.getKind());
            // 通过类节点获取包节点（全路径：com.netease.xxx）
            String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
            // 获取简单类名
            String className = element.getSimpleName().toString();
            messager.printMessage(Diagnostic.Kind.NOTE, "被注解的类有：" + className);
            // 最终想生成的类文件名
            String finalClassName = className + "$$ARouter";
//            useNormal(element, packageName, className, finalClassName);
            userJavaPoet(element, packageName, finalClassName);

        }

        // 遍历所有BindView注解的节点
        Set<? extends Element> bindViewElements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
        // 类为 key，类下被注解的成员为 value
        // 成员由当前注解的value作为 key，当前成员为 value
        Map<TypeElement, Map<Integer, VariableElement>> typeElementMapHashMap = new HashMap<>();
        for (Element element : bindViewElements) {
            messager.printMessage(Diagnostic.Kind.NOTE, "element有：" + element.toString() + "::" + element.getKind());
            if (element.getKind() != ElementKind.FIELD) {
                continue;
            } else {
                // 转换成 VariableElement
                VariableElement variableElement = (VariableElement) element;
                // getEnclosingElement 方法返回封装此 Element 的最里层元素
                // 如果 Element 直接封装在另一个元素的声明中，则返回该封装元素
                // 此处表示的即 Activity 类对象
                TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
                Map<Integer, VariableElement> variableElementMap = typeElementMapHashMap.get(typeElement);
                if (variableElementMap == null) {
                    variableElementMap = new HashMap<>();
                    typeElementMapHashMap.put(typeElement, variableElementMap);
                }
                //获取注解值，即 ViewId
                BindView bindAnnotation = variableElement.getAnnotation(BindView.class);
                int viewId = bindAnnotation.value();
                //将每个包含了 BindView 注解的字段对象以及其注解值保存起来
                variableElementMap.put(viewId, variableElement);
            }
        }
        messager.printMessage(Diagnostic.Kind.NOTE, "typeElementMapHashMap：" + typeElementMapHashMap.toString());
        for (TypeElement key : typeElementMapHashMap.keySet()) {
            Map<Integer, VariableElement> elementMap = typeElementMapHashMap.get(key);
            String packageName = ElementUtils.getPackageName(elementUtils, key);
            JavaFile javaFile = JavaFile.builder(packageName, generateCodeByPoet(key,elementMap)).build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    /**
     * 使用JavaPoet的方式
     */
    private void userJavaPoet(Element element, String packageName, String finalClassName) {
        // 高级写法，javapoet构建工具，参考（https://github.com/JakeWharton/butterknife）
        try {
            // 获取类之上@ARouter注解的path值
            ARouter aRouter = element.getAnnotation(ARouter.class);
            /**构建方法*/
            MethodSpec methodSpec = MethodSpec
                    // 方法名
                    .methodBuilder("findTargetClassUseJavaPoet")
                    // 修饰
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    // 返回值
                    .returns(Class.class)
                    // 参数(String path)
                    .addParameter(String.class, "path")
                    // 方法内容拼接：
                    // return path.equals("/app/MainActivity") ? MainActivity.class : null
                    .addStatement("return path.equals($S) ? $T.class : null", aRouter.path(), ClassName.get((TypeElement) element))
                    .build();
            /**构建类*/
            TypeSpec typeSpec = TypeSpec
                    // 类名
                    .classBuilder(finalClassName)
                    // 修饰
                    .addModifiers(Modifier.PUBLIC)
                    // 添加方法
                    .addMethod(methodSpec)
                    .build();
            // 在指定的包名下，生成Java类文件，并写入数据
            JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                    .build();
            javaFile.writeTo(filer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 正常写文件，一行一行写
     */
    private void useNormal(Element element, String packageName, String className, String finalClassName) {
        // 公开课写法，也是EventBus写法（https://github.com/greenrobot/EventBus）
        try {
            // 创建一个新的源文件（Class），并返回一个对象以允许写入它
            JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + finalClassName);
            // 定义Writer对象，开启写入
            Writer writer = sourceFile.openWriter();
            // 设置包名
            writer.write("package " + packageName + ";\n");

            writer.write("public class " + finalClassName + " {\n");

            writer.write("public static Class<?> findTargetClass(String path) {\n");

            // 获取类之上@ARouter注解的path值
            ARouter aRouter = element.getAnnotation(ARouter.class);

            writer.write("if (path.equals(\"" + aRouter.path() + "\")) {\n");

            writer.write("return " + className + ".class;\n}\n");

            writer.write("return null;\n");

            writer.write("}\n}");

            // 最后结束别忘了
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成 Java 类
     *
     * @param typeElement        注解对象上层元素对象，即 Activity 对象
     * @param variableElementMap Activity 包含的注解对象以及注解的目标对象
     * @return
     */
    private TypeSpec generateCodeByPoet(TypeElement typeElement, Map<Integer, VariableElement> variableElementMap) {
        //自动生成的文件以 Activity名 + ViewBinding 进行命名
        return TypeSpec.classBuilder(ElementUtils.getEnclosingClassName(typeElement) + "ViewBinding")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(generateMethodByPoet(typeElement, variableElementMap))
                .build();
    }

    /**
     * 生成方法
     *
     * @param typeElement        注解对象上层元素对象，即 Activity 对象
     * @param variableElementMap Activity 包含的注解对象以及注解的目标对象
     * @return
     */
    private MethodSpec generateMethodByPoet(TypeElement typeElement, Map<Integer, VariableElement> variableElementMap) {
        ClassName className = ClassName.bestGuess(typeElement.getQualifiedName().toString());
        //方法参数名
        String parameter = "_" + StringUtils.toLowerCaseFirstChar(className.simpleName());
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("bind")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(className, parameter);
        for (int viewId : variableElementMap.keySet()) {
            VariableElement element = variableElementMap.get(viewId);
            //被注解的字段名
            String name = element.getSimpleName().toString();
            //被注解的字段的对象类型的全名称
            String type = element.asType().toString();
            String text = "{0}.{1}=({2})({3}.findViewById({4}));\n";
            methodBuilder.addCode(MessageFormat.format(text, parameter, name, type, parameter, String.valueOf(viewId)));
        }
        return methodBuilder.build();
    }
}