package net.conczin.data;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

public class MelodyAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, MelodyAsset>> {
    public static final AssetBuilderCodec<String, MelodyAsset> CODEC = AssetBuilderCodec.builder(
                    MelodyAsset.class,
                    MelodyAsset::new,
                    Codec.STRING,
                    (t, id) -> t.id = id,
                    t -> t.id,
                    (t, data) -> t.data = data,
                    t -> t.data
            )
            .appendInherited(
                    new KeyedCodec<>("Name", Codec.STRING),
                    (item, v) -> item.name = v,
                    item -> item.name,
                    (item, parent) -> item.name = parent.name
            )
            .add()
            .afterDecode((o, extrainfo) -> {
                if (o.id != null && extrainfo instanceof AssetExtraInfo<?> assetExtraInfo) {
                    Path path = assetExtraInfo.getAssetPath();
                    if (path == null) return;
                    Path midiPath = path.getParent().resolve(path.getFileName().toString().replaceFirst("\\.json$", ".midi"));
                    try {
                        InputStream midiStream = new FileInputStream(midiPath.toFile());
                        o.melody = new Melody(o.name, MidiParser.parseMidi(midiStream));
                    } catch (FileNotFoundException e) {
                        // Nop
                    }
                }
            })
            .build();


    private static AssetStore<String, MelodyAsset, DefaultAssetMap<String, MelodyAsset>> ASSET_STORE;

    public static AssetStore<String, MelodyAsset, DefaultAssetMap<String, MelodyAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(MelodyAsset.class);
        }
        return ASSET_STORE;
    }

    private String id;
    private AssetExtraInfo.Data data;

    private String name;

    private Melody melody;

    @Override
    public String getId() {
        return id;
    }

    public Melody getMelody() {
        return melody;
    }
}
