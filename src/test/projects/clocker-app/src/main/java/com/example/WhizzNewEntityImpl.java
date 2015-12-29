package com.example;

import org.apache.brooklyn.entity.software.base.EmptySoftwareProcessImpl;

public class WhizzNewEntityImpl extends EmptySoftwareProcessImpl implements WhizzNewEntity {

    @Override
    public void init() {
        super.init();
        sensors().set(FIXED_VALUE, "Hello from " + getClass().getName());
    }

}
