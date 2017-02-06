package com.github.cimsbioko.server.domain.constraint.impl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.github.cimsbioko.server.domain.constraint.AppContextAware;
import com.github.cimsbioko.server.domain.constraint.CheckIndividualGenderMale;
import com.github.cimsbioko.server.domain.model.Individual;
import com.github.cimsbioko.server.domain.service.impl.SitePropertiesServiceImpl;

public class CheckIndividualGenderMaleImpl extends AppContextAware implements ConstraintValidator<CheckIndividualGenderMale, Individual> {

    private SitePropertiesServiceImpl properties;
    private boolean allowNull;

    public void initialize(CheckIndividualGenderMale arg0) {
        properties = (SitePropertiesServiceImpl) context.getBean("siteProperties");
        this.allowNull = arg0.allowNull();
    }

    public boolean isValid(Individual arg0, ConstraintValidatorContext arg1) {

        if (allowNull && arg0 == null) {
            return true;
        }

        if (arg0.getExtId().equals(properties.getUnknownIdentifier()))
            return true;

        return arg0.getGender().equals(properties.getMaleCode());

    }
}