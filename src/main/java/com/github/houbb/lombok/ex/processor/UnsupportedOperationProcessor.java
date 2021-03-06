package com.github.houbb.lombok.ex.processor;

import com.github.houbb.lombok.ex.annotation.UnsupportedOperation;
import com.github.houbb.lombok.ex.metadata.LMethod;
import com.github.houbb.lombok.ex.util.AstUtil;
import com.github.houbb.lombok.ex.util.ExceptionUtil;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author binbin.hou
 * @since 0.0.9
 */
@SupportedAnnotationTypes("com.github.houbb.lombok.ex.annotation.UnsupportedOperation")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class UnsupportedOperationProcessor extends BaseMethodProcessor {

    @Override
    protected Class<? extends Annotation> getAnnotationClass() {
        return UnsupportedOperation.class;
    }

    /**
     * @param method 单个方法
     */
    @Override
    protected void handleMethod(LMethod method) {
        final Element element = method.methodSymbol();
        //1. 包名称
        // 后期可以添加自定义的异常类和错误描述
        final Class<? extends RuntimeException> clazz = UnsupportedOperationException.class;
        String fullClassName = clazz.getName();
        AstUtil.importPackage(processContext, element, fullClassName);
        AstUtil.importPackage(processContext, element, ExceptionUtil.class.getName());

        //2. 生成代码
        this.generateBlockCode(clazz, element);
    }

    /**
     * 生成代码
     *
     * <pre>
     *     throw ExceptionUtil.throwException(clazz);
     * </pre>
     * @param clazz 类信息
     * @param element 元素
     * @since 0.0.9
     */
    private void generateBlockCode(final Class clazz, final Element element) {
        final TreeMaker treeMaker = processContext.treeMaker();
        final Names names = processContext.names();
        final Trees trees = processContext.trees();

        JCTree tree = (JCTree) trees.getTree(element);

        tree.accept(new TreeTranslator() {
            @Override
            public void visitBlock(JCTree.JCBlock tree) {
                ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
                statements.addAll(tree.getStatements());

                //2. 构建 statement
                JCTree.JCFieldAccess fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString("ExceptionUtil")), names.fromString("throwException"));

                // 避免类型擦除
                ListBuffer<JCTree.JCExpression> argListBuffer = new ListBuffer<>();
                String className = clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1);
                JCTree.JCIdent classIdent = treeMaker.Ident(names.fromString(className));
                JCTree.JCFieldAccess argFieldAccess = treeMaker.Select(classIdent, names.fromString("class"));

                argListBuffer.add(argFieldAccess);
                JCTree.JCMethodInvocation methodInvocation = treeMaker.Apply(List.<JCTree.JCExpression>nil(),
                        fieldAccess, argListBuffer.toList());
                JCTree.JCExpressionStatement code = treeMaker.Exec(methodInvocation);
                statements.add(code);

                result = treeMaker.Block(0, statements.toList());
            }
        });
    }

    /**
     * 生成代码
     *
     * <pre>
     *     throw new UnsupportedOperationException();
     * </pre>
     * @param fullClassName 全称
     * @param element 元素
     * @since 0.0.9
     */
    @Deprecated
    private void generateBlockCode(final String fullClassName, final Element element) {
        final TreeMaker treeMaker = processContext.treeMaker();
        final Names names = processContext.names();
        final Trees trees = processContext.trees();

        JCTree tree = (JCTree) trees.getTree(element);

        tree.accept(new TreeTranslator() {
            @Override
            public void visitBlock(JCTree.JCBlock tree) {
                ListBuffer<JCTree.JCStatement> statements = new ListBuffer<JCTree.JCStatement>();
                for (int i = 0; i < tree.getStatements().size(); i++) {
                    JCTree.JCStatement statement = tree.getStatements().get(i);
                    statements.append(statement);
                }

                // 插入最后一个 throw new UnsupportedOperationException();
                String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
                JCTree.JCIdent jcIdent = treeMaker.Ident(names.fromString(className));
                /**
                 * this.encl = var1;
                 * this.typeargs = var2 == null ? List.nil() : var2;
                 * this.clazz = var3;
                 * this.args = var4;
                 * this.def = var5;
                 */
                JCTree.JCNewClass jcNewClass = treeMaker.NewClass(null, null, jcIdent, List.<JCTree.JCExpression>nil(),
                        null);
                JCTree.JCExpression jcTree = jcNewClass;
                // 打印出方法
                Method[] methods = treeMaker.getClass().getMethods();
                for(Method method : methods) {
                    if(method.getName().startsWith("Throw")) {
                        System.out.println(method.getName());
                        System.out.println(Arrays.toString(method.getParameterTypes()));
                    }
                }

                // TODO: 语法不兼容。
                // https://bz.apache.org/netbeans/show_bug.cgi?id=235347
                // http://www.docjar.com/html/api/com/sun/tools/javac/tree/TreeMaker.java.html
                JCTree.JCThrow jcThrow = treeMaker.Throw(jcTree);
                statements.add(jcThrow);

                result = treeMaker.Block(0, statements.toList());
            }
        });
    }

}
