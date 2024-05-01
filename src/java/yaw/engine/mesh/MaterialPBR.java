package yaw.engine.mesh;

public class MaterialPBR {

    private String name;
    private Texture basecolor;
    private float metallic;
    private float roughness;
    private boolean isMetal;

    // gtlf file textures
    private Texture metallicRoughnessTexture;
    private Texture normalTexture;
    private Texture emissiveTexture;

    public MaterialPBR(String name) {
        this.name = name;
    }

    public MaterialPBR(){}

    private Texture getColor() { return basecolor; }

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

    public Texture getMetallicRoughnessTexture() {
        return metallicRoughnessTexture;
    }

    public Texture getNormalTexture() {
        return normalTexture;
    }

    public Texture getEmissiveTexture() {
        return emissiveTexture;
    }

    public void setBasecolor(Texture basecolor) {
        this.basecolor = basecolor;
    }

    public void setMetallicRoughnessTexture(Texture metallicRoughnessTexture) {
        this.metallicRoughnessTexture = metallicRoughnessTexture;
    }

    public void setNormalTexture(Texture normalTexture) {
        this.normalTexture = normalTexture;
    }

    public void setEmissiveTexture(Texture emissiveTexture) {
        this.emissiveTexture = emissiveTexture;
    }

    @Override
    public String toString() {
        return "PbrMaterial{" +
                ", basecolor=" + basecolor +
                ", metallic=" + metallic +
                ", roughness=" + roughness +
                ", isMetal=" + isMetal +
                '}';
    }

    public Object isTextured() {
        return basecolor != null;
    }

    public Texture getTexture() {
        return basecolor;
    }
}
