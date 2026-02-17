package net.conczin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;


public final class MelodyProgress {
    public static final BuilderCodec<MelodyProgress> CODEC = BuilderCodec.builder(MelodyProgress.class, MelodyProgress::new)
            .appendInherited(
                    new KeyedCodec<>("Melody", Codec.STRING),
                    (o, v) -> o.melody = v,
                    o -> o.melody,
                    (o, p) -> o.melody = p.melody)
            .add()
            .appendInherited(
                    new KeyedCodec<>("WorldTime", Codec.LONG),
                    (o, v) -> o.worldTime = v,
                    o -> o.worldTime,
                    (o, p) -> o.worldTime = p.worldTime)
            .add()
            .appendInherited(
                    new KeyedCodec<>("Time", Codec.LONG),
                    (o, v) -> o.time = v,
                    o -> o.time,
                    (o, p) -> o.time = p.time)
            .add()
            .appendInherited(
                    new KeyedCodec<>("StartWorldTime", Codec.LONG),
                    (o, v) -> o.startWorldTime = v,
                    o -> o.startWorldTime,
                    (o, p) -> o.startWorldTime = p.startWorldTime)
            .add()
            .build();

    public String melody = "";
    public long worldTime = 0;
    public long time = 0;
    public long startWorldTime = 0;
}
