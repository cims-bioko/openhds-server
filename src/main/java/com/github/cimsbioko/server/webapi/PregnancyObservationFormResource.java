package com.github.cimsbioko.server.webapi;

import com.github.cimsbioko.server.controller.exception.ConstraintViolations;
import com.github.cimsbioko.server.controller.service.VisitService;
import com.github.cimsbioko.server.controller.service.refactor.FieldWorkerService;
import com.github.cimsbioko.server.controller.service.refactor.IndividualService;
import com.github.cimsbioko.server.controller.service.refactor.PregnancyObservationService;
import com.github.cimsbioko.server.domain.annotations.Description;
import com.github.cimsbioko.server.domain.model.*;
import com.github.cimsbioko.server.domain.util.CalendarAdapter;
import com.github.cimsbioko.server.errorhandling.constants.ErrorConstants;
import com.github.cimsbioko.server.errorhandling.service.ErrorHandlingService;
import com.github.cimsbioko.server.errorhandling.util.ErrorLogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Calendar;

@Controller
@RequestMapping("/pregnancyObservationForm")
public class PregnancyObservationFormResource extends AbstractFormResource {

    @Autowired
    private IndividualService individualService;

    @Autowired
    private FieldWorkerService fieldWorkerService;

    @Autowired
    private VisitService visitService;

    @Autowired
    private PregnancyObservationService pregnancyObservationService;

    @Autowired
    private CalendarAdapter adapter;

    @Autowired
    private ErrorHandlingService errorService;

    private JAXBContext context = null;
    private Marshaller marshaller = null;

    @RequestMapping(method = RequestMethod.POST, produces = "application/xml", consumes = "application/xml")
    @Transactional
    public ResponseEntity<? extends Serializable> processForm(@RequestBody Form form)
            throws JAXBException {

        try {
            context = JAXBContext.newInstance(Form.class);
            marshaller = context.createMarshaller();
            marshaller.setAdapter(adapter);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create JAXB context and marshaller for PregnancyObservationFormResource");
        }

        ConstraintViolations cv = new ConstraintViolations();

        PregnancyObservation pregnancyObservation = new PregnancyObservation();
        pregnancyObservation.setRecordedDate(form.getRecordedDate());
        pregnancyObservation.setExpectedDeliveryDate(form.getExpectedDeliveryDate());

        FieldWorker fieldWorker = fieldWorkerService.getByUuid(form.getFieldWorkerUuid());
        if (null == fieldWorker) {
            cv.addViolations(ConstraintViolations.INVALID_FIELD_WORKER_UUID + " : " + form.getFieldWorkerUuid());
            ErrorLog errorLog = ErrorLogUtil.generateErrorLog(ErrorConstants.UNASSIGNED, createDTOPayload(form), null,
                    Form.LOG_NAME, null, ConstraintViolations.INVALID_FIELD_WORKER_UUID, cv.getViolations());
            errorService.logError(errorLog);
            return requestError(cv);
        }
        pregnancyObservation.setCollectedBy(fieldWorker);

        Individual individual = individualService.getByUuid(form.getIndividualUuid());
        if (null == individual) {
            cv.addViolations(ConstraintViolations.INVALID_INDIVIDUAL_UUID + " : " + form.getIndividualUuid());
            ErrorLog errorLog = ErrorLogUtil.generateErrorLog(ErrorConstants.UNASSIGNED, createDTOPayload(form), null,
                    Form.LOG_NAME, null, ConstraintViolations.INVALID_INDIVIDUAL_UUID, cv.getViolations());
            errorService.logError(errorLog);
            return requestError(cv);
        }
        pregnancyObservation.setMother(individual);

        Visit visit = visitService.findVisitByUuid(form.getVisitUuid());
        if (null == visit) {
            cv.addViolations(ConstraintViolations.INVALID_VISIT_UUID + " : " + form.getVisitUuid());
            ErrorLog errorLog = ErrorLogUtil.generateErrorLog(ErrorConstants.UNASSIGNED, createDTOPayload(form), null,
                    Form.LOG_NAME, null, ConstraintViolations.INVALID_VISIT_UUID, cv.getViolations());
            errorService.logError(errorLog);
            return requestError(cv);
        }

        pregnancyObservation.setVisit(visit);
        try {
            pregnancyObservationService.create(pregnancyObservation);
        } catch (ConstraintViolations e) {
            ErrorLog errorLog = ErrorLogUtil.generateErrorLog(ErrorConstants.UNASSIGNED, createDTOPayload(form), null,
                    Form.LOG_NAME, null, ErrorConstants.CONSTRAINT_VIOLATION, e.getViolations());
            errorService.logError(errorLog);
            return requestError(e);
        }

        return new ResponseEntity<>(form, HttpStatus.CREATED);

    }

