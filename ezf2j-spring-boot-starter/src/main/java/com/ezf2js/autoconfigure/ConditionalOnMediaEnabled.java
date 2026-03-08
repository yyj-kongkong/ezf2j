package com.ezf2js.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 媒体服务启用条件注解
 * 当 ezf2j.media.enabled=true 时才生效
 *
 * @author ZJ
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = "ezf2j.media", name = "enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnMediaEnabled {
}
