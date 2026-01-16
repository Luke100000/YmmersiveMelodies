package net.conczin.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.data.MidiToQwertyMapping;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Intercepta teclas do teclado quando o jogador está no modo MidiToQwerty.
 * Previne ações normais do jogo (como "G" para dropar item) e captura as teclas para processamento.
 * 
 * NOTA: Para funcionar completamente, este sistema precisa ser conectado a um listener/hook
 * do Hytale que capture eventos de teclas antes que sejam processados pelo jogo.
 * O método handleKeyPress() deve ser chamado quando um evento de tecla é detectado.
 */
public class KeyboardInterceptor {
    private static final Map<UUID, Consumer<Integer>> activeInterceptors = new HashMap<>();
    private static final Map<UUID, Function<Ref<EntityStore>, Boolean>> f5Handlers = new HashMap<>();
    
    // F5 key code = 116
    private static final int F5_KEY_CODE = 116;
    
    /**
     * Ativa a interceptação de teclas para um jogador.
     * Quando ativado, as teclas serão capturadas e não executarão ações normais.
     * 
     * @param playerId ID do jogador
     * @param keyHandler Handler para processar as teclas capturadas
     */
    public static void enableInterception(UUID playerId, Consumer<Integer> keyHandler) {
        activeInterceptors.put(playerId, keyHandler);
    }
    
    /**
     * Desativa a interceptação de teclas para um jogador.
     * 
     * @param playerId ID do jogador
     */
    public static void disableInterception(UUID playerId) {
        activeInterceptors.remove(playerId);
    }
    
    /**
     * Verifica se a interceptação está ativa para um jogador.
     * 
     * @param playerId ID do jogador
     * @return true se a interceptação está ativa
     */
    public static boolean isIntercepting(UUID playerId) {
        return activeInterceptors.containsKey(playerId);
    }
    
    /**
     * Registra um handler para F5 (abrir console de debug).
     * 
     * @param playerId ID do jogador
     * @param handler Função que recebe a ref e retorna true se o console foi aberto
     */
    public static void registerF5Handler(UUID playerId, Function<Ref<EntityStore>, Boolean> handler) {
        f5Handlers.put(playerId, handler);
    }
    
    /**
     * Remove o handler de F5 para um jogador.
     * 
     * @param playerId ID do jogador
     */
    public static void unregisterF5Handler(UUID playerId) {
        f5Handlers.remove(playerId);
    }
    
    /**
     * Processa uma tecla pressionada.
     * Se a interceptação estiver ativa para o jogador, previne a ação normal e processa a tecla.
     * Se for F5 (keyCode 116), tenta abrir o console de debug.
     * 
     * IMPORTANTE: Este método precisa ser chamado quando um evento de tecla é detectado.
     * Conecte este método a um listener/hook do Hytale que capture eventos de teclas.
     * Exemplo: registrar um listener de eventos de input que chame este método para cada tecla pressionada.
     * 
     * @param ref Referência da entidade do jogador
     * @param keyCode Código da tecla pressionada (F5 = 116)
     * @return true se a tecla foi interceptada e processada, false caso contrário
     */
    public static boolean handleKeyPress(@Nonnull Ref<EntityStore> ref, int keyCode) {
        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        if (player == null) return false;
        
        UUID playerId = Utils.getUUID(ref);
        
        // Verificar se é F5 para abrir console
        if (keyCode == F5_KEY_CODE) {
            Function<Ref<EntityStore>, Boolean> f5Handler = f5Handlers.get(playerId);
            if (f5Handler != null) {
                try {
                    if (f5Handler.apply(ref)) {
                        return true; // F5 foi processado
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        Consumer<Integer> handler = activeInterceptors.get(playerId);
        
        if (handler != null) {
            // Tecla foi interceptada - processar e prevenir ação normal
            handler.accept(keyCode);
            return true;
        }
        
        return false;
    }
    
    /**
     * Converte código de tecla para nota MIDI usando mapeamento reverso.
     * Encontra qual nota MIDI corresponde à tecla pressionada.
     * 
     * @param keyCode Código da tecla (ex: 71 para 'G')
     * @return Nota MIDI correspondente, ou -1 se não mapeada
     */
    public static int keyToMidiNote(int keyCode) {
        // Buscar no mapeamento reverso
        // Nota: Isso requer criar um mapeamento reverso do MidiToQwertyMapping
        for (int midiNote = 21; midiNote <= 108; midiNote++) {
            List<Integer> mappedKeys = MidiToQwertyMapping.getKeyMapping(midiNote);
            if (!mappedKeys.isEmpty()) {
                // Verificar se a última tecla (tecla principal) corresponde
                int mainKey = mappedKeys.get(mappedKeys.size() - 1);
                if (mainKey == keyCode) {
                    return midiNote;
                }
            }
        }
        return -1;
    }
}

