package org.forwoods.messagematch.jpa;


import org.forwoods.messagematch.jpa.project.MyEntity;
import org.forwoods.messagematch.jpa.project.MyParentEntity;
import org.forwoods.messagematch.jpa.project.MyParentRepo;
import org.forwoods.messagematch.jpa.project.MyRepo;
import org.forwoods.messagematch.junit.MessageSpec;
import org.forwoods.messagematch.junit.MessageSpecExtension;
import org.forwoods.messagematch.spec.TestSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(MessageSpecExtension.class)
@DataJpaTest
@Import(App.class)
public class JPABehaviourBuilderIntegrationTest {

    @Autowired
    MyRepo repo;
    @Autowired
    MyParentRepo parentRepo;

    @Autowired
    List<CrudRepository<?, ?>> repos;

    @Test
    void readSucceeds(@MessageSpec("src/test/resources/database-passes") TestSpec spec) {
        JPABehaviorBuilder builder = new JPABehaviorBuilder(repos);
        builder.addBehavior(spec.getSideEffects());

        Iterable<MyEntity> ents = repo.findAll();
        MyEntity e = ents.iterator().next();
        assertEquals("test", e.getValue());

        Iterable<MyParentEntity> parents = parentRepo.findAll();
        MyParentEntity p = parents.iterator().next();
        assertEquals(e, p.getMyEntity());
    }
}
