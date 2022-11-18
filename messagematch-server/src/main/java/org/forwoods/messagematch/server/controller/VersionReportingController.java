package org.forwoods.messagematch.server.controller;

import org.forwoods.messagematch.server.model.deployed.VersionsDeployedReport;
import org.forwoods.messagematch.server.service.VersionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/version-report")
public class VersionReportingController {

    @Autowired
    VersionsService versionsService;

    @RequestMapping(value = "/submit", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public void submitVersionReport(@RequestBody VersionsDeployedReport versionsReport) {
        versionsService.saveVersionsReport(versionsReport);
    }
}
