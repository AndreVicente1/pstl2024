package yaw.engine.resources;

import org.joml.Vector3f;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.MaterialPBR;
import yaw.engine.mesh.Texture;

public class GltfMaterial {

    private String name;
    public Vector3f basecolor;
    public float metallic;
    public float roughness;
    public float occlusionStrength;
    public boolean isMetal;

    // gtlf file textures
    public String metallicRoughnessTexture;
    public String normalTexture;
    public String emissiveTexture;
    public String basecolorTexture;
    public String occlusionTexture;


    public GltfMaterial(String name) {
        this.name = name;
    }

    public GltfMaterial(){}

    public String toString() {
        return "GltfMaterial{" +
                "name='" + name + '\'' +
                ", metallic=" + metallic +
                ", roughness=" + roughness +
                ", isMetal=" + isMetal +
                ", basecolor='" + basecolorTexture + '\'' +
                ", metallicRoughnessTexture='" + metallicRoughnessTexture + '\'' +
                ", normalTexture='" + normalTexture + '\'' +
                ", emissiveTexture='" + emissiveTexture + '\'' +
                ", occlusionTexture='" + occlusionTexture + '\'' +
                '}';
    }

    public Material getMaterial(boolean withShadows) {
        return new MaterialPBR(basecolor,
                withShadows,
                metallic,
                roughness,
                isMetal,
                occlusionStrength,
                basecolorTexture == null ? null : new Texture("/resources/" + basecolorTexture),
                metallicRoughnessTexture == null ? null : new Texture("/resources/" + metallicRoughnessTexture),
                normalTexture == null ? null : new Texture("/resources/" + normalTexture),
                emissiveTexture == null ? null : new Texture("/resources/" + emissiveTexture),
                occlusionTexture == null ? null : new Texture("/resources/" + occlusionTexture)
        );
    }

}