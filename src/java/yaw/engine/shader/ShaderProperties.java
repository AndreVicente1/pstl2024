package yaw.engine.shader;

import java.util.Objects;

public class ShaderProperties {
    // scene-specific properties
    public final boolean hasDirectionalLight;
    public final int maxPointLights;
    public final int maxSpotLights;

    // mesh-specific properties
    public final boolean hasTexture;

    public final boolean withShadows;

    public final boolean hasSpecularMap;

    public ShaderProperties(boolean hasDirectionalLight, int maxPointLights, int maxSpotLights, boolean hasTexture, boolean withShadows, boolean hasSpecularMap) {
        this.hasDirectionalLight = hasDirectionalLight;
        this.maxPointLights = maxPointLights;
        this.maxSpotLights = maxSpotLights;
        this.hasTexture = hasTexture;
        this.withShadows = withShadows;
        this.hasSpecularMap = hasSpecularMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShaderProperties that = (ShaderProperties) o;
        return hasDirectionalLight == that.hasDirectionalLight && maxPointLights == that.maxPointLights && maxSpotLights == that.maxSpotLights && hasTexture == that.hasTexture && withShadows == that.withShadows;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hasDirectionalLight, maxPointLights, maxSpotLights, hasTexture, withShadows);
    }
}
