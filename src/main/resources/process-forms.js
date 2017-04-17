/**
 * The core form processing logic. The following script describes how submitted
 * forms will be processed by the server.
 */

var imports = new JavaImporter(
    java.io,
    javax.xml.transform.stream,
    com.github.cimsbioko.server.controller.service,
    com.github.cimsbioko.server.webapi
);

with (imports) {

    var appCtx, jaxb, formService;

    /**
     * Convenience function for looking up application objects.
     */
    function getBean(o) {
        return appCtx.getBean(o);
    }

    /**
     * Called by the server upon initialization.
     */
    function setApplicationContext(ctx) {
        appCtx = ctx;
        formService = getBean(FormSubmissionService.class);
        jaxb = getBean('jaxbMarshaller');
    }

    /**
     * Metadata for adapting raw odk form submissions to existing form endpoints.
     */
    var bindings = {
        spraying: {
            endpoint: SprayingFormResource.class,
            mapData: function(data) {
                return {
                    sprayingForm: {
                        entity_uuid: data.entityUuid,
                        evaluation: data.evaluation
                    }
                };
            }
        },
        location: {
            endpoint: LocationFormResource.class,
            mapData: function(data) {
                var result = {
                    locationForm: {
                        entity_uuid: data.entityUuid,
                        entity_ext_id: data.entityExtId,
                        field_worker_uuid: data.fieldWorkerUuid,
                        field_worker_ext_id: data.fieldWorkerExtId,
                        collection_date_time: data.collectionDateTime,
                        hierarchy_ext_id: data.hierarchyExtId,
                        hierarchy_uuid: data.hierarchyUuid,
                        hierarchy_parent_uuid: data.hierarchyParentUuid,
                        location_ext_id: data.locationExtId,
                        location_name: data.locationName,
                        location_type: data.locationType,
                        community_name: data.communityName,
                        community_code: data.communityCode,
                        map_area_name: data.mapAreaName,
                        locality_name: data.localityName,
                        sector_name: data.sectorName,
                        location_building_number: data.locationBuildingNumber,
                        location_floor_number: data.locationFloorNumber,
                        description: data.description
                    }
                };
                if (data.location) {
                    var form = result.locationForm, gps = toGPS(data.location);
                    form.latitude = gps.latitude;
                    form.longitude = gps.longitude;
                }
                return result;
            }
        },
        duplicate_location: {
            endpoint: DuplicateLocationFormResource.class,
            mapData: function(data) {
                var result = {
                    duplicateLocationForm: {
                        entity_uuid: data.entityUuid,
                        action: data.action,
                        description: data.description
                    }
                };
                if (data.globalPosition) {
                    var form = result.duplicateLocationForm, gps = toGPS(data.globalPosition);
                    form.global_position_lat = gps.latitude;
                    form.global_position_lng = gps.longitude;
                    form.global_position_acc = gps.accuracy;
                }
                return result;
            }
        }
    };

    /**
     * This processing entry point is called periodically by the server (every 30s).
     */
    function processForms() {
        var batchSize = 300, forms = formService.getUnprocessed(batchSize), processed = 0, failures = 0;
        for (var f = 0; f < forms.length; f++) {
            var submission = forms[f];
            try {
                process(submission);
            } catch (error) {
                error.printStackTrace();
                failures += 1;
            }
            formService.markProcessed(submission);
            processed += 1;
        }
        print('processing completed with ' + failures + ' failures');
        return processed;
    }

    /**
     * Processes a single form submission.
     */
    function process(submission) {
        var form = JSON.parse(submission.json),
            data = form.data, meta = data.meta,
            bindingName = submission.getFormBinding(), binding = bindings[bindingName];
        if (binding) {
            var formXml = toXml(binding.mapData(data)), formObj = toForm(formXml);
            print('processing form ' + meta.instanceID + ' with binding ' + bindingName);
            getBean(binding.endpoint).processForm(formObj);
        }
    }

    /**
     * Converts an xml document to an endpoint form using jaxb.
     */
    function toForm(xml) {
        var reader = new StringReader(xml), source = new StreamSource(reader);
        return jaxb.unmarshal(source);
    }

    /**
     * Converts a javascript object to an xml string.
     */
    function toXml(data) {
        var result = '';
        for (var field in data) {
            var value = data[field];
            if (typeof(value) !== 'undefined') {
                result += '<' + field + '>';
                if (typeof(value) === 'object') {
                    result += toXml(value);
                } else {
                    result += escapeXml('' + value);
                }
                result += '</' + field + '>';
            }
        }
        return result;
    }

    /**
     * Escapes a string value so it can be safely inserted into an XML document.
     */
    function escapeXml(unsafe) {
        return unsafe.replace(/[<>&'"]/g, function (c) {
            switch (c) {
                case '<': return '&lt;';
                case '>': return '&gt;';
                case '&': return '&amp;';
                case '\'': return '&apos;';
                case '"': return '&quot;';
            }
        });
    }

    /**
     * Converts ODK gps string values into objects.
     */
    function toGPS(gpsString) {
        var gps = gpsString.split(' ');
        return {
            latitude: gps[0],
            longitude: gps[1],
            altitude: gps[2],
            accuracy: gps[3]
        };
    }
}