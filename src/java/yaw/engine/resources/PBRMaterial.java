package yaw.engine.resources;

import org.joml.Vector3f;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.Texture;

public class PBRMaterial {

    private String name;
    public String basecolor;
    public float metallic;
    public float roughness;
    public boolean isMetal;

    // gtlf file textures
    public String metallicRoughnessTexture;
    public String normalTexture;
    public String emissiveTexture;

    public PBRMaterial(String name) {
        this.name = name;
    }

    public PBRMaterial(){}

    public String toString() {
        return "PBRMaterial{" +
                "name='" + name + '\'' +
                ", basecolor='" + basecolor + '\'' +
                ", metallic=" + metallic +
                ", roughness=" + roughness +
                ", isMetal=" + isMetal +
                ", metallicRoughnessTexture='" + metallicRoughnessTexture + '\'' +
                ", normalTexture='" + normalTexture + '\'' +
                ", emissiveTexture='" + emissiveTexture + '\'' +
                '}';
    }

    public Material getMaterial(boolean withShadows) {
        Vector3f baseColor = new Vector3f(1.0f, 1.0f, 1.0f);
        return new Material(baseColor,
                withShadows,
                metallic,
                roughness,
                isMetal,
                metallicRoughnessTexture == null ? null : new Texture("/resources/" + metallicRoughnessTexture),
                normalTexture == null ? null : new Texture("/resources/" + normalTexture),
                emissiveTexture == null ? null : new Texture("/resources/" + emissiveTexture)
                );
    }

}
