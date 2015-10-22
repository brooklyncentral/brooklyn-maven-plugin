package com.example.test;

import org.apache.brooklyn.entity.software.base.EmptySoftwareProcessImpl;

public class ExceptionalEntityImpl extends EmptySoftwareProcessImpl implements ExceptionalEntity {

    @Override
    protected void preStart() {
        throw new RuntimeException("On second thoughts let's not start.");
    }

}
