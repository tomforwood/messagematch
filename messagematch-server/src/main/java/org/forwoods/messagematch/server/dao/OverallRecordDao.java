package org.forwoods.messagematch.server.dao;

import org.forwoods.messagematch.server.model.compatibility.OverallTestRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OverallRecordDao extends CrudRepository<OverallTestRecord, Long> {

}
