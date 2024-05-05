package yaw.engine.resources;

import org.joml.Vector3f;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.MaterialADS;
import yaw.engine.mesh.Texture;

import static org.joml.Math.clamp;

public class MtlMaterial {

    private String name;
    public float shineness = 8;

    public Vector3f ambient  = null;
    public Vector3f diffuse = null;
    public Vector3f specular = null;
    public Vector3f emissive = null;
    public float opacity = 0;

    public String map_Kd = null;
    public String map_Bump = null;
    public String map_Ns = null;

    public MtlMaterial(String name) {
        this.name = name;
    }

    public String toString() {
        String s = "MtlMaterial{" +
                "shininess=" + shineness +
                ", ambient=" + ambient +
                ", diffuse=" + diffuse +
                ", specular=" + specular +
                ", emissive=" + emissive +
                ", opacity=" + opacity +
                '}';
        return s;
    }

    public Material getMaterial(boolean withShadows) {
        // XXX : MTL has no notion of a "basic color"
        Vector3f baseColor = new Vector3f(1.0f, 1.0f, 1.0f);
        return new MaterialADS(baseColor,
                ambient == null ? baseColor : ambient,
                emissive == null ? new Vector3f(0, 0, 0) : emissive,
                emissive == null ? 0 : 1.0f,
                diffuse == null ? baseColor : diffuse,
                specular == null ? baseColor : specular,
                shineness,
                withShadows,
                map_Kd == null ? null : new Texture("/resources/" + map_Kd),
                map_Bump == null ? null : new Texture("/resources/" + map_Bump),
                map_Ns == null ? null : new Texture("/resources/" + map_Ns),
                opacity);
    }

    public Material getMaterial() {
        return getMaterial(false);
    }
}
