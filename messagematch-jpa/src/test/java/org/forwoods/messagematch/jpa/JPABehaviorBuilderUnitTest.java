package org.forwoods.messagematch.jpa;

import org.forwoods.messagematch.jpa.project.MyEntity;
import org.forwoods.messagematch.jpa.project.MyParentRepo;
import org.forwoods.messagematch.jpa.project.MyRepo;
import org.forwoods.messagematch.junit.BehaviourVerificationException;
import org.forwoods.messagematch.junit.MessageSpec;
import org.forwoods.messagematch.junit.MessageSpecExtension;
import org.forwoods.messagematch.spec.TestSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MessageSpecExtension.class)
@ExtendWith(MockitoExtension.class)
public class JPABehaviorBuilderUnitTest {

    @Mock
    MyRepo myRepo;
    @Mock
    MyParentRepo myParentRepo;

    @Test
    void readSucceeds(@MessageSpec("src/test/resources/database-passes") TestSpec spec) {
        JPABehaviorBuilder jpaBehaviorBuilder = new JPABehaviorBuilder(List.of(myRepo, myParentRepo));
        jpaBehaviorBuilder.addBehavior(spec.getSideEffects());

        verify(myRepo).save(any(MyEntity.class));
    }

    @Test
    void writeSucceeds(@MessageSpec("src/test/resources/database-write") TestSpec spec) {
        JPABehaviorBuilder jpaBehaviorBuilder = new JPABehaviorBuilder(List.of(myRepo, myParentRepo));
        jpaBehaviorBuilder.addBehavior(spec.getSideEffects());

        MyEntity myEntity = new MyEntity();
        myEntity.setValue("test");
        myEntity.setValueInt(1);
        when(myRepo.findAll()).thenReturn(List.of(myEntity));

        jpaBehaviorBuilder.verifyBehaviour(spec.getSideEffects());
    }
    @Test
    void writeFailsNoWrite(@MessageSpec("src/test/resources/database-write") TestSpec spec) {
        JPABehaviorBuilder jpaBehaviorBuilder = new JPABehaviorBuilder(List.of(myRepo, myParentRepo));
        jpaBehaviorBuilder.addBehavior(spec.getSideEffects());

        BehaviourVerificationException ex = assertThrows(BehaviourVerificationException.class, () -> jpaBehaviorBuilder.verifyBehaviour(spec.getSideEffects()));
        assertEquals("For persister interface org.forwoods.messagematch.jpa.project.MyRepo Expected values matching [{\"value\":\"test\",\"valueInt\":1}]\nbut found values []", ex.getMessage());
    }
    @Test
    void writeFailsNoMatch(@MessageSpec("src/test/resources/database-write") TestSpec spec) {
        JPABehaviorBuilder jpaBehaviorBuilder = new JPABehaviorBuilder(List.of(myRepo, myParentRepo));
        jpaBehaviorBuilder.addBehavior(spec.getSideEffects());

        MyEntity myEntity = new MyEntity();
        myEntity.setValue("blah");
        when(myRepo.findAll()).thenReturn(List.of(myEntity));

        BehaviourVerificationException ex = assertThrows(BehaviourVerificationException.class, () -> jpaBehaviorBuilder.verifyBehaviour(spec.getSideEffects()));
        assertEquals("Error at root:value expected matching test but was blah\n" +
                "Error at root:valueInt expected matching 1 but was 0", ex.getMessage());
    }
}