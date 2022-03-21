package org.forwoods.messagematch.sample.db;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.forwoods.messagematch.sample.api.GreetingTemplate;

import java.util.function.Supplier;

public class GreetingDAO {

    private final Supplier<MongoCollection<GreetingTemplate>> mongo;

    public GreetingDAO(Supplier<MongoCollection<GreetingTemplate>> mongo) {
        this.mongo = mongo;
    }

    public GreetingTemplate lookup(String language) {
        FindIterable<GreetingTemplate> template = mongo.get().find(new Document("language", language));
        return template.first();
    }

    public GreetingTemplate persistTemplate(GreetingTemplate template) {
        Bson key = Filters.eq("language", template.getLanguage());
        UpdateResult result = mongo.get().replaceOne(key, template, new ReplaceOptions().upsert(true));
        if (result.wasAcknowledged()) {
            return template;
        }
        else throw new RuntimeException("Something went wrong");
    }
}
