package com.github.cimsbioko.server.web;

import com.github.cimsbioko.server.service.SyncService;
import com.github.cimsbioko.server.service.impl.MobileDbGeneratorEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.core.annotation.Order;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Controller
public class SyncController {

    private SyncService service;
    private SimpMessagingTemplate messageTemplate;

    public SyncController(SyncService service, SimpMessagingTemplate msgTemplate) {
        this.service = service;
        this.messageTemplate = msgTemplate;
    }

    @PreAuthorize("hasAuthority('VIEW_SYNC')")
    @GetMapping("/sync")
    @ResponseBody
    public Map<String, Object> syncStatus() {
        return getSyncStatus();
    }

    @EventListener
    @Order
    @Transactional(propagation = REQUIRES_NEW)
    public void status(MobileDbGeneratorEvent event) {
        messageTemplate.convertAndSend("/topic/syncstatus", getSyncStatus());
    }

    private Map<String, Object> getSyncStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("nextRun", service.getMinutesToNextRun().orElse(null));
        result.put("task", service.getTask());
        result.put("scheduled", service.isTaskScheduled());
        result.put("schedule", service.getSchedule().map(String::trim).orElse(""));
        result.put("running", service.isTaskRunning());
        result.put("downloadable", service.getOutput().canRead());
        return result;
    }

    @PreAuthorize("hasAuthority('MANAGE_SYNC')")
    @GetMapping("/sync/pause")
    @ResponseBody
    public Map<String, Object> pause() {
        service.cancelTask();
        return syncStatus();
    }

    @PreAuthorize("hasAuthority('MANAGE_SYNC')")
    @GetMapping("/sync/start")
    @ResponseBody
    public Map<String, Object> start() {
        service.resumeSchedule();
        return syncStatus();
    }

    @PreAuthorize("hasAuthority('MANAGE_SYNC')")
    @GetMapping("/sync/run")
    @ResponseBody
    public Map<String, Object> run() {
        service.requestTaskRun();
        return syncStatus();
    }

    @PreAuthorize("hasAuthority('EXPORT_SYNC')")
    @GetMapping("/sync/export")
    public void downloadDb(HttpServletResponse response) throws IOException {

        String filename = "openhds.db";

        FileSystemResource dbFileRes = new FileSystemResource(service.getOutput());

        if (!dbFileRes.isReadable()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Unable to find mobiledb file. Try generating it from the tasks menu.");
        } else {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + ".zip\"");
            try (ZipOutputStream zOut = new ZipOutputStream(response.getOutputStream())) {
                ZipEntry e = new ZipEntry(filename);
                e.setSize(dbFileRes.contentLength());
                e.setTime(System.currentTimeMillis());
                zOut.putNextEntry(e);
                StreamUtils.copy(dbFileRes.getInputStream(), zOut);
                zOut.closeEntry();
                zOut.finish();
            }
        }
    }
}