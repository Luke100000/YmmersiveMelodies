package net.conczin.data;

import java.util.*;

/**
 * Mapeia notas MIDI para combinações de teclas do teclado.
 * Baseado no mapeamento inteligente do código C# de referência.
 */
public class MidiToQwertyMapping {
    private static final Map<Integer, List<Integer>> KEY_MAPPING = createIntelligentKeyMapping();

    /**
     * Cria o mapeamento de notas MIDI para teclas do teclado.
     * 
     * Mapeamento:
     * - Notas baixas (21-35): Ctrl + teclas (D1-D0, Q-T)
     * - Notas médias (36-96): teclas simples ou Shift + teclas
     * - Notas altas (97-108): Ctrl + teclas (Y-M, J)
     * 
     * Códigos de teclas seguem padrão:
     * - Números: 1-9, 0 (49-57, 48)
     * - Letras: A-Z (65-90)
     * - Modificadores: Left Ctrl (162), Left Shift (160)
     */
    private static Map<Integer, List<Integer>> createIntelligentKeyMapping() {
        Map<Integer, List<Integer>> mapping = new HashMap<>();

        // --- Middle Keys (C2-C7) - Chromatic Mapping ---
        // C2 (36) -> 1
        mapping.put(36, Arrays.asList(49));
        // C#2 (37) -> Shift + 1 (!)
        mapping.put(37, Arrays.asList(160, 49));
        // D2 (38) -> 2
        mapping.put(38, Arrays.asList(50));
        // D#2 (39) -> Shift + 2 (@)
        mapping.put(39, Arrays.asList(160, 50));
        // E2 (40) -> 3
        mapping.put(40, Arrays.asList(51));
        // E#2 (Enharmonic F2) -> Shift + 3 (#) - Maps to F2 MIDI Note for playability
        // Note: We can't put same key for multiple notes easily if reverse lookup is
        // simple,
        // BUT here we are mapping Note -> Keys.
        // Wait, the system looks up Note -> Keys for playback display (autoupdate)
        // AND Keys -> Note for Input.
        // We need to support Keys -> Note.
        // The current map is Note -> Keys.
        // For Input, we iterate this map. If F2(41) is mapped to '4', we won't find
        // 'Shift+3'.
        // We need to add 'fake' note entries or handle multiple keys per note?
        // Actually, checking findMidiNoteForKeyCombo... it iterates the map.
        // If (41) is associated with [52] ('4'), it won't match [160, 51] ('#').
        // I need to add a way to map multiple key combos to the same note,
        // OR add specific "fake" notes that play the real note?
        // EASIER: Just add duplication in the list?
        // Map<Integer, List<Integer>> ... The Value is a List of integers representing
        // ONE key play (Modifier + Key).
        // It does NOT support multiple alternative keybindings for the same note in
        // this structure.
        //
        // However, looking at the code:
        // findMidiNoteForKeyCombo iterates entries.
        // If I simply add another entry for F2? Map keys must be unique.
        // F2 is key 41. It's already there.
        // I can't add another 41.

        // SOLUTION:
        // Use negative/unused MIDI IDs for these "Ghost" keys, then in the play logic,
        // remap them to the real notes?
        // OR, better: Allow the map to hold complex data? Too much refactor.
        //
        // Hack: Map them to "Visual Only" notes or duplicates?
        // No, simplest is to just manually handle these specific weird keys in
        // MidiPerformanceGui?
        // NO, that's messy.
        //
        // Better: Change the structure of the data? No.
        //
        // Wait, look at findMidiNoteForKeyCombo:
        // loops through map.
        //
        // If I want Shift+3 to play F2 (41)...
        // but 41 is mapped to '4'.
        //
        // I can add a fake MIDI note, say 1000 + 41, which maps to Shift+3.
        // Then in the play method, we mod 1000? or just let it play?
        // If I request to play MIDI note 1041, the system might crash if it expects
        // valid range 21-108.
        // But the playMidiNote method calculates pitch from midiNote.
        // 1041 would be extremely high pitch.
        //
        // Okay, let's look at `findMidiNoteForKeyCombo`.
        // It serves INPUT.
        //
        // Can I just add entry: mapping.put(-100, Arrays.asList(160, 51)); // # -> -100
        // Then in Gui: if note < 0, note = realNote?
        // Or if note is special code...
        //
        // Alternative:
        // `createIntelligentKeyMapping` returns the canonical mapping for "Show me how
        // to play X".
        // But for "Input X", we can have a secondary map.
        // But I don't want to create a new map structure just for 3 keys.
        //
        // Let's look at `findMidiNoteForKeyCombo` again.
        /*
         * public static int findMidiNoteForKeyCombo(int modifierCode, int keyCode) {
         * for (Map.Entry<Integer, List<Integer>> entry : KEY_MAPPING.entrySet()) {
         * List<Integer> keys = entry.getValue();
         * if (keys.size() == 2 && modifierCode != 0) {
         * if (keys.get(0) == modifierCode && keys.get(1) == keyCode) {
         * return entry.getKey();
         * }
         * }
         * }
         * return -1;
         * }
         */

        // I can hardcode specific overrides in `findMidiNoteForKeyCombo`!
        // This is the cleanest way without breaking the canonical visual mapping.

        // F2 (41) -> 4
        mapping.put(41, Arrays.asList(52));
        // F#2 (42) -> Shift + 4 ($)
        mapping.put(42, Arrays.asList(160, 52));
        // G2 (43) -> 5
        mapping.put(43, Arrays.asList(53));
        // G#2 (44) -> Shift + 5 (%)
        mapping.put(44, Arrays.asList(160, 53));
        // A2 (45) -> 6
        mapping.put(45, Arrays.asList(54));
        // A#2 (46) -> Shift + 6 (^)
        mapping.put(46, Arrays.asList(160, 54));
        // B2 (47) -> 7
        mapping.put(47, Arrays.asList(55));

        // C3 (48) -> 8
        mapping.put(48, Arrays.asList(56));
        // C#3 (49) -> Shift + 8 (*)
        mapping.put(49, Arrays.asList(160, 56));
        // D3 (50) -> 9
        mapping.put(50, Arrays.asList(57));
        // D#3 (51) -> Shift + 9 (()
        mapping.put(51, Arrays.asList(160, 57));
        // E3 (52) -> 0
        mapping.put(52, Arrays.asList(48));
        // F3 (53) -> Q
        mapping.put(53, Arrays.asList(81));
        // F#3 (54) -> Shift + Q
        mapping.put(54, Arrays.asList(160, 81));
        // G3 (55) -> W
        mapping.put(55, Arrays.asList(87));
        // G#3 (56) -> Shift + W
        mapping.put(56, Arrays.asList(160, 87));
        // A3 (57) -> E
        mapping.put(57, Arrays.asList(69));
        // A#3 (58) -> Shift + E
        mapping.put(58, Arrays.asList(160, 69));
        // B3 (59) -> R
        mapping.put(59, Arrays.asList(82));

        // C4 (60) -> T
        mapping.put(60, Arrays.asList(84));
        // C#4 (61) -> Shift + T
        mapping.put(61, Arrays.asList(160, 84));
        // D4 (62) -> Y
        mapping.put(62, Arrays.asList(89));
        // D#4 (63) -> Shift + Y
        mapping.put(63, Arrays.asList(160, 89));
        // E4 (64) -> U
        mapping.put(64, Arrays.asList(85));
        // F4 (65) -> I
        mapping.put(65, Arrays.asList(73));
        // F#4 (66) -> Shift + I
        mapping.put(66, Arrays.asList(160, 73));
        // G4 (67) -> O
        mapping.put(67, Arrays.asList(79));
        // G#4 (68) -> Shift + O
        mapping.put(68, Arrays.asList(160, 79));
        // A4 (69) -> P
        mapping.put(69, Arrays.asList(80));
        // A#4 (70) -> Shift + P
        mapping.put(70, Arrays.asList(160, 80));
        // B4 (71) -> A
        mapping.put(71, Arrays.asList(65));

        // C5 (72) -> S
        mapping.put(72, Arrays.asList(83));
        // C#5 (73) -> Shift + S
        mapping.put(73, Arrays.asList(160, 83));
        // D5 (74) -> D
        mapping.put(74, Arrays.asList(68));
        // D#5 (75) -> Shift + D
        mapping.put(75, Arrays.asList(160, 68));
        // E5 (76) -> F
        mapping.put(76, Arrays.asList(70));
        // F5 (77) -> G
        mapping.put(77, Arrays.asList(71));
        // F#5 (78) -> Shift + G
        mapping.put(78, Arrays.asList(160, 71));
        // G5 (79) -> H
        mapping.put(79, Arrays.asList(72));
        // G#5 (80) -> Shift + H
        mapping.put(80, Arrays.asList(160, 72));
        // A5 (81) -> J
        mapping.put(81, Arrays.asList(74));
        // A#5 (82) -> Shift + J
        mapping.put(82, Arrays.asList(160, 74));
        // B5 (83) -> K
        mapping.put(83, Arrays.asList(75));

        // C6 (84) -> L
        mapping.put(84, Arrays.asList(76));
        // C#6 (85) -> Shift + L
        mapping.put(85, Arrays.asList(160, 76));
        // D6 (86) -> Z
        mapping.put(86, Arrays.asList(90));
        // D#6 (87) -> Shift + Z
        mapping.put(87, Arrays.asList(160, 90));
        // E6 (88) -> X
        mapping.put(88, Arrays.asList(88));
        // F6 (89) -> C
        mapping.put(89, Arrays.asList(67));
        // F#6 (90) -> Shift + C
        mapping.put(90, Arrays.asList(160, 67));
        // G6 (91) -> V
        mapping.put(91, Arrays.asList(86));
        // G#6 (92) -> Shift + V
        mapping.put(92, Arrays.asList(160, 86));
        // A6 (93) -> B
        mapping.put(93, Arrays.asList(66));
        // A#6 (94) -> Shift + B
        mapping.put(94, Arrays.asList(160, 66));
        // B6 (95) -> N
        mapping.put(95, Arrays.asList(78));
        // C7 (96) -> M
        mapping.put(96, Arrays.asList(77));

        // --- High Keys (C#7-C8) - Using Ctrl ---

        return Collections.unmodifiableMap(mapping);
    }

