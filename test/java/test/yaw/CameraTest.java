package test.yaw;

import org.joml.Vector3f;
import yaw.engine.SceneRenderer;
import yaw.engine.UpdateCallback;
import yaw.engine.World;
import yaw.engine.items.ItemObject;
import yaw.engine.light.LightModel;
import yaw.engine.light.SpotLight;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.MaterialADS;
import yaw.engine.mesh.Mesh;

/**
 * The objective of this exemple is to show the lights behaviour, with mixed color, testing the positionning of different lights
 */
public class CameraTest implements UpdateCallback {

    private World world;

    private ItemObject cube;
    private float speed = 10;

    public CameraTest() {

        world = new World(0, 0, 800, 600);
        world.installScene(new SceneRenderer(new LightModel()));
        world.getCamera().setPosition(0,0,5);

        //world.getSceneLight().getSpotTable()[0] = new SpotLight(0, 255, 0, 0, 0, 0, 1, 0, 0.5f, 0, 0, 0, -5, 10f);
        world.getSceneLight().addSpotLight(new SpotLight(0, 255, 0, 0.2f, 0f,0f, 1, 0, 0.75f, 0, 0, 0, -5, 3));
        world.getSceneLight().addSpotLight(new SpotLight(255, 0, 0, -0.2f, 0.0f, 0, 1f, 0, 0.75f, 0, 0f, 0, -5, 3f));
        //world.getSceneLight().setSun(new DirectionalLight());

        Mesh cubem = MeshExamples.makeDice(1);
        cubem.setMaterial(new MaterialADS(new Vector3f(0, 0, 7.0f)));
        cube = world.createItemObject("cube", 0f, 0, 0, 1.0f, cubem);

        world.registerUpdateCallback(this);

    }

    public void run() {
        world.launchSync();
    }

    public void update(double deltaTime) {
        cube.rotateY(3.1415925f * (speed / 100f) * (float) deltaTime);
        world.getCamera().rotateXYZ(0, 0, 3.1415925f * speed * (float) deltaTime);
    }

    public static void main(String[] args) {

        CameraTest test = new CameraTest();

        test.run();

    }

}
