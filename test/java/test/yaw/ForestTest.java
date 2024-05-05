package test.yaw;

import org.joml.Vector3f;
import yaw.engine.SceneRenderer;
import yaw.engine.UpdateCallback;
import yaw.engine.World;
import yaw.engine.items.ItemObject;
import yaw.engine.light.DirectionalLight;
import yaw.engine.light.LightModel;
import yaw.engine.light.ShadowMap;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.MaterialADS;
import yaw.engine.mesh.Mesh;
import yaw.engine.mesh.builder.Rectangle;
import yaw.engine.util.LoggerYAW;

/**
 * The objective of this exemple is to show the lights behaviour, with mixed color, testing the positionning of different lights
 */
public class ForestTest implements UpdateCallback {

    private World world;

    private ItemObject floor;
    private float speed = 10;

    private Vector3f center = new Vector3f(0,0,0);

    public ForestTest() {

        LoggerYAW.getInstance().activateConsoleMode();

        world = new World(0, 0, 800, 600);
        world.installScene(new SceneRenderer(new LightModel()));
        world.getCamera().lookAt(new Vector3f(-1,3,3).add(center), new Vector3f(0,1f,0).add(center), new Vector3f(0,1,0));

        world.getSceneLight().setDirectionalLight(new DirectionalLight(new Vector3f(1,1,1), 0.7f, new Vector3f(-1,-1,-1)));

        ShadowMap shadow = new ShadowMap(10000, 10000);
        shadow.setBias(0.01f);
        world.getSceneLight().getDirectionalLight().setShadowMap(shadow);

        world.getSceneLight().getAmbientLight().setIntensity(0.3f);

        Mesh floorm = new Rectangle(20).generate();
        Material ground = new MaterialADS();
        ground.setBaseColor(new Vector3f(0.6078f, 0.4627f, 0.3255f));
        floorm.setMaterial(ground);

        floor = world.createItemObject("floor", center.x, center.y, center.z, 1.0f, floorm);
        floor.rotateX(-3.14159265f / 2);
        floor.setCastShadows(false);

        generateForest(world, 100, 0.6);

        world.registerUpdateCallback(this);

    }

    public static void generateForest(World world, int size, double rand) {

        int side = (int) Math.floor(Math.sqrt(size));

        for(int i = 0; i<size; i++) {

            int x = i/side;
            int z = i%side;

            int layers = 4 + (int) (Math.random() * 4);
            Mesh treem = TreeTest.generateTreeMesh(layers, (float) (Math.random()*1.5+1.5), rand);

            // Grid
            Vector3f pos = new Vector3f(x, 0, z).sub(new Vector3f(side/2.f)).mul(2.0f);

            // Random
            pos.add(new Vector3f((float)Math.random(), 0, (float)Math.random()).sub(new Vector3f(0.5f)));

            ItemObject tree = world.createItemObject("tree", pos.x, 0, pos.z, 1.0f, treem);

        }
    }

    public void run() {
        world.launchSync();
    }

    public void update(double deltaTime) {

        world.getCamera().getCameraMat().rotateLocalY((float) (deltaTime));

    }

    public static void main(String[] args) {

        ForestTest test = new ForestTest();

        test.run();

    }

}
