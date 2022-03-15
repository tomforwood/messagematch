package org.forwoods.messagematch.mongo;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.junit.MessageArgumentMatcher;
import org.forwoods.messagematch.spec.CallExample;
import org.mockito.internal.stubbing.defaultanswers.ReturnsSmartNulls;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class MongoBehaviourBuilder extends BehaviourBuilder {

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public void addBehavior(Collection<CallExample> calls) {

        MongoCollection mongo = (MongoCollection) this.mocks.entrySet().stream()
                .filter(e->MongoCollection.class.isAssignableFrom(e.getKey()))
                .map(e->e.getValue())
                .findFirst().orElseThrow(()->new RuntimeException("Mock implementing MongoCollection not found"));
        calls.stream().filter(c->c.getChannel() instanceof MongoChannel).forEach(c->{
            MongoChannel channel = (MongoChannel) c.getChannel();
            MongoMethod method = channel.getMethod();
            switch(method) {
                case FIND:
                    when(mongo.find(argThat(new MessageArgumentMatcher<Document>(c.getRequestMessage())), any(Class.class)))
                            .thenAnswer(new Answer() {

                                @Override
                                public Object answer(InvocationOnMock invocation) throws Throwable {
                                    Class returnClass = invocation.getArgument(1);
                                    List<Document> list;
                                    if (returnClass.equals(Document.class)) {
                                        ArrayNode arr = (ArrayNode) c.getResponseMessage();
                                        list = new ArrayList<>();
                                        arr.forEach(d->list.add(Document.parse(d.toString())));
                                    }
                                    else{
                                        JavaType t = TypeFactory.defaultInstance().constructCollectionLikeType(List.class, returnClass);
                                        list =  mapper.treeToValue(c.getResponseMessage(), t);
                                    }
                                    FindIterable it = mock(FindIterable.class, new DefaultingAnswer());
                                    MongoCursor cursor = new ListCursor(list);
                                    when(it.iterator()).thenReturn(cursor);
                                    when(it.projection(any())).thenReturn(it);
                                    return it;
                                }
                            });
            }
        });
    }

    private static  class ListCursor<T> implements MongoCursor<T> {
        private Iterator<T> list;

        public ListCursor(List<T> list) {
            this.list = list.listIterator();
        }

        @Override
        public void close() {
        }

        @Override
        public boolean hasNext() {
            return list.hasNext();
        }

        @Override
        public T next() {
            return list.next();
        }

        @Override
        public T tryNext() {
            if (!hasNext()) return null;
            return next();
        }

        @Override
        public ServerCursor getServerCursor() {
            return null;
        }

        @Override
        public ServerAddress getServerAddress() {
            return null;
        }
    }

    private static class DefaultingAnswer extends ReturnsSmartNulls {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            if (invocation.getMethod().getReturnType().isAssignableFrom(invocation.getMock().getClass())) {
                return invocation.getMock();
            }
            return super.answer(invocation);
        }
    }
}
