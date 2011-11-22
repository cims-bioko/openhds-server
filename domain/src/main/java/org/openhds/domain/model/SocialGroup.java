package org.openhds.domain.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.validation.constraints.Past;
import org.openhds.domain.annotations.Description;
import org.openhds.domain.constraint.CheckEntityNotVoided;
import org.openhds.domain.constraint.CheckFieldNotBlank;
import org.openhds.domain.constraint.CheckHouseholdHeadAge;
import org.openhds.domain.constraint.CheckIndividualNotUnknown;
import org.openhds.domain.constraint.Searchable;
import org.openhds.domain.value.extension.ExtensionConstraint;

@Description(description="A Social Group represents a distinct family within the " +
		"study area. Social Groups are identified by a uniquely generated identifier " +
		"which the system uses internally. A Social Group has one head of house which " +
		"all Membership relationships are based on.")
@Entity
@Table(name="socialgroup")
public class SocialGroup extends AuditableCollectedEntity implements Serializable {
   
	private static final long serialVersionUID = -5592935530217622317L;
    
    @Column(name="extId", unique = true)
    @Searchable
    @Description(description="External Id of the social group. This id is used internally.")
    String extId;
    
    @CheckFieldNotBlank
    @Searchable
    @Description(description="Name of the social group.")
    String groupName;
    
    @Searchable
    @CheckEntityNotVoided
    @CheckIndividualNotUnknown
    @CheckHouseholdHeadAge(allowNull = true, message="The social group head is younger than the minimum age required in order to be a household head")
    @ManyToOne(cascade = {CascadeType.ALL})
    @Description(description="Individual who is head of the social group, identified by external id.")
    Individual groupHead;
    
    @Searchable
    @CheckEntityNotVoided
    @CheckIndividualNotUnknown
    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, targetEntity = Individual.class)
    @Description(description="Individual who supplied the information for this social group.")
    Individual respondent;
    
    @ExtensionConstraint(constraint="socialGroupTypeConstraint", message="Invalid Value for social group type",allowNull=true)
    @Description(description="Type of the social group.")
    String groupType;
    
	@Past
    @Temporal(javax.persistence.TemporalType.DATE)
    @Description(description="Date of interview.")
    Calendar dateOfInterview;
    
    @OneToMany(cascade={CascadeType.ALL}, mappedBy="socialGroup" )
    @Description(description="Set of all memberships of the social group.")
    Set<Membership> memberships;

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String type) {
        this.groupType = type;
    }
    
    //@XmlJavaTypeAdapter(value=CalendarAdapter.class) 
    public Calendar getDateOfInterview() {
		return dateOfInterview;
	}

	public void setDateOfInterview(Calendar dateOfInterview) {
		this.dateOfInterview = dateOfInterview;
	}

    public Set<Membership> getMemberships() {
        return memberships;
    }

    public void setMemberships(Set<Membership> memberships) {
        this.memberships = memberships;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String name) {
        this.groupName = name;
    }

    public Individual getGroupHead() {
        return groupHead;
    }

    public void setGroupHead(Individual head) {
        this.groupHead = head;
    }
    
    public Individual getRespondent() {
		return respondent;
	}

	public void setRespondent(Individual respondent) {
		this.respondent = respondent;
	}
}
