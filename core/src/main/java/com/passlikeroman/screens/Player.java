package com.passlikeroman.screens;

public class Player {

    public String name;
    public int number;

    // Estadísticas del kit/personaje.
    public float efecto;
    public float precision;
    public float potencia;
    public float recepcion;

    public float x;
    public float y;

    // ============================================================
    // ANIMACIÓN
    // ============================================================

    public enum ReceiveType {
        NONE, FOOT, CHEST, HEAD
    }

    // Cuánto dura la animación de patada al pasar.
    private static final float KICK_DURATION = 0.22f;

    // Cuánto dura la animación al recibir.
    private static final float RECEIVE_DURATION = 0.28f;

    private float kickTimer = 0f;
    private float receiveTimer = 0f;
    private ReceiveType receiveType = ReceiveType.NONE;

    public Player(String name, int number, float x, float y,
                  float efecto, float precision, float potencia, float recepcion) {
        this.name = name;
        this.number = number;
        this.x = x;
        this.y = y;
        this.efecto = efecto;
        this.precision = precision;
        this.potencia = potencia;
        this.recepcion = recepcion;
    }

    public float getReceptionRadius() {
        return 28f + recepcion * 0.32f;
    }

    // ------------------------------------------------------------
    // Llamar cuando este jugador patea la pelota.
    // ------------------------------------------------------------
    public void startKick() {
        kickTimer = KICK_DURATION;
    }

    // ------------------------------------------------------------
    // Llamar cuando este jugador recibe la pelota.
    // ------------------------------------------------------------
    public void startReceive(ReceiveType type) {
        receiveTimer = RECEIVE_DURATION;
        receiveType = type;
    }

    // ------------------------------------------------------------
    // Actualizar timers. Llamar una vez por frame.
    // ------------------------------------------------------------
    public void update(float delta) {
        if (kickTimer > 0) {
            kickTimer -= delta;
            if (kickTimer < 0) kickTimer = 0;
        }
        if (receiveTimer > 0) {
            receiveTimer -= delta;
            if (receiveTimer < 0) {
                receiveTimer = 0;
                receiveType = ReceiveType.NONE;
            }
        }
    }

    public boolean isKicking() {
        return kickTimer > 0;
    }

    // 1 = recién empezó la patada, 0 = terminó.
    // Sirve para animar el ángulo de la pierna progresivamente.
    public float getKickProgress() {
        return 1f - (kickTimer / KICK_DURATION);
    }

    public boolean isReceiving() {
        return receiveTimer > 0;
    }

    public ReceiveType getReceiveType() {
        return receiveType;
    }

    public float getReceiveProgress() {
        return 1f - (receiveTimer / RECEIVE_DURATION);
    }

    // ------------------------------------------------------------
    // Posición del pie donde debería aparecer/salir la pelota.
    // ------------------------------------------------------------
    public float getFootX() {
        return x + 14f;
    }

    public float getFootY() {
        return y - 17f;
    }
}
