package org.forwoods.messagematch.springjdbc;

import org.forwoods.messagematch.junit.BehaviourVerificationException;
import org.forwoods.messagematch.junit.MessageSpec;
import org.forwoods.messagematch.junit.MessageSpecExtension;
import org.forwoods.messagematch.spec.TestSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MessageSpecExtension.class)
@SuppressWarnings( {"deprecation", "ConstantConditions"} )
public class SpringJdbcTemplateBehaviourBuilderTest {

    SpringJdbcTemplateBehaviourBuilder templateBehaviourBuilder = new SpringJdbcTemplateBehaviourBuilder();
    JdbcTemplate template = templateBehaviourBuilder.getTemplate();

    @Test
    void queryForObject(@MessageSpec("src/test/resources/objectQuery") TestSpec spec) {
        templateBehaviourBuilder.addBehavior(spec.getSideEffects());

        Pojo p = template.queryForObject("Select * from pojos where key = ?", new Object[]{5}, (RowMapper<Pojo>)null);
        assertEquals("hello world", p.stringVal);

        p = template.queryForObject("Select * from pojos where key = ?", (RowMapper<Pojo>)null, 5);
        assertEquals("hello world", p.stringVal);

    }

    @Test
    void verificationFailure(@MessageSpec("src/test/resources/objectQuery") TestSpec spec) {
        templateBehaviourBuilder.addBehavior(spec.getSideEffects());

        //we don't make the expected database calls - the verification will fail
        BehaviourVerificationException e = assertThrows(BehaviourVerificationException.class, ()->templateBehaviourBuilder.verifyBehaviour(spec.getSideEffects()));
        assertTrue(e.getMessage().contains("Expected at least 1 calls"));

    }

    @Test
    void queryForList(@MessageSpec("src/test/resources/listQuery") TestSpec spec) {
        templateBehaviourBuilder.addBehavior(spec.getSideEffects());

        List<Pojo> p = template.query("Select * from pojos where key >= ? and key <= ?", new Object[]{5, 6}, (RowMapper<Pojo>)null);
        assertEquals("hello world", p.get(0).stringVal);
        assertEquals(2, p.size());

        p = template.query("Select * from pojos where key >= ? and key <= ?", (RowMapper<Pojo>)null, 5, 6);
        assertEquals("hello world", p.get(0).stringVal);
        assertEquals(2, p.size());

    }

}