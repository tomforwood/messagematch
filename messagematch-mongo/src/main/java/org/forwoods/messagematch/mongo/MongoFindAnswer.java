package org.forwoods.messagematch.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.NonNull;
import org.bson.Document;
import org.mockito.internal.stubbing.defaultanswers.ReturnsSmartNulls;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public abstract class MongoFindAnswer<T> implements Answer<FindIterable<T>> {

    private final ObjectMapper mapper = new ObjectMapper();
    protected final JsonNode responseMessage;

    public MongoFindAnswer(JsonNode responseMessage) {
        this.responseMessage = responseMessage;
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

    private static  class ListCursor<T> implements MongoCursor<T> {
        private final Iterator<T> list;

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
        @NonNull
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
        @NonNull
        public ServerAddress getServerAddress() {
            return new ServerAddress((String) null);
        }
    }

    protected FindIterable<T> getFindIterable(Class<T> returnClass) throws JsonProcessingException {
        List<T> list;
        if (returnClass.equals(Document.class)) {
            ArrayNode arr = (ArrayNode) responseMessage;
            list = new ArrayList<>();

            arr.forEach(d -> { @SuppressWarnings("unchecked")
                    T val = (T)Document.parse(d.toString());
                    list.add(val);
            });
        } else {
            JavaType t = TypeFactory.defaultInstance().constructCollectionLikeType(List.class, returnClass);
            list = mapper.treeToValue(responseMessage, t);
        }
        @SuppressWarnings("unchecked")
        FindIterable<T> it = (FindIterable<T>)mock(FindIterable.class, new DefaultingAnswer());
        MongoCursor<T> cursor = new ListCursor<>(list);
        lenient().when(it.iterator()).thenReturn(cursor);
        lenient().when(it.projection(any())).thenReturn(it);
        lenient().when(it.first()).thenAnswer(i->cursor.tryNext());
        return it;
    }
}
