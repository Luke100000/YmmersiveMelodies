package net.conczin.utils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings({"unused", "unchecked"})
public abstract class RecordCodec<C> implements Codec<C> {
    public static <C> RecordCodec<C> composite(
            final Functions.Function0<C> factory
    ) {
        return compositeInternal(
                args -> factory.apply()
        );
    }

    public static <C, T0> RecordCodec<C> composite(
            String nameA, Codec<T0> codecA, Function<C, T0> getterA,
            Functions.Function1<T0, C> factory
    ) {
        return composite(
                factory,
                new Field<>(nameA, codecA, getterA)
        );
    }

    public static <C, T0, T1> RecordCodec<C> composite(
            String nameA, Codec<T0> codecA, Function<C, T0> getterA,
            String nameB, Codec<T1> codecB, Function<C, T1> getterB,
            Functions.Function2<T0, T1, C> factory
    ) {
        return composite(
                factory,
                new Field<>(nameA, codecA, getterA),
                new Field<>(nameB, codecB, getterB)
        );
    }

    public static <C, T0, T1, T2> RecordCodec<C> composite(
            String nameA, Codec<T0> codecA, Function<C, T0> getterA,
            String nameB, Codec<T1> codecB, Function<C, T1> getterB,
            String nameC, Codec<T2> codecC, Function<C, T2> getterC,
            Functions.Function3<T0, T1, T2, C> factory
    ) {
        return composite(
                factory,
                new Field<>(nameA, codecA, getterA),
                new Field<>(nameB, codecB, getterB),
                new Field<>(nameC, codecC, getterC)
        );
    }

    public static <C, T0, T1, T2, T3> RecordCodec<C> composite(
            String nameA, Codec<T0> codecA, Function<C, T0> getterA,
            String nameB, Codec<T1> codecB, Function<C, T1> getterB,
            String nameC, Codec<T2> codecC, Function<C, T2> getterC,
            String nameD, Codec<T3> codecD, Function<C, T3> getterD,
            Functions.Function4<T0, T1, T2, T3, C> factory
    ) {
        return composite(
                factory,
                new Field<>(nameA, codecA, getterA),
                new Field<>(nameB, codecB, getterB),
                new Field<>(nameC, codecC, getterC),
                new Field<>(nameD, codecD, getterD)
        );
    }

    /*
    Field composites
    */

    public static <C, T0> RecordCodec<C> composite(
            Functions.Function1<T0, C> factory,
            Field<C, T0> fieldA
    ) {
        return compositeInternal(
                args -> factory.apply((T0) args[0]),
                fieldA
        );
    }

    public static <C, T0, T1> RecordCodec<C> composite(
            Functions.Function2<T0, T1, C> factory,
            Field<C, T0> fieldA, Field<C, T1> fieldB
    ) {
        return compositeInternal(
                args -> factory.apply((T0) args[0], (T1) args[1]),
                fieldA, fieldB
        );
    }

    public static <C, T0, T1, T2> RecordCodec<C> composite(
            Functions.Function3<T0, T1, T2, C> factory,
            Field<C, T0> fieldA, Field<C, T1> fieldB, Field<C, T2> fieldC
    ) {
        return compositeInternal(
                args -> factory.apply((T0) args[0], (T1) args[1], (T2) args[2]),
                fieldA, fieldB, fieldC
        );
    }

    public static <C, T0, T1, T2, T3> RecordCodec<C> composite(
            Functions.Function4<T0, T1, T2, T3, C> factory,
            Field<C, T0> fieldA, Field<C, T1> fieldB, Field<C, T2> fieldC,
            Field<C, T3> fieldD
    ) {
        return compositeInternal(
                args -> factory.apply(
                        (T0) args[0], (T1) args[1], (T2) args[2], (T3) args[3]
                ),
                fieldA, fieldB, fieldC, fieldD
        );
    }

