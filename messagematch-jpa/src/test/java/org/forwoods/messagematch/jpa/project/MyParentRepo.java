package org.forwoods.messagematch.jpa.project;

import org.springframework.data.repository.CrudRepository;

public interface MyParentRepo extends CrudRepository <MyParentEntity, Long> {
}
