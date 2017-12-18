package com.github.cimsbioko.server.dao.impl;

import com.github.cimsbioko.server.domain.model.FieldWorker;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import com.github.cimsbioko.server.domain.service.SitePropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("fieldWorkerDao")
public class FieldWorkerDaoImpl extends BaseDaoImpl<FieldWorker, String> {

    @Autowired
    SitePropertiesService properties;

    public FieldWorkerDaoImpl() {
        super(FieldWorker.class);
    }

    @Override
    protected Criteria addImplicitRestrictions(Criteria criteria) {
        criteria = super.addImplicitRestrictions(criteria);
        return criteria.add(Restrictions.ne("extId", properties.getUnknownIdentifier()));
    }

    public SitePropertiesService getProperties() {
        return properties;
    }

    public void setProperties(SitePropertiesService properties) {
        this.properties = properties;
    }
}
