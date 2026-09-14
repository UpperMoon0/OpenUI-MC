package com.nstut.openui.api;

import java.lang.annotation.*;

/** Marks implementation detail with no compatibility guarantee. */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface Internal { }
