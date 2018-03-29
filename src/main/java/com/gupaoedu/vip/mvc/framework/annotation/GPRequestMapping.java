package com.gupaoedu.vip.mvc.framework.annotation;

import java.lang.annotation.*;

/**
 * TODO
 *
 * @author lrj
 * @see
 * @since 2018.01.17
 */
@Target({ElementType.METHOD,ElementType.TYPE}) //写在方法上和写在类上
@Retention(RetentionPolicy.RUNTIME) //这是运行时生效
@Documented //这个注解只做说明使用
public @interface GPRequestMapping {
    String value() default "";
}
