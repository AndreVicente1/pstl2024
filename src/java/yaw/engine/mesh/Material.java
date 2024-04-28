package yaw.engine.mesh;

import org.joml.Vector3f;

/**
 * More complex material classes:
 * ColorMapping, TextMapping, ProceduralMapping (material generator with reuse of images)
 */

public class Material {
    private Vector3f baseColor;
    private Texture texture;
    private Vector3f ambient;
    private Vector3f emissive;
    private float emissiveAmount;
    private Vector3f diffuse;
    private Vector3f specular;
    private float shineness;

    public final boolean withShadows;

    //RGB vector
    private Vector3f mColor;

    private Texture map_Kd;
    private Texture map_Bump;
    private Texture map_Ns;
    private float opacity;

    public Material(Vector3f baseColor, Vector3f ambient, Vector3f emissive, float emissiveAmount, Vector3f diffuse, Vector3f specular, float shineness, boolean withShadows) {
        this.baseColor = baseColor;
        texture = null;
        this.ambient = ambient;
        this.emissive = emissive;
        this.emissiveAmount = emissiveAmount;
        this.diffuse = diffuse;
        this.specular = specular;
        this.shineness = shineness;
        this.withShadows = withShadows;
    }

    public Material(Vector3f baseColor) {
        this(baseColor, new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(0, 0, 0), 0,
                new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(1.0f, 1.0f, 1.0f), 32, false);
    }

    public Material() {
        this(new Vector3f(1.0f, 1.0f, 1.0f));
    }

    public Material(Texture texture, Vector3f ambient, Vector3f emissive, float emissiveAmount, Vector3f diffuse, Vector3f specular, float shineness, boolean withShadows) {
        this.texture = texture;
        this.ambient = ambient;
        this.emissive = emissive;
        this.emissiveAmount = emissiveAmount;
        this.diffuse = diffuse;
        this.specular = specular;
        this.shineness = shineness;
        this.withShadows = withShadows;
    }

    public Material(Vector3f baseColor, Vector3f ambient, Vector3f emissive, float emissiveAmount, Vector3f diffuse, Vector3f specular, float shineness, boolean withShadows, Texture map_Kd, Texture map_Bump, Texture map_Ns, float opacity) {
        this.baseColor = baseColor;
        this.ambient = ambient;
        this.emissive = emissive;
        this.emissiveAmount = emissiveAmount;
        this.diffuse = diffuse;
        this.specular = specular;
        this.shineness = shineness;
        this.withShadows = withShadows;

        this.texture = map_Kd;
        this.map_Bump = map_Bump;
        this.map_Ns = map_Ns;
        this.opacity = opacity;
    }

    public Vector3f getBaseColor() {
        return baseColor;
    }

    public void setBaseColor(Vector3f baseColor) {
        this.baseColor = baseColor;
    }

    public boolean isTextured() {
        return texture != null;
    }

    public Vector3f getAmbientColor() {
        return ambient;
    }

    public Vector3f getEmissiveColor() {
        return emissive;
    }

    public float getEmissiveAmount() {
        return emissiveAmount;
    }

    public Vector3f getDiffuseColor() {
        return diffuse;
    }

    public Vector3f getSpecularColor() {
        return specular;
    }

    public float getShineness() {
        return shineness;
    }


    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public Texture getSpecularTexture(){ return map_Ns; }

    public Texture getNormalTexture(){ return map_Bump; }

    public boolean hasNormalMap() { return map_Bump != null; }

    public boolean hasSpecularMap() { return map_Ns != null; }

}