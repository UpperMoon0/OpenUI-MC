package com.nstut.openui.api;

import java.lang.annotation.*;

/** Documents the OpenUI version that introduced an API. */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface Since {
    String value();
}
