package com.github.cimsbioko.server.webapi;

import com.github.cimsbioko.server.domain.model.*;
import com.github.cimsbioko.server.domain.util.CalendarAdapter;
import com.github.cimsbioko.server.domain.util.UUIDGenerator;
import com.github.cimsbioko.server.errorhandling.constants.ErrorConstants;
import com.github.cimsbioko.server.errorhandling.util.ErrorLogUtil;
import com.github.cimsbioko.server.controller.exception.ConstraintViolations;
import com.github.cimsbioko.server.controller.service.PregnancyService;
import com.github.cimsbioko.server.controller.service.VisitService;
import com.github.cimsbioko.server.controller.service.refactor.FieldWorkerService;
import com.github.cimsbioko.server.controller.service.refactor.IndividualService;
import com.github.cimsbioko.server.controller.service.refactor.SocialGroupService;
import com.github.cimsbioko.server.domain.annotations.Description;
import com.github.cimsbioko.server.domain.service.SitePropertiesService;
import com.github.cimsbioko.server.errorhandling.service.ErrorHandlingService;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.github.cimsbioko.server.webapi.PregnancyOutcomeFormResource.PREGNANCY_OUTCOME_PATH;


@Controller
@RequestMapping(PREGNANCY_OUTCOME_PATH)
public class PregnancyOutcomeFormResource extends AbstractFormResource {

    public static final String PREGNANCY_OUTCOME_PATH = "/rest/pregnancyOutcomeForm";
    public static final String CORE_PATH = "/core";
    public static final String OUTCOMES_PATH = "/outcomes";
    public static final String CORE_FORM_PATH = PREGNANCY_OUTCOME_PATH + CORE_PATH;
    public static final String OUTCOMES_FORM_PATH = PREGNANCY_OUTCOME_PATH + OUTCOMES_PATH;

    private static final String START_TYPE = "PregnancyOutcomeForm";

    @Autowired
    private PregnancyService pregnancyService;

    @Autowired
    private SocialGroupService socialGroupService;

    @Autowired
    private IndividualService individualService;

    @Autowired
    private FieldWorkerService fieldWorkerService;

    @Autowired
    private VisitService visitService;

    @Autowired
    private ErrorHandlingService errorService;

    @Autowired
    private CalendarAdapter adapter;

    @Autowired
    private SitePropertiesService siteProperties;

    @RequestMapping(value = CORE_PATH, method = RequestMethod.POST, produces = "application/xml", consumes = "application/xml")
    @Transactional
    public ResponseEntity<? extends Serializable> processCoreForm(@RequestBody CoreForm coreForm) throws JAXBException {

        Marshaller marshaller;
        try {
            JAXBContext context = JAXBContext.newInstance(CoreForm.class);
            marshaller = context.createMarshaller();
            marshaller.setAdapter(adapter);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create JAXB context and marshaller for PregnancyOutcomeFormResource");
        }

        List<String> logMessage = new ArrayList<>();

        PregnancyOutcome pregnancyOutcome = new PregnancyOutcome();
        try {
            fillInCoreFields(coreForm, pregnancyOutcome);
            pregnancyService.createPregnancyOutcome(pregnancyOutcome);
        } catch (ConstraintViolations constraintViolations) {
            logMessage.add(constraintViolations.getMessage());
            String errorDataPayload = createDTOPayload(coreForm, marshaller);
            ErrorLog error = ErrorLogUtil.generateErrorLog(ErrorConstants.UNASSIGNED, errorDataPayload, null,
                    CoreForm.LOG_NAME, pregnancyOutcome.getCollectedBy(),
                    ConstraintViolations.INVALID_PREGNANCY_OUTCOME_CORE, logMessage);
            errorService.logError(error);
            return requestError(constraintViolations.getMessage());
        }

        return new ResponseEntity<>(coreForm, HttpStatus.CREATED);
    }

    @RequestMapping(value = OUTCOMES_PATH, method = RequestMethod.POST, produces = "application/xml", consumes = "application/xml")
    @Transactional
    public ResponseEntity<? extends Serializable> processOutcomesForm(@RequestBody OutcomesForm outcomesForm) throws JAXBException {

        Marshaller marshaller;
        try {
            JAXBContext context = JAXBContext.newInstance(OutcomesForm.class);
            marshaller = context.createMarshaller();
            marshaller.setAdapter(adapter);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create JAXB context and marshaller for PregnancyOutcomeFormResource");
        }

        List<String> logMessage = new ArrayList<>();

        try {
            PregnancyOutcome updatedOutcome = fillInOutcomesFields(outcomesForm);
            pregnancyService.createPregnancyOutcome(updatedOutcome);
        } catch (ConstraintViolations cv) {
            logMessage.add(cv.getMessage());
            String errorDataPayload = createDTOPayload(outcomesForm, marshaller);
            ErrorLog error = ErrorLogUtil.generateErrorLog(ErrorConstants.UNASSIGNED, errorDataPayload, null,
                    OutcomesForm.LOG_NAME, null,
                    ConstraintViolations.INVALID_PREGNANCY_OUTCOME_CHILD, logMessage);
            errorService.logError(error);
            return requestError(cv.getMessage());
        }

        return new ResponseEntity<>(outcomesForm, HttpStatus.CREATED);
    }