    private String createDTOPayload(Form form) throws JAXBException {

        StringWriter writer = new StringWriter();
        marshaller.marshal(form, writer);
        return writer.toString();

    }


    @Description(description = "Model data from the PregnancyObservation xform for the Bioko island project.")
    @XmlRootElement(name = "pregnancyObservationForm")
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = Form.LOG_NAME)
    public static class Form implements Serializable {

        public static final String LOG_NAME = "PregnancyObservationForm";

        private static final long serialVersionUID = 1L;

        //core form fields
        @XmlElement(name = "processed_by_mirth")
        private boolean processedByMirth;

        @XmlElement(name = "entity_uuid")
        private String entityUuid;

        @XmlElement(name = "entity_ext_id")
        private String entityExtId;

        @XmlElement(name = "field_worker_ext_id")
        private String fieldWorkerExtId;

        @XmlElement(name = "field_worker_uuid")
        private String fieldWorkerUuid;

        @XmlElement(name = "collection_date_time")
        @XmlJavaTypeAdapter(CalendarAdapter.class)
        private Calendar collectionDateTime;

        //PregObs form fields
        @XmlElement(name = "individual_ext_id")
        private String individualExtId;

        @XmlElement(name = "individual_uuid")
        private String individualUuid;

        @XmlElement(name = "visit_ext_id")
        private String visitExtId;

        @XmlElement(name = "visit_uuid")
        private String visitUuid;

        @XmlElement(name = "expected_delivery_date")
        @XmlJavaTypeAdapter(CalendarAdapter.class)
        private Calendar expectedDeliveryDate;

        @XmlElement(name = "recorded_date")
        @XmlJavaTypeAdapter(CalendarAdapter.class)
        private Calendar recordedDate;

        public String getIndividualUuid() {
            return individualUuid;
        }

        public void setIndividualUuid(String individualUuid) {
            this.individualUuid = individualUuid;
        }

        public String getVisitUuid() {
            return visitUuid;
        }

        public void setVisitUuid(String visitUuid) {
            this.visitUuid = visitUuid;
        }

        public String getEntityExtId() {
            return entityExtId;
        }

        public void setEntityExtId(String entityExtId) {
            this.entityExtId = entityExtId;
        }

        public Calendar getCollectionDateTime() {
            return collectionDateTime;
        }

        public void setCollectionDateTime(Calendar collectionDateTime) {
            this.collectionDateTime = collectionDateTime;
        }

        public String getEntityUuid() {
            return entityUuid;
        }

        public void setEntityUuid(String entityUuid) {
            this.entityUuid = entityUuid;
        }

        public String getFieldWorkerUuid() {
            return fieldWorkerUuid;
        }

        public void setFieldWorkerUuid(String fieldWorkerUuid) {
            this.fieldWorkerUuid = fieldWorkerUuid;
        }

        public static long getSerialVersionUID() {
            return serialVersionUID;
        }

        public boolean isProcessedByMirth() {
            return processedByMirth;
        }

        public void setProcessedByMirth(boolean processedByMirth) {
            this.processedByMirth = processedByMirth;
        }

        public String getFieldWorkerExtId() {
            return fieldWorkerExtId;
        }

        public void setFieldWorkerExtId(String fieldWorkerExtId) {
            this.fieldWorkerExtId = fieldWorkerExtId;
        }

        public String getIndividualExtId() {
            return individualExtId;
        }

        public void setIndividualExtId(String individualExtId) {
            this.individualExtId = individualExtId;
        }

        public String getVisitExtId() {
            return visitExtId;
        }

        public void setVisitExtId(String visitExtId) {
            this.visitExtId = visitExtId;
        }

        public Calendar getExpectedDeliveryDate() {
            return expectedDeliveryDate;
        }

        public void setExpectedDeliveryDate(Calendar expectedDeliveryDate) {
            this.expectedDeliveryDate = expectedDeliveryDate;
        }

        public Calendar getRecordedDate() {
            return recordedDate;
        }

        public void setRecordedDate(Calendar recordedDate) {
            this.recordedDate = recordedDate;
        }
    }

}