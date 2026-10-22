package com.gudos.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CameraMotionTest {
    @Test void everyCameraSweepsAutomaticallyWithinThePanorama() {
        for (int camera = 1; camera <= 6; camera++) {
            Scene scene = Scene.camera(camera);
            assertNotEquals(CameraMotion.pan(scene, 0), CameraMotion.pan(scene, 7), 0.01);
            for (int second = 0; second < 360; second++) {
                double pan = CameraMotion.pan(scene, second);
                assertTrue(pan >= 0.039 && pan <= 0.961);
            }
        }
    }

    @Test void camerasHaveIndependentPhasesAndPeriods() {
        assertNotEquals(CameraMotion.pan(Scene.CAM_01, 0), CameraMotion.pan(Scene.CAM_02, 0));
        assertEquals(CameraMotion.pan(Scene.CAM_01, 0), CameraMotion.pan(Scene.CAM_01, 24.3), 0.00001);
        assertThrows(IllegalArgumentException.class, () -> CameraMotion.pan(Scene.OFFICE, 0));
    }
}
