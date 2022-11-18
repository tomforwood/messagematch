package org.forwoods.messagematch.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.forwoods.messagematch.server.model.compatibility.VersionCompatibilityReport;
import org.forwoods.messagematch.server.service.VersionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/compatibility-report")
@Slf4j
public class CompatibilityReportController {

    @Autowired
    VersionsService versionsService;

    @GetMapping(value = "/get", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public HttpEntity<VersionCompatibilityReport> submitVersionReport() {
        return new HttpEntity<>(versionsService.getVersionCompatibilities());
    }

    @PostMapping(value="doTestBuild")
    public void doTestBuild(@RequestBody VersionedArtifact artifact){
        log.info("doing a test build for {}", artifact);
    }
}
