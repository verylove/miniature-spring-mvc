package com.gupaoedu.vip.mvc.framework.annotation;

import java.lang.annotation.*;

/**
 * TODO
 *
 * @author lrj
 * @see
 * @since 2018.01.17
 */
@Target(ElementType.TYPE) //表示这个注解是加到我们的class上的
@Retention(RetentionPolicy.RUNTIME) //这是运行时生效
@Documented //这个注解只做说明使用
public @interface GPController {
    String value() default ""; //默认值是空
}
