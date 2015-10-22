package com.example.test;

import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.entity.software.base.EmptySoftwareProcess;

@ImplementedBy(ExceptionalEntityImpl.class)
public interface ExceptionalEntity extends EmptySoftwareProcess {

}