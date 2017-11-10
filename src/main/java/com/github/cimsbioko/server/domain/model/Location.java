
package com.github.cimsbioko.server.domain.model;

import com.github.cimsbioko.server.domain.annotations.Description;
import com.github.cimsbioko.server.domain.constraint.CheckFieldNotBlank;
import com.github.cimsbioko.server.domain.constraint.Searchable;
import com.vividsolutions.jts.geom.Point;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import com.github.cimsbioko.server.domain.constraint.ExtensionStringConstraint;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Description(description = "All distinct Locations within the area of study are represented here. A Location is identified by a uniquely generated identifier that the system uses internally. Each Location has a name associated with it and resides at a particular hierarchy within the Location Hierarchy.")
@Entity
@Table(name = "location")
@XmlRootElement
public class Location
        extends AuditableCollectedEntity
        implements Serializable {

    public final static long serialVersionUID = 169551578162260199L;

    @NotNull
    @Size(min = 1)
    @Searchable
    @Description(description = "External Id of the location. This id is used internally.")
    private String extId;

    @CheckFieldNotBlank
    @Searchable
    @Description(description = "Name of the location.")
    private String locationName;

    @ManyToOne
    @Cascade(CascadeType.SAVE_UPDATE)
    private LocationHierarchy locationHierarchy = new LocationHierarchy();

    @ExtensionStringConstraint(constraint = "locationTypeConstraint", message = "Invalid Value for location type", allowNull = true)
    @Description(description = "The type of Location.")
    private String locationType;

    @Description(description = "the global position represented as longitude, latitude, altitude")
    @Column(name = "global_pos")
    private Point globalPos;

    @OneToMany(targetEntity = Residency.class)
    @JoinColumn(name = "location_uuid")
    private List<Residency> residencies = new ArrayList<>();

    // Extensions for bioko island project
    @Description(description = "The number of this building within a sector")
    private Integer buildingNumber;

    @Description(description = "The floor number within the building this location is associated with")
    private Integer floorNumber;

    @Description(description = "The name of the Region that contains this location")
    private String regionName;

    @Description(description = "The name of the Province that contains this location")
    private String provinceName;

    @Description(description = "The name of the Sub District that contains this location")
    private String subDistrictName;

    @Description(description = "The name of the District that contains this location")
    private String districtName;

    @Description(description = "The name of the map sector that contains this location")
    private String sectorName;

    @Description(description = "The name of the locality (aka AREA) that contains this location")
    private String localityName;

    @Description(description = "The name of the community that contains this location")
    private String communityName;

    @Description(description = "Formatted code for the community that contains this location")
    private String communityCode;

    @Description(description = "The name of the map area - disregarding Locality - that contains this location")
    private String mapAreaName;

    @Description(description = "A description of the observable features of a location")
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtId() {
        return extId;
    }

    public void setExtId(String id) {
        extId = id;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String name) {
        locationName = name;
    }

    public LocationHierarchy getLocationHierarchy() {
        return locationHierarchy;
    }

    public void setLocationHierarchy(LocationHierarchy hierarchy) {
        locationHierarchy = hierarchy;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String type) {
        locationType = type;
    }

    public Point getGlobalPos() {
        return globalPos;
    }

    public void setGlobalPos(Point globalPos) {
        this.globalPos = globalPos;
    }

    public List<Residency> getResidencies() {
        return residencies;
    }

    public void setResidencies(List<Residency> list) {
        residencies = list;
    }

    public String getSectorName() {
        return sectorName;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    public String getLocalityName() {
        return localityName;
    }

    public void setLocalityName(String localityName) {
        this.localityName = localityName;
    }

    public String getCommunityName() {
        return communityName;
    }

    public void setCommunityName(String communityName) {
        this.communityName = communityName;
    }

    public String getCommunityCode() {
        return communityCode;
    }

    public void setCommunityCode(String communityCode) {
        this.communityCode = communityCode;
    }

    public String getMapAreaName() {
        return mapAreaName;
    }

    public void setMapAreaName(String mapAreaName) {
        this.mapAreaName = mapAreaName;
    }

    public Integer getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(Integer buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getSubDistrictName() {
        return subDistrictName;
    }

    public void setSubDistrictName(String subDistrictName) {
        this.subDistrictName = subDistrictName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public static Location makeStub(String uuid, String extId) {

        Location stub = new Location();
        stub.setUuid(uuid);
        stub.setExtId(extId);
        return stub;

    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Location)) {
            return false;
        }

        final String otherUuid = ((Location) other).getUuid();
        return null != uuid && null != otherUuid && uuid.equals(otherUuid);
    }

    @XmlRootElement
    public static class Locations {

        private List<Location> locations;

        @XmlElement(name = "location")
        public List<Location> getLocations() {
            return locations;
        }

        public void setLocations(List<Location> locations) {
            this.locations = locations;
        }

    }

}
