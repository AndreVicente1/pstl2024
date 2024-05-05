package yaw.engine.mesh;

import org.joml.Vector3f;

public class MaterialPBR extends Material{
    private float metallic;
    private float roughness;
    private boolean isMetal;
    private float occlusionStrength;
    private Texture occlusionTexture;
    private Texture metallicRoughnessTexture;
    private Texture normalTexture;
    private Texture emissiveTexture;

    public MaterialPBR(Vector3f baseColor, boolean withShadows, float metallic, float roughness, boolean isMetal, float occlusion, Texture baseColorTexture, Texture metallicRoughnessTexture, Texture normalTexture, Texture emissiveTexture, Texture occlusionTexture) {
        super(baseColor,baseColorTexture,withShadows);
        this.metallic = metallic;
        this.roughness = roughness;
        this.isMetal = isMetal;
        this.occlusionStrength = occlusion;
        this.metallicRoughnessTexture = metallicRoughnessTexture;
        this.normalTexture = normalTexture;
        this.emissiveTexture = emissiveTexture;
        this.occlusionTexture = occlusionTexture;

    }

    public boolean hasMetallicRoughnessTexture() {return metallicRoughnessTexture != null;}

    public boolean hasPbrNormalTexture() { return normalTexture!= null;}

    public boolean hasEmissiveTexture() { return emissiveTexture != null;}

    public boolean hasOcclusionTexture() { return occlusionTexture != null;}

    public float getMetallic() {return metallic;}

    public float getRoughness() {return roughness;}

    public Texture getMetallicRoughnessTexture() {
        return metallicRoughnessTexture;
    }

    public Texture getPbrNormalTexture() {
        return normalTexture;
    }

    public Texture getEmissiveTexture() {
        return emissiveTexture;
    }

    public boolean getIsMetal() { return isMetal; }

    public float getOcclusion() { return occlusionStrength;}
    public Texture getOcclusionTexture() { return occlusionTexture; }
}
