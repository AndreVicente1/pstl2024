package yaw.engine.mesh.builder;

import org.joml.Vector3f;
import yaw.engine.geom.GeomLib;
import yaw.engine.geom.Geometry;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.MaterialADS;
import yaw.engine.mesh.Mesh;
import yaw.engine.mesh.strategy.DefaultDrawingStrategy;

/**
 * A builder class for Cuboid (block) meshes.
 */
public class Cuboid implements MeshBuilder {
    private final float xLength;
    private final float yLength;
    private final float zLength;

  /**
   * Create a Cuboid (block) generator with the specified material , width, length and height.
   * Create vertices based on the specified lengths.
   * (Normals and indices are hard coded)
   *
   * @param xLength Length along X-axis
   * @param yLength Length along Y-axis
   * @param zLength Length along Z-axis
   */
    public Cuboid(float xLength, float yLength, float zLength) {
        this.xLength = xLength;
        this.yLength = yLength;
        this.zLength = zLength;
    }

    /**
     * Create a Cube generator of the specified side-length.
     * @param length
     */
    public Cuboid(float length) {
        this(length, length, length);
    }

    @Override
    public Mesh generate() {
        float x = xLength / 2f;
        float y = yLength / 2f;
        float z = zLength / 2f;
        float[] vertices = new float[]{
                //Front face
                x, y, z, -x, y, z, -x, -y, z, x, -y, z,
                //Top face
                x, y, z, -x, y, z, x, y, -z, -x, y, -z,
                //Back face
                x, y, -z, -x, y, -z, -x, -y, -z, x, -y, -z,
                //Bottom face
                x, -y, z, -x, -y, z, x, -y, -z, -x, -y, -z,
                //Left face
                -x, y, z, -x, y, -z, -x, -y, z, -x, -y, -z,
                //Right face
                x, y, z, x, y, -z, x, -y, z, x, -y, -z};
        //for light
        float[] normals = {
                //Front face
                0, 0, 1f, 0, 0, 1f, 0, 0, 1f, 0, 0, 1f,
                //Top face
                0, 1f, 0, 0, 1f, 0, 0, 1f, 0, 0, 1f, 0,
                //Back face
                0, 0, -1f, 0, 0, -1f, 0, 0, -1f, 0, 0, -1f,
                //Bottom face
                0, -1f, 0, 0, -1f, 0, 0, -1f, 0, 0, -1f, 0,
                //Left face
                -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0,
                //Right face
                1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0};
        //mapping for the texture
        float[] textCoord2 = new float[]{
                0.0f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f,
                0.5f, 0.0f,

                0.0f, 0.0f,
                0.5f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f,

                // For text coords in top face
                0.0f, 0.5f,
                0.5f, 0.5f,
                0.0f, 1.0f,
                0.5f, 1.0f,

                // For text coords in right face
                0.0f, 0.0f,
                0.0f, 0.5f,

                // For text coords in left face
                0.5f, 0.0f,
                0.5f, 0.5f,

                // For text coords in bottom face
                0.5f, 0.0f,
                1.0f, 0.0f,
                0.5f, 0.5f,
                1.0f, 0.5f,
        };

        float[] textCoord = new float[]{
                0.5f,0,
                0.5f,0.25f,
                0.75f,0.25f,
                0.75f,0,

                0.25f,0.25f,
                0.25f,0.5f,
                0.5f,0.5f,
                0.5f,0.25f,

                0.5f,0.25f,
                0.5f,0.5f,
                0.75f,0.5f,
                0.75f,0.25f,

                0.75f,0.25f,
                0.75f,0.5f,
                1,0.5f,
                1,0.25f,

                0.5f,0.5f,
                0.5f,0.75f,
                0.75f,0.75f,
                0.75f,0.5f,

                0.5f,0.75f,
                0.5f,1,
                0.75f,1,
                0.75f,0.75f
        };

        //mapping the triangle for each face
        int[] indices = new int[]{
                //Front face
                0, 1, 2, 0, 2, 3,
                //Top face
                4, 6, 5, 6, 7, 5,
                //Back face
                8, 11, 10, 8, 10, 9,
                //Bottom Face
                14, 12, 13, 14, 13, 15,
                //Left face
                16, 19, 18, 16, 17, 19,
                //Right face
                20, 22, 21, 22, 23, 21};

        Geometry geom = GeomLib.makeCuboid(xLength, yLength, zLength).build();
        Mesh lMesh = new Mesh(geom, new MaterialADS(new Vector3f(1.0f, 1.0f, 1.0f)));
        lMesh.setDrawingStrategy(new DefaultDrawingStrategy());
        return lMesh;
    }
}
