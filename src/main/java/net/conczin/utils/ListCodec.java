package net.conczin.utils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.RawJsonCodec;
import com.hypixel.hytale.codec.WrappedCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ArraySchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import org.bson.BsonArray;
import org.bson.BsonNull;
import org.bson.BsonValue;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ListCodec<T> implements Codec<List<T>>, RawJsonCodec<List<T>>, WrappedCodec<T> {
    private final Codec<T> codec;

    public ListCodec(Codec<T> codec) {
        this.codec = codec;
    }

    @Override
    public Codec<T> getChildCodec() {
        return codec;
    }

    @Override
    public List<T> decode(BsonValue value, ExtraInfo info) {
        BsonArray arr = value.asArray();
        List<T> out = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            info.pushIntKey(i);
            try {
                out.add(arr.get(i).isNull() ? null : codec.decode(arr.get(i), info));
            } finally {
                info.popKey();
            }
        }
        return out;
    }

    @Override
    public BsonValue encode(List<T> list, ExtraInfo info) {
        BsonArray arr = new BsonArray();
        for (T v : list) {
            arr.add(v == null ? new BsonNull() : codec.encode(v, info));
        }
        return arr;
    }

    @Override
    public List<T> decodeJson(RawJsonReader reader, ExtraInfo extraInfo) throws IOException {
        BsonValue bsonvalue = RawJsonReader.readBsonValue(reader);
        return this.decode(bsonvalue, extraInfo);
    }

    @Override
    @Nonnull
    public Schema toSchema(SchemaContext ctx) {
        ArraySchema s = new ArraySchema();
        s.setItem(ctx.refDefinition(codec));
        return s;
    }
}
