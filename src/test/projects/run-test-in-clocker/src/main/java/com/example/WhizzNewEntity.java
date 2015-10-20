package com.example;

import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.core.sensor.Sensors;
import org.apache.brooklyn.entity.software.base.EmptySoftwareProcess;

@ImplementedBy(WhizzNewEntityImpl.class)
public interface WhizzNewEntity extends EmptySoftwareProcess {

    AttributeSensor<String> FIXED_VALUE = Sensors.newStringSensor("some.sensor");

}