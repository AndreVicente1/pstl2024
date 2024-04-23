package yaw.engine.resources;

import org.joml.Vector3f;
import yaw.engine.mesh.Texture;

public class PbrMaterial extends MtlMaterial {
    private Vector3f color;
    private float metallic;
    private float roughness;
    private boolean isMetal;

    public PbrMaterial(String name) {
        super(name);
    }

    public float getMetallic() {
        return metallic;
    }

    public void setMetallic(float metallic) {
        this.metallic = metallic;
    }

    public float getRoughness() {
        return roughness;
    }

    public void setRoughness(float roughness) {
        this.roughness = roughness;
    }

    @Override
    public String toString() {
        return "PbrMaterial{" +
                ", metallic=" + metallic +
                ", roughness=" + roughness +
                ", isMetal=" + isMetal +
                '}';
    }
}