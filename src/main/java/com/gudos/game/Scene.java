package com.gudos.game;

public enum Scene {
    OFFICE("office", "Security office", 0),
    CAM_01("cam01", "Dining area", 1),
    CAM_02("cam02", "Serving counter", 2),
    CAM_03("cam03", "Kitchen", 3),
    CAM_04("cam04", "Stockroom", 4),
    CAM_05("cam05", "Side corridor", 5),
    CAM_06("cam06", "Back alley", 6);

    private final String id;
    private final String label;
    private final int camera;

    Scene(String id, String label, int camera) {
        this.id = id;
        this.label = label;
        this.camera = camera;
    }

    public String id() { return id; }
    public String label() { return label; }
    public int camera() { return camera; }

    public static Scene camera(int number) {
        if (number < 1 || number > 6) throw new IllegalArgumentException("Camera must be 1–6");
        return values()[number];
    }
}
