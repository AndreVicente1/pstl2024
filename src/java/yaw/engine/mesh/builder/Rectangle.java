package yaw.engine.mesh.builder;

import yaw.engine.geom.Geometry;
import yaw.engine.mesh.DeprecatedMeshBuilder;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.MaterialADS;
import yaw.engine.mesh.Mesh;
import yaw.engine.mesh.strategy.DefaultDrawingStrategy;

import java.util.Map;

/**
 * A builder class for Cuboid (block) meshes.
 */
public class Rectangle implements MeshBuilder {
    private final float xLength;
    private final float yLength;

  /**
   * Create a 2D Rectangle  generator with the specified lengths.
   * (Normals and indices are hard coded)
   *
   * @param xLength Length along X-axis
   * @param yLength Length along Y-axis
   */
    public Rectangle(float xLength, float yLength) {
        this.xLength = xLength;
        this.yLength = yLength;
    }

    /**
     * Create a Square generator of the specified side-length.
     * @param length
     */
    public Rectangle(float length) {
        this(length, length);
    }

    @Override
    public Mesh generate() {
        float x = xLength / 2f;
        float y = yLength / 2f;
        float z = 0;
        float[] vertices = new float[]{
                x, y, z, -x, y, z, -x, -y, z, x, -y, z};
        //for light
        float[] normals = { 0, 0, 1f, 0, 0, 1f, 0, 0, 1f, 0, 0, 1f};
        //mapping for the texture
        float[] textCoord = new float[]{
                0.0f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f,
                0.5f, 0.0f,

                0.0f, 0.0f,
                0.5f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f};

        //mapping the triangle for each face
        int[] indices = new int[] {0, 1, 2, 0, 2, 3};
        Mesh lMesh = new Mesh(new Geometry(vertices, textCoord, normals, indices), new MaterialADS());
        lMesh.setDrawingStrategy(new DefaultDrawingStrategy());
        Map<String, String> lOptionalAttributes = DeprecatedMeshBuilder.getPositionAttributesMap(xLength, yLength, 0);
        lMesh.putOptionalAttributes(lOptionalAttributes);
        return lMesh;
    }
}
