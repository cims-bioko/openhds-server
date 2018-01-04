package com.github.cimsbioko.server.formproc;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.sql.CallableStatement;

@Component
public class CreateSectorFormProcessor {

    @Autowired
    SessionFactory sessionFactory;

    @Transactional
    public void processForm(Form form) {
        Session session = sessionFactory.getCurrentSession();
        session.doWork(
                c -> {
                    try (CallableStatement f = c.prepareCall("{ call create_sector(?, ?) }")) {
                        f.setString(1, form.mapUuid);
                        f.setString(2, form.sectorName);
                        f.execute();
                    }
                }
        );
    }

    @XmlRootElement(name = "createSectorForm")
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = Form.LOG_NAME)
    public static class Form implements Serializable {

        public static final String LOG_NAME = "CreateSectorForm";

        @XmlElement(name = "map_uuid")
        private String mapUuid;

        @XmlElement(name = "sector_name")
        private String sectorName;
    }
}
