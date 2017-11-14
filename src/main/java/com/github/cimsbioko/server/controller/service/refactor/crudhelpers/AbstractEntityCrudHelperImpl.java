package com.github.cimsbioko.server.controller.service.refactor.crudhelpers;

import com.github.cimsbioko.server.dao.GenericDao;
import com.github.cimsbioko.server.domain.util.UUIDGenerator;
import com.github.cimsbioko.server.controller.exception.ConstraintViolations;
import com.github.cimsbioko.server.controller.service.CurrentUser;
import com.github.cimsbioko.server.controller.service.EntityValidationService;
import com.github.cimsbioko.server.domain.model.AuditableEntity;
import com.github.cimsbioko.server.domain.service.SitePropertiesService;
import com.github.cimsbioko.server.domain.util.CalendarUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;


public abstract class AbstractEntityCrudHelperImpl<T extends AuditableEntity> implements EntityCrudHelper<T> {

    @Autowired
    protected GenericDao genericDao;
    @Autowired
    protected CurrentUser currentUser;
    @Autowired
    protected CalendarUtil calendarUtil;
    @Autowired
    protected SitePropertiesService sitePropertiesService;
    @Autowired
    protected EntityValidationService entityValidationService;

    @Transactional
    @Override
    public void delete(T entity) throws IllegalArgumentException {
        entity.setDeleted(true);
        entityValidationService.setStatusVoided(entity);
        genericDao.update(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void create(T entity) throws ConstraintViolations {

        if (null != currentUser) {
            entity.setInsertBy(currentUser.getCurrentUser());
        }

        //TODO: there are too many places that validations are happening - or they are atleast organized poorly.
        //TODO: clean up the validations into well defined blobs of logic.

        // is the entity eligble?
        preCreateSanityChecks(entity);

        // complete the cascading effects of creating this entity
        cascadeReferences(entity);

        // evaluate the changes, sanity check
        validateReferences(entity);


        Calendar insertDate = calendarUtil.dateToCalendar(new Date());
        entity.setInsertDate(insertDate);

        setEntityUuidIfNull(entity);

        entityValidationService.setStatusPending(entity);
        entityValidationService.validateEntity(entity);


        genericDao.create(entity);

    }

    @Transactional
    @Override
    public void save(T entity) throws ConstraintViolations {
        validateReferences(entity);
        entityValidationService.setStatusPending(entity);
        entityValidationService.validateEntity(entity);
        genericDao.update(genericDao.merge(entity));
    }

    public static void setEntityUuidIfNull(AuditableEntity entity) {
        if (null == entity.getUuid() || entity.getUuid().isEmpty() || entity.getUuid().equals("null")) {
            entity.setUuid(UUIDGenerator.generate());
        }
    }

    protected abstract void preCreateSanityChecks(T entity) throws ConstraintViolations;

    protected abstract void cascadeReferences(T entity) throws ConstraintViolations;

    protected abstract void validateReferences(T entity) throws ConstraintViolations;
}

