package org.forwoods.messagematch.jpa.project;

import org.springframework.data.repository.CrudRepository;

public interface MyRepo extends CrudRepository<MyEntity, Long> {
}
