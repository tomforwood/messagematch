package org.forwoods.messagematch.server.model.compatibility;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Setter
@Getter
public class VersionCompatibilityReport {

    private List<VersionCompatibilityEnvironment> environments = new ArrayList<>();

    @Override
    public String toString() {
        return "VersionCompatibilityReport{" +
                "environments=" + environments +
                '}';
    }
}
