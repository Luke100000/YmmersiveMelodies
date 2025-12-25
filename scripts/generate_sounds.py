import json
import math
import os
from pathlib import Path

import numpy as np
from pydub import AudioSegment

template = """{
  "Layers": [
    {
      "Files": [
        "Sounds/{instrument}/c{octave}_{length}ms.ogg"
      ],
      "Volume": 0
    }
  ],
  "Volume": 0,
  "Pitch": 0,
  "MaxDistance": 40,
  "StartAttenuationDistance": 5,
  "AudioCategory": "Ymmersive_Melodies_Instrument"
}"""


def process(audio: Path, output: Path, length: float, fade: float):
    seg = AudioSegment.from_ogg(audio)

    samples = np.array(seg.get_array_of_samples()).astype(np.float32)
    win = int(0.03 * seg.frame_rate)  # 30 ms blur window
    if win < 1:
        win = 1

    # blurred absolute amplitude
    abs_s = np.abs(samples)
    kernel = np.ones(win) / win
    blurred = np.convolve(abs_s, kernel, mode="same")

    decay_len = min(seg.frame_rate, len(blurred))
    decay = np.linspace(1.0, 0.0, decay_len)
    blurred = blurred[:decay_len] * decay

    peak = np.argmax(blurred) / seg.frame_rate  # seconds of peak
    center = (length + fade) / 4

    shift = max(0, peak - center)  # only shift left
    seg = seg[int(shift * 1000) :]

    total = (length + fade) * 1000
    if len(seg) < total:
        seg += AudioSegment.silent(duration=total - len(seg))
    seg = seg[:total]

    if shift > 0:
        seg = seg.fade_in(int((length / 4) * 1000))

    seg = seg.fade_out(int(fade * 1000))
    seg.export(output, format="ogg")


def humanize(name: str) -> str:
    return " ".join(
        part.capitalize() for part in name.replace("_", " ").replace("-", " ").split()
    )


def main():
    sounds_source_root = Path(__file__).parent / "instruments"

    pack_root = Path(__file__).parent.parent / "src/main/resources"
    sounds_root = pack_root / "Common/Sounds"
    event_root = pack_root / "Server/Audio/SoundEvents"
    midi_root = pack_root / "Server/YmmersiveMelodies"

    for dirpath, _, filenames in os.walk(midi_root):
        for fn in filenames:
            base, ext = os.path.splitext(fn)
            if ext.lower() in ".midi":
                json_path = os.path.join(dirpath, base + ".json")
                if os.path.exists(json_path):
                    continue
                payload = {"Name": humanize(base)}
                with open(json_path, "w", encoding="utf-8") as f:
                    json.dump(payload, f, ensure_ascii=False, indent=2)

    # Rename all to title case
    for dirpath, _, filenames in os.walk(midi_root):
        for fn in filenames:
            base, ext = os.path.splitext(fn)
            corrected = base.replace("_", " ").title().replace(" ", "_") + ext
            if fn != corrected:
                os.rename(os.path.join(dirpath, fn), os.path.join(dirpath, corrected))

    for instrument in sounds_source_root.iterdir():
        for octave in range(1, 9):
            for length in [
                0.125,
                0.25,
                0.375,
                0.5,
                0.625,
                0.75,
                0.875,
                1,
                1.25,
                1.5,
                1.75,
                2,
                2.5,
                3,
                4
            ]:
                # Generate sound file
                length_ms = int(length * 1000)
                output_path = (
                    sounds_root / instrument.name / f"c{octave}_{length_ms}ms.ogg"
                )
                output_path.parent.mkdir(parents=True, exist_ok=True)

                fade = math.sqrt(length) * 0.25
                process(
                    instrument / f"c{octave}.ogg",
                    output_path,
                    length=length - fade / 2,
                    fade=length,
                )

                # Generate sound event file
                path = (
                    event_root
                    / instrument.name
                    / f"SFX_Ymmersive_Melodies_{instrument.name}_C{octave}_{length_ms}ms.json"
                )
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(
                    template.replace("{instrument}", instrument.name)
                    .replace("{length}", str(length_ms))
                    .replace("{octave}", str(octave))
                )


if __name__ == "__main__":
    main()
