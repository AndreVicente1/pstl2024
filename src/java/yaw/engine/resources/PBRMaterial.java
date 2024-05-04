package yaw.engine.resources;

import org.joml.Vector3f;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.Texture;

public class PBRMaterial {

    private String name;
    public Vector3f basecolor;
    public float metallic;
    public float roughness;
    public boolean isMetal;

    // gtlf file textures
    public String metallicRoughnessTexture;
    public String normalTexture;
    public String emissiveTexture;
    public String basecolorTexture;



    public PBRMaterial(String name) {
        this.name = name;
    }

    public PBRMaterial(){}

    public String toString() {
        return "PBRMaterial{" +
                "name='" + name + '\'' +
                ", metallic=" + metallic +
                ", roughness=" + roughness +
                ", isMetal=" + isMetal +
                ", basecolor='" + basecolorTexture + '\'' +
                ", metallicRoughnessTexture='" + metallicRoughnessTexture + '\'' +
                ", normalTexture='" + normalTexture + '\'' +
                ", emissiveTexture='" + emissiveTexture + '\'' +
                '}';
    }

    public Material getMaterial(boolean withShadows) {
        return new Material(basecolor,
                withShadows,
                metallic,
                roughness,
                isMetal,
                basecolorTexture == null ? null : new Texture("/resources/" + basecolorTexture),
                metallicRoughnessTexture == null ? null : new Texture("/resources/" + metallicRoughnessTexture),
                normalTexture == null ? null : new Texture("/resources/" + normalTexture),
                emissiveTexture == null ? null : new Texture("/resources/" + emissiveTexture)
        );
    }

}