    private PregnancyOutcome fillInOutcomesFields(OutcomesForm outcomesForm) throws ConstraintViolations {

        PregnancyOutcome parentOutcome = pregnancyService.getPregnancyOutcomeByUuid(outcomesForm.getPregnancyOutcomeUuid());
        if (null == parentOutcome) {
            throw new ConstraintViolations("Could not find parent PregnancyOutcome with UUID:" + outcomesForm.getPregnancyOutcomeUuid());
        }

        List<Outcome> existingOutcomes = parentOutcome.getOutcomes();
        if (null == existingOutcomes) {
            existingOutcomes = new ArrayList<>();
        }

        Outcome outcome = new Outcome();
        outcome.setType(outcomesForm.getOutcomeType());
        outcome.setUuid(UUIDGenerator.generate());

        if (outcome.getType().equalsIgnoreCase(siteProperties.getLiveBirthCode())) {
            Individual child = new Individual();
            child.setUuid(outcomesForm.getChildUuid());

            // generate an extId for this child
            child.setExtId(individualService.generateChildExtId(parentOutcome.getMother()));

            child.setCollectedBy(parentOutcome.getCollectedBy());
            child.setFirstName(outcomesForm.getChildFirstName());
            child.setMiddleName(outcomesForm.getChildMiddleName());
            child.setLastName(outcomesForm.getChildLastName());
            child.setGender(outcomesForm.getChildGender());
            child.setNationality(outcomesForm.getChildNationality());

            SocialGroup socialGroup = socialGroupService.getByUuid(outcomesForm.getSocialGroupUuid());
            if (null == socialGroup) {
                throw new ConstraintViolations("Could not find Social Group with UUID: " + outcomesForm.getSocialGroupUuid());
            }

            child.setMother(parentOutcome.getMother());
            child.setFather(parentOutcome.getFather());

            //Instantiate Relationship
            establishRelationship(child, outcomesForm, socialGroup);

            //Instantiate Membership: Delegate to the service entirely?
            Membership m = establishMembership(child, outcomesForm, socialGroup);
            child.getAllMemberships().add(m);
            outcome.setChild(child);
            outcome.setChildMembership(m);
        }
        existingOutcomes.add(outcome);
        return parentOutcome;

    }

    private Membership establishMembership(Individual child, OutcomesForm form, SocialGroup socialGroup) {
        Membership mem = new Membership();
        mem.setUuid(UUIDGenerator.generate());
        mem.setIndividual(child);
        mem.setInsertDate(form.getCollectionDateTime());
        mem.setbIsToA(form.getChildRelationshipToGroupHead());
        mem.setStartDate(form.getCollectionDateTime());
        mem.setCollectedBy(child.getCollectedBy());
        mem.setSocialGroup(socialGroup);
        mem.setStartType(START_TYPE);
        mem.setEndType(NOT_APPLICABLE_END_TYPE);
        return mem;
    }

    private void establishRelationship(Individual child, OutcomesForm form, SocialGroup socialGroup) {
        Relationship rel = new Relationship();
        rel.setUuid(UUIDGenerator.generate());
        rel.setInsertDate(form.getCollectionDateTime());
        rel.setIndividualA(child);
        rel.setIndividualB(socialGroup.getGroupHead());
        rel.setaIsToB(form.getChildRelationshipToGroupHead());
        rel.setCollectedBy(child.getCollectedBy());
        rel.setInsertDate(form.getCollectionDateTime());
        rel.setStartDate(form.getCollectionDateTime());
        child.getAllRelationships1().add(rel);
    }