    public static <C, T0, T1, T2, T3, T4> RecordCodec<C> composite(
            Functions.Function5<T0, T1, T2, T3, T4, C> factory,
            Field<C, T0> fieldA, Field<C, T1> fieldB, Field<C, T2> fieldC,
            Field<C, T3> fieldD, Field<C, T4> fieldE
    ) {
        return compositeInternal(
                args -> factory.apply(
                        (T0) args[0], (T1) args[1], (T2) args[2], (T3) args[3], (T4) args[4]
                ),
                fieldA, fieldB, fieldC, fieldD, fieldE
        );
    }

    public static <C, T0, T1, T2, T3, T4, T5> RecordCodec<C> composite(
            Functions.Function6<T0, T1, T2, T3, T4, T5, C> factory,
            Field<C, T0> fieldA, Field<C, T1> fieldB, Field<C, T2> fieldC,
            Field<C, T3> fieldD, Field<C, T4> fieldE, Field<C, T5> fieldF
    ) {
        return compositeInternal(
                args -> factory.apply(
                        (T0) args[0], (T1) args[1], (T2) args[2],
                        (T3) args[3], (T4) args[4], (T5) args[5]
                ),
                fieldA, fieldB, fieldC, fieldD, fieldE, fieldF
        );
    }

    public static <C, T0, T1, T2, T3, T4, T5, T6> RecordCodec<C> composite(
            Functions.Function7<T0, T1, T2, T3, T4, T5, T6, C> factory,
            Field<C, T0> fieldA, Field<C, T1> fieldB, Field<C, T2> fieldC,
            Field<C, T3> fieldD, Field<C, T4> fieldE, Field<C, T5> fieldF,
            Field<C, T6> fieldG
    ) {
        return compositeInternal(
                args -> factory.apply(
                        (T0) args[0], (T1) args[1], (T2) args[2],
                        (T3) args[3], (T4) args[4], (T5) args[5], (T6) args[6]
                ),
                fieldA, fieldB, fieldC, fieldD, fieldE, fieldF, fieldG
        );
    }

    public static <C, T0, T1, T2, T3, T4, T5, T6, T7> RecordCodec<C> composite(
            Functions.Function8<T0, T1, T2, T3, T4, T5, T6, T7, C> factory,
            Field<C, T0> fieldA, Field<C, T1> fieldB, Field<C, T2> fieldC,
            Field<C, T3> fieldD, Field<C, T4> fieldE, Field<C, T5> fieldF,
            Field<C, T6> fieldG, Field<C, T7> fieldH
    ) {
        return compositeInternal(
                args -> factory.apply(
                        (T0) args[0], (T1) args[1], (T2) args[2],
                        (T3) args[3], (T4) args[4], (T5) args[5],
                        (T6) args[6], (T7) args[7]
                ),
                fieldA, fieldB, fieldC, fieldD, fieldE, fieldF, fieldG, fieldH
        );
    }

    public static <C, T0, T1, T2, T3, T4, T5, T6, T7, T8> RecordCodec<C> composite(
            Functions.Function9<T0, T1, T2, T3, T4, T5, T6, T7, T8, C> factory,
            Field<C, T0> fieldA, Field<C, T1> fieldB, Field<C, T2> fieldC,
            Field<C, T3> fieldD, Field<C, T4> fieldE, Field<C, T5> fieldF,
            Field<C, T6> fieldG, Field<C, T7> fieldH, Field<C, T8> fieldI
    ) {
        return compositeInternal(
                args -> factory.apply(
                        (T0) args[0], (T1) args[1], (T2) args[2],
                        (T3) args[3], (T4) args[4], (T5) args[5],
                        (T6) args[6], (T7) args[7], (T8) args[8]
                ),
                fieldA, fieldB, fieldC, fieldD, fieldE, fieldF, fieldG, fieldH, fieldI
        );
    }

