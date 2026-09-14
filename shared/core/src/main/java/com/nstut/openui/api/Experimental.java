package com.nstut.openui.api;

import java.lang.annotation.*;

/** Marks API that may evolve before becoming part of the stable contract. */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface Experimental { }
