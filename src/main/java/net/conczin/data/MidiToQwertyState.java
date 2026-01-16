package net.conczin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Estado do modo MidiToQwerty para um jogador.
 * Armazena se o modo está ativo ou não.
 */
public final class MidiToQwertyState {
    public static final BuilderCodec<MidiToQwertyState> CODEC = BuilderCodec.builder(MidiToQwertyState.class, MidiToQwertyState::new)
            .appendInherited(
                    new KeyedCodec<>("Active", Codec.BOOLEAN),
                    (o, v) -> o.active = v,
                    o -> o.active,
                    (o, p) -> o.active = p.active)
            .add()
            .build();

    public boolean active = false;

    public MidiToQwertyState() {
    }

    public MidiToQwertyState(boolean active) {
        this.active = active;
    }
}