    public static <C, T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> RecordCodec<C> composite(
            Functions.Function10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, C> factory,
            Field<C, T0> fieldA, Field<C, T1> fieldB, Field<C, T2> fieldC,
            Field<C, T3> fieldD, Field<C, T4> fieldE, Field<C, T5> fieldF,
            Field<C, T6> fieldG, Field<C, T7> fieldH, Field<C, T8> fieldI,
            Field<C, T9> fieldJ
    ) {
        return compositeInternal(
                args -> factory.apply(
                        (T0) args[0], (T1) args[1], (T2) args[2],
                        (T3) args[3], (T4) args[4], (T5) args[5],
                        (T6) args[6], (T7) args[7], (T8) args[8],
                        (T9) args[9]
                ),
                fieldA, fieldB, fieldC, fieldD, fieldE, fieldF, fieldG, fieldH, fieldI, fieldJ
        );
    }

    /*
    Internals
    */

    public record Field<C, T>(String name, Codec<T> codec, Function<C, T> getter, T defaultValue) {
        public Field(String name, Codec<T> codec, Function<C, T> getter) {
            this(name, codec, getter, null);
        }

        T decode(BsonDocument doc, ExtraInfo info) {
            return doc.containsKey(name) && !doc.get(name).isNull() ? codec.decode(doc.get(name), info) : defaultValue;
        }

        void encode(BsonDocument doc, C value, ExtraInfo info) {
            doc.put(name, codec.encode(getter.apply(value), info));
        }

        void schema(SchemaContext ctx, Map<String, Schema> props) {
            props.put(name, codec.toSchema(ctx));
        }
    }

    @SafeVarargs
    private static <C> RecordCodec<C> compositeInternal(
            Function<Object[], C> factory,
            Field<C, ?>... fields
    ) {
        return new RecordCodec<>() {
            @Override
            public C decode(BsonValue value, ExtraInfo info) {
                BsonDocument doc = value.asDocument();
                Object[] args = new Object[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    args[i] = fields[i].decode(doc, info);
                }
                return factory.apply(args);
            }

            @Override
            public void encode(BsonDocument doc, C value, ExtraInfo info) {
                for (Field<C, ?> f : fields) {
                    f.encode(doc, value, info);
                }
            }

            @Override
            public void toSchema(SchemaContext ctx, Map<String, Schema> props) {
                for (Field<C, ?> f : fields) {
                    f.schema(ctx, props);
                }
            }
        };
    }

    @Nonnull
    public static String readRawJson(@Nonnull RawJsonReader reader) throws IOException {
        reader.consumeWhiteSpace();
        int first = reader.peek();
        if (first == -1) throw new IOException("Unexpected EOF!");

        StringBuilder sb = new StringBuilder();
        char startChar = (char) first;

        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        while (true) {
            int j = reader.read();
            if (j == -1) break;
            char c = (char) j;
            sb.append(c);

            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            switch (c) {
                case '"' -> inString = true;
                case '{', '[' -> depth++;
                case '}', ']' -> {
                    depth--;
                    if (depth == 0) return sb.toString();
                }
            }
        }

        throw new IOException("Unexpected EOF!");
    }


    @Nullable
    @Override
    public C decodeJson(@Nonnull RawJsonReader reader, ExtraInfo extraInfo) throws IOException {
        String rawJson = readRawJson(reader);
        BsonValue bson = BsonDocument.parse(rawJson);
        return this.decode(bson, extraInfo);
    }

    @Override
    public BsonValue encode(C value, ExtraInfo info) {
        BsonDocument document = new BsonDocument();
        encode(document, value, info);
        return document;
    }

    abstract public void encode(BsonDocument document, C value, ExtraInfo info);

    @Nonnull
    @Override
    public Schema toSchema(@Nonnull SchemaContext context) {
        ObjectSchema schema = new ObjectSchema();
        Map<String, Schema> properties = new Object2ObjectLinkedOpenHashMap<>();
        toSchema(context, properties);
        schema.setProperties(properties);
        return schema;
    }

    abstract public void toSchema(SchemaContext context, Map<String, Schema> properties);
}