    private static void addMap(Map<Integer, List<Integer>> mapping, int midiNote, char key) {
        // Map as simple key press
        mapping.put(midiNote, Arrays.asList((int) key));
    }

    /**
     * Obtém a combinação de teclas para uma nota MIDI específica.
     * 
     * @param midiNote A nota MIDI (21-108)
     * @return Lista de códigos de teclas. Primeira tecla é modificador (se houver),
     *         última é a tecla principal.
     *         Retorna lista vazia se a nota não estiver mapeada.
     */
    public static List<Integer> getKeyMapping(int midiNote) {
        return KEY_MAPPING.getOrDefault(midiNote, Collections.emptyList());
    }

    /**
     * Verifica se uma nota MIDI está mapeada.
     * 
     * @param midiNote A nota MIDI a verificar
     * @return true se a nota estiver mapeada, false caso contrário
     */
    public static boolean isMapped(int midiNote) {
        return KEY_MAPPING.containsKey(midiNote);
    }

    /**
     * Obtém todas as notas MIDI que estão mapeadas.
     * 
     * @return Conjunto de notas MIDI mapeadas
     */
    public static Set<Integer> getMappedNotes() {
        return KEY_MAPPING.keySet();
    }

    /**
     * Encontra a nota MIDI correspondente a uma tecla específica.
     * Busca no mapeamento reverso (tecla -> nota MIDI).
     * 
     * @param keyCode Código da tecla principal (sem modificadores)
     * @return Nota MIDI correspondente, ou -1 se não encontrada
     */
    public static int findMidiNoteForKey(int keyCode) {
        for (Map.Entry<Integer, List<Integer>> entry : KEY_MAPPING.entrySet()) {
            List<Integer> keys = entry.getValue();
            if (!keys.isEmpty()) {
                // A última tecla na lista é sempre a tecla principal
                int mainKey = keys.get(keys.size() - 1);
                if (mainKey == keyCode) {
                    return entry.getKey();
                }
            }
        }
        return -1;
    }

