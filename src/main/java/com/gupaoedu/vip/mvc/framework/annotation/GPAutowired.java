package com.gupaoedu.vip.mvc.framework.annotation;

import java.lang.annotation.*;

/**
 * TODO
 *
 * @author lrj
 * @see
 * @since 2018.01.17
 */
@Target(ElementType.FIELD) //autowired是写在我们的属性上面，所以这个是@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME) //这是运行时生效
@Documented //这个注解只做说明使用
public @interface GPAutowired {
    String value() default ""; //默认值是空
    
}
