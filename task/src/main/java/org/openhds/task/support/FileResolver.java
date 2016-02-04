package org.openhds.task.support;

import java.io.File;

public interface FileResolver {

    File resolveIndividualXmlFile();

    File resolveLocationXmlFile();

    File resolveRelationshipXmlFile();

    File resolveSocialGroupXmlFile();

    File resolveMembershipXmlFile();

    File resolveVisitXmlFile();

    File resolveFieldWorkerFile();

    File resolveLocationHierarchyFile();

    File getFileForTask(String taskName);
}
