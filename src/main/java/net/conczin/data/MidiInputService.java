package net.conczin.data;

import javax.sound.midi.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Serviço para capturar eventos MIDI de dispositivos físicos em tempo real.
 * Detecta dispositivos MIDI disponíveis e processa eventos Note On/Off.
 */
public class MidiInputService {
    private static MidiInputService instance;
    private final Map<UUID, Consumer<Integer>> activeListeners = new ConcurrentHashMap<>();
    private MidiDevice midiDevice;
    private boolean isRunning = false;

    private MidiInputService() {
    }

    public static synchronized MidiInputService getInstance() {
        if (instance == null) {
            instance = new MidiInputService();
        }
        return instance;
    }

    /**
     * Inicializa o serviço e tenta conectar ao primeiro dispositivo MIDI
     * disponível.
     * 
     * @return true se um dispositivo MIDI foi encontrado e conectado, false caso
     *         contrário
     */
    /**
     * Inicializa o serviço e tenta conectar ao primeiro dispositivo MIDI
     * disponível.
     * 
     * @return true se um dispositivo MIDI foi encontrado e conectado, false caso
     *         contrário
     */
    public boolean initialize() {
        if (isRunning) {
            return true;
        }
        // Default to first device
        return selectDevice(0);
    }

    /**
     * Conecta a um dispositivo MIDI específico pelo índice na lista de disponíveis.
     * 
     * @param index Índice do dispositivo (0-based) na lista retornada por
     *              getAvailableDevices()
     * @return true se conectado com sucesso
     */
    public boolean selectDevice(int index) {
        // Stop current if any
        if (midiDevice != null && midiDevice.isOpen()) {
            midiDevice.close();
        }
        midiDevice = null;
        isRunning = false;

        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        int validConstructorIndex = 0;

        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);

                // Check capability
                if (device.getMaxTransmitters() != 0) {
                    if (validConstructorIndex == index) {
                        device.open();
                        midiDevice = device;

                        Transmitter transmitter = device.getTransmitter();
                        transmitter.setReceiver(new MidiInputReceiver());

                        isRunning = true;
                        System.out.println("[MidiInputService] Connected to: " + info.getName());
                        return true;
                    }
                    validConstructorIndex++;
                }
            } catch (MidiUnavailableException e) {
                // Skip
            }
        }
        return false;
    }

    /**
     * Para o serviço e fecha o dispositivo MIDI.
     */
    public void shutdown() {
        if (!isRunning) {
            return;
        }

        isRunning = false;

        if (midiDevice != null && midiDevice.isOpen()) {
            midiDevice.close();
            midiDevice = null;
        }

        activeListeners.clear();
    }

    /**
     * Registra um listener para receber eventos de notas MIDI.
     * 
     * @param playerId     ID único do jogador
     * @param noteCallback Callback chamado quando uma nota MIDI é pressionada (Note
     *                     On)
     * @return true se registrado com sucesso, false se o serviço não estiver
     *         rodando
     */
    public boolean registerListener(UUID playerId, Consumer<Integer> noteCallback) {
        if (!isRunning) {
            return false;
        }

        activeListeners.put(playerId, noteCallback);
        return true;
    }

    /**
     * Remove um listener registrado.
     * 
     * @param playerId ID único do jogador
     */
    public void unregisterListener(UUID playerId) {
        activeListeners.remove(playerId);
    }

    /**
     * Verifica se o serviço está rodando e conectado a um dispositivo MIDI.
     * 
     * @return true se o serviço está ativo
     */
    public boolean isRunning() {
        return isRunning && midiDevice != null && midiDevice.isOpen();
    }

    /**
     * Verifica se o serviço foi inicializado (mesmo que não esteja rodando).
     * 
     * @return true se o serviço foi inicializado
     */
    public boolean isInitialized() {
        return isRunning;
    }

    /**
     * Obtém lista de dispositivos MIDI disponíveis.
     * 
     * @return Lista de nomes dos dispositivos MIDI disponíveis
     */
    public List<String> getAvailableDevices() {
        List<String> devices = new ArrayList<>();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxTransmitters() != 0) {
                    devices.add(info.getName());
                }
            } catch (MidiUnavailableException e) {
                // Ignorar dispositivos indisponíveis
            }
        }

        return devices;
    }

    /**
     * Receptor MIDI interno que processa eventos MIDI e notifica os listeners.
     */
    private class MidiInputReceiver implements Receiver {
        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (message instanceof ShortMessage sm) {
                int command = sm.getCommand();

                // Processar apenas Note On (com velocity > 0) e Note Off
                if (command == ShortMessage.NOTE_ON) {
                    int velocity = sm.getData2();
                    if (velocity > 0) {
                        // Note On - notificar todos os listeners ativos
                        int note = sm.getData1();
                        for (Consumer<Integer> callback : activeListeners.values()) {
                            try {
                                callback.accept(note);
                            } catch (Exception e) {
                                // Ignorar erros em callbacks individuais
                                e.printStackTrace();
                            }
                        }
                    }
                }
                // Note Off pode ser ignorado para este caso de uso
                // pois estamos apenas simulando pressionamento de teclas
            }
        }

        @Override
        public void close() {
            // Limpar recursos se necessário
        }
    }
}
