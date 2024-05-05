package yaw.engine.mesh;

import org.joml.Vector3f;

/**
 * More complex material classes:
 * ColorMapping, TextMapping, ProceduralMapping (material generator with reuse of images)
 */

public abstract class Material {
    private Vector3f baseColor;
    private Texture texture;
    public final boolean withShadows;

    //RGB vector
    private Vector3f mColor;

    public Material(Vector3f baseColor, Texture texture, boolean withShadows){
        this.baseColor = baseColor;
        this.texture = texture;
        this.withShadows = withShadows;
    }

    public Material(Vector3f baseColor) {

        this(baseColor, null, false);
    }

    public Material() {
        this(new Vector3f(1.0f, 1.0f, 1.0f));
    }


    // polymorphism
    public Texture getSpecularTexture() {
        return null;
    }

    public Texture getNormalTexture() {
        return null;
    }

    public Texture getMetallicRoughnessTexture() {
        return null;
    }

    public Texture getPbrNormalTexture() {
        return null;
    }

    public Texture getEmissiveTexture() {
        return null;
    }

    public Texture getOcclusionTexture() {
        return null;
    }

    public Vector3f getEmissiveColor() {
        return null;
    }

    public float getEmissiveAmount() {
        return 0;
    }

    public Vector3f getDiffuseColor() {
        return null;
    }

    public Vector3f getSpecularColor() {
        return null;
    }

    public float getShineness() {
        return 0;
    }

    public boolean hasNormalMap() { return false; }

    public boolean hasSpecularMap() { return false; }

    public Vector3f getAmbientColor() {
        return null;
    }

    public float getMetallic() {return 0;}

    public float getRoughness() {return 0;}

    public boolean getIsMetal() { return false; }

    public float getOcclusion() {return 0;}

    public boolean hasMetallicRoughnessTexture() {return false;}

    public boolean hasPbrNormalTexture() { return false;}

    public boolean hasEmissiveTexture() { return false;}

    public boolean hasOcclusionTexture() {return false;}

    public Vector3f getBaseColor() {
        return baseColor;
    }

    public void setBaseColor(Vector3f baseColor) {
        this.baseColor = baseColor;
    }

    public boolean isTextured() {
        return texture != null;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

}