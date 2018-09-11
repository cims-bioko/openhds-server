package com.github.cimsbioko.server.service;

import org.jdom2.JDOMException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface FormService {
    void uploadForm(MultipartFile formXml, MultipartFile xlsform, MultiValueMap<String, MultipartFile> uploadedFiles)
            throws JDOMException, IOException, NoSuchAlgorithmException;
    void uploadXlsform(MultipartFile xlsform, MultiValueMap<String,MultipartFile> multiFileMap) throws IOException, NoSuchAlgorithmException, JDOMException;
}