    private void fillInCoreFields(CoreForm coreForm, PregnancyOutcome pregnancyOutcome) throws ConstraintViolations {

        pregnancyOutcome.setUuid(coreForm.getPregnancyOutcomeUuid());

        pregnancyOutcome.setOutcomeDate(coreForm.getDeliveryDate());

        FieldWorker fieldWorker = fieldWorkerService.getByUuid(coreForm.getFieldWorkerUuid());
        if (null == fieldWorker) {
            throw new ConstraintViolations("Could not find fieldworker with UUID: " + coreForm.getFieldWorkerUuid());
        }
        pregnancyOutcome.setCollectedBy(fieldWorker);

        Visit visit = visitService.findVisitByUuid(coreForm.getVisitUuid());
        if (null == visit) {
            throw new ConstraintViolations("Could not find visit with UUID: " + coreForm.getVisitUuid());
        }
        pregnancyOutcome.setVisit(visit);

        Individual father = individualService.getByUuid(coreForm.getFatherUuid());
        if (null == father) {
            father = individualService.getUnknownIndividual();
        }
        pregnancyOutcome.setFather(father);


        Individual mother = individualService.getByUuid(coreForm.getMotherUuid());
        if (null == mother) {
            throw new ConstraintViolations("Could not find mother with UUID: " + coreForm.getMotherUuid());
        }
        pregnancyOutcome.setMother(mother);
    }

    private String createDTOPayload(OutcomesForm form, Marshaller marshaller) throws JAXBException {
        StringWriter writer = new StringWriter();
        marshaller.marshal(form, writer);
        return writer.toString();
    }

    private String createDTOPayload(CoreForm coreForm, Marshaller marshaller) throws JAXBException {
        StringWriter writer = new StringWriter();
        marshaller.marshal(coreForm, writer);
        return writer.toString();
    }


    @Description(description = "Model data from the PregnancyOutcome form for the Bioko project. Additional Outcome data is contained in OutcomesForm")
    @XmlRootElement(name = "pregnancyOutcomeCoreForm")
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = CoreForm.LOG_NAME)
    public static class CoreForm implements Serializable {

        public static final String LOG_NAME = "PregnancyOutcomeCoreForm";

        //core form fields
        @XmlElement(name = "pregnancy_outcome_uuid")
        private String pregnancyOutcomeUuid;

        @XmlElement(name = "field_worker_uuid")
        private String fieldWorkerUuid;

        @XmlElement(name = "visit_uuid")
        private String visitUuid;

        @XmlElement(name = "mother_uuid")
        private String motherUuid;

        @XmlElement(name = "father_uuid")
        private String fatherUuid;

        @XmlElement(name = "delivery_date")
        @XmlJavaTypeAdapter(CalendarAdapter.class)
        private Calendar deliveryDate;

        public String getPregnancyOutcomeUuid() {
            return pregnancyOutcomeUuid;
        }

        public String getFieldWorkerUuid() {
            return fieldWorkerUuid;
        }

        public String getVisitUuid() {
            return visitUuid;
        }

        public String getMotherUuid() {
            return motherUuid;
        }

        public String getFatherUuid() {
            return fatherUuid;
        }

        public Calendar getDeliveryDate() {
            return deliveryDate;
        }

    }

    @XmlRootElement(name = "pregnancyOutcomeOutcomesForm")
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = OutcomesForm.LOG_NAME)
    public static class OutcomesForm implements Serializable {

        public static final String LOG_NAME = "PregnancyOutcomeOutcomesForm";

        @XmlElement(name = "pregnancy_outcome_uuid")
        private String pregnancyOutcomeUuid;

        @XmlElement(name = "collection_date_time")
        @XmlJavaTypeAdapter(CalendarAdapter.class)
        private Calendar collectionDateTime;

        @XmlElement(name = "socialgroup_uuid")
        private String socialGroupUuid;

        @XmlElement(name = "outcome_type")
        private String outcomeType;

        @XmlElement(name = "child_uuid")
        private String childUuid;

        @XmlElement(name = "child_first_name")
        private String childFirstName;

        @XmlElement(name = "child_middle_name")
        private String childMiddleName;

        @XmlElement(name = "child_last_name")
        private String childLastName;

        @XmlElement(name = "child_gender")
        private String childGender;

        @XmlElement(name = "child_relationship_to_group_head")
        private String childRelationshipToGroupHead;

        @XmlElement(name = "child_nationality")
        private String childNationality;

        public String getSocialGroupUuid() {
            return socialGroupUuid;
        }

        public String getPregnancyOutcomeUuid() {
            return pregnancyOutcomeUuid;
        }

        public Calendar getCollectionDateTime() {
            return collectionDateTime;
        }

        public String getOutcomeType() {
            return outcomeType;
        }

        public String getChildUuid() {
            return childUuid;
        }

        public String getChildFirstName() {
            return childFirstName;
        }

        public String getChildMiddleName() {
            return childMiddleName;
        }

        public String getChildLastName() {
            return childLastName;
        }

        public String getChildGender() {
            return childGender;
        }

        public String getChildRelationshipToGroupHead() {
            return childRelationshipToGroupHead;
        }

        public String getChildNationality() {
            return childNationality;
        }

    }
}