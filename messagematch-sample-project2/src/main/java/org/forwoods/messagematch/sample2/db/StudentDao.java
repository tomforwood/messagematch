package org.forwoods.messagematch.sample2.db;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.forwoods.messagematch.sample2.api.StudentDetails;

import java.util.function.Supplier;

public class StudentDao {

    private final Supplier<MongoCollection<StudentDetails>> mongo;

    public StudentDao(Supplier<MongoCollection<StudentDetails>> mongo) {
        this.mongo = mongo;
    }

    public StudentDetails getStudentDetails(int userId) {
        FindIterable<StudentDetails> template = mongo.get().find(new Document("id", userId));
        return template.first();
    }
}