    /**
     * Encontra a nota MIDI correspondente a uma combinação de teclas (modificador +
     * tecla).
     * 
     * @param modifierCode Código do modificador (Ctrl=162, Shift=160, ou 0 se
     *                     nenhum)
     * @param keyCode      Código da tecla principal
     * @return Nota MIDI correspondente, ou -1 se não encontrada
     */
    public static int findMidiNoteForKeyCombo(int modifierCode, int keyCode) {
        // Hardcoded Enharmonics for user convenience (E# -> F, B# -> C)
        // Shift + 3 (#) -> F2 (41)
        if (modifierCode == 160 && keyCode == 51)
            return 41;
        // Shift + 7 (&) -> C3 (48)
        if (modifierCode == 160 && keyCode == 55)
            return 48;
        // Shift + 0 ()) -> F3 (53)
        if (modifierCode == 160 && keyCode == 48)
            return 53;

        for (Map.Entry<Integer, List<Integer>> entry : KEY_MAPPING.entrySet()) {
            List<Integer> keys = entry.getValue();
            if (keys.size() == 1 && modifierCode == 0) {
                // Tecla simples sem modificador
                if (keys.get(0) == keyCode) {
                    return entry.getKey();
                }
            } else if (keys.size() == 2 && modifierCode != 0) {
                // Tecla com modificador
                if (keys.get(0) == modifierCode && keys.get(1) == keyCode) {
                    return entry.getKey();
                }
            }
        }
        return -1;
    }
}
