package com.gupaoedu.vip.mvc.framework.annotation;

import java.lang.annotation.*;

/**
 * TODO
 *
 * @author lrj
 * @see
 * @since 2018.01.17
 */
@Target({ElementType.METHOD}) //描述返回值类型，所以定义在方法上
@Retention(RetentionPolicy.RUNTIME) //这是运行时生效
@Documented //这个注解只做说明使用
public @interface GPResponseBody {
    String value() default "";
    
    boolean required() default true; //表示这个参数是不是必填，默认必填true
}
