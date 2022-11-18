package org.forwoods.messagematch.server.model.project;

import lombok.Getter;
import lombok.Setter;
import org.forwoods.messagematch.server.model.Artifact;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "MonitoredProject", uniqueConstraints = {
        @UniqueConstraint(name = "uc_monitoredproject", columnNames = {"artifact_id"})
})
@Getter
@Setter
public class MonitoredProject {
    @Id
    long id;

    @OneToOne
    Artifact artifact;
    String uri;
    @ElementCollection
    Map<String, String> environmentBranches=  new HashMap<>();
}
