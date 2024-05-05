package test.yaw;

import org.joml.Vector3f;
import yaw.engine.SceneRenderer;
import yaw.engine.UpdateCallback;
import yaw.engine.World;
import yaw.engine.geom.Geometry;
import yaw.engine.items.ItemObject;
import yaw.engine.light.DirectionalLight;
import yaw.engine.light.LightModel;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.MaterialADS;
import yaw.engine.mesh.Mesh;
import yaw.engine.mesh.DeprecatedMeshBuilder;
import yaw.engine.mesh.strategy.DefaultDrawingStrategy;
import yaw.engine.util.LoggerYAW;

/**
 * The objective of this exemple is to show the lights behaviour, with mixed color, testing the positionning of different lights
 */
public class TreeTest implements UpdateCallback {

    private World world;

    private ItemObject tree;
    private ItemObject floor;
    private float speed = 10;

    private Vector3f center = new Vector3f(0,0,0);

    public TreeTest() {

        LoggerYAW.getInstance().activateConsoleMode();

        world = new World(0, 0, 800, 600);
        world.installScene(new SceneRenderer(new LightModel()));

        world.getCamera().lookAt(new Vector3f(-1,2,3).add(center), new Vector3f(0,1f,0).add(center), new Vector3f(0,1,0));

        world.getSceneLight().setDirectionalLight(new DirectionalLight(new Vector3f(1,1,1), 0.7f, new Vector3f(-1,-1,-1)));

        //ShadowMap shadow = new ShadowMap();
        //shadow.setBias(0.0005f);
        //world.getSceneLight().getSun().setShadowMap(shadow);

        world.getSceneLight().getAmbientLight().setIntensity(0.3f);

        Mesh treem = generateTreeMesh(5, 2, 0.5);

        Mesh floorm = DeprecatedMeshBuilder.generateBlock(10, 0.1f, 10);

        tree = world.createItemObject("tree", center.x, center.y, center.z, 1.0f, treem);
        floor = world.createItemObject("floor", center.x, center.y, center.z, 1.0f, floorm);
        floor.setCastShadows(false);

        world.registerUpdateCallback(this);

    }

    public void run() {
        world.launchSync();
    }

    public void update(double deltaTime) {

        world.getCamera().getCameraMat().rotateLocalY((float) (deltaTime));

    }

    public static void set_3d(float[] b, int index, float x, float y, float z) {
        b[index*3] = x;
        b[index*3+1] = y;
        b[index*3+2] = z;
    }

    private interface ConeOperator {
        void cone(float bh, float r, int n);
    }

    public static Mesh generateTreeMesh(int layers, float size, double rand) {

        int[] is = new int[100000];
        float[] vs = new float[100000];

        BoxInt ii = new BoxInt(0);
        BoxInt vi = new BoxInt(0);

        final float s = size/4.f;
        final float h = size;

        ConeOperator cone = (float bh, float r, int n) -> {

            int top = vi.v;
            float height = Math.min(h, bh + h*0.3f);
            if(bh == 0) height = h;
            set_3d(vs, vi.v++, 0, height, 0);

            for(int i = 0; i<n; i++) {
                double angle = Math.PI*2 * i / n;
                double pair = i%2*2.0-1.0;
                float x = (float) (r*s*Math.cos(angle)*(1 + (Math.random()*2-1)*0.1f*rand*pair));
                float y = (float) (bh + Math.random()*0.05f*rand*pair);
                float z = (float) (r*s*Math.sin(angle)*(1 + (Math.random()*2-1)*0.1f*rand*pair));
                set_3d(vs, vi.v+i, x, y, z);
                is[ii.v+i*3+1] = vi.v+i;
                is[ii.v+i*3] = vi.v+((i+1) % n);
                is[ii.v+i*3+2] = top;
            }
            ii.v += n*3;
            vi.v += n;
        };


        cone.cone(0f, 0.3f, 20);
        float bh = h*0.90f;
        float r = 0.3f;
        for(int i = 0; i<layers; i++) {
            bh -= h/layers*0.6f + (Math.random()*2-1)*0.1f*rand;
            r += size/layers*0.4f + (Math.random()*2-1)*0.1f*rand;
            int n = (int) Math.floor((Math.random()*2-1)*10*rand + 20);
            cone.cone(bh, r, n);
        }

        // System.out.println(ii.v + " " + vi.v*3);

        int[] nis = new int[ii.v];
        float[] nvs = new float[vi.v*3];

        System.arraycopy(is, 0, nis, 0, nis.length);
        System.arraycopy(vs, 0, nvs, 0, nvs.length);

        Mesh treem = new Mesh(new Geometry(nvs, null, null, nis));
        treem.setDrawingStrategy(new DefaultDrawingStrategy());
        Material mat = new MaterialADS(new Vector3f(0,1,0));
        treem.setMaterial(mat);

        return treem;
    }

    public static void main(String[] args) {

        TreeTest test = new TreeTest();

        test.run();

    }

}

/* package */ class BoxInt {
    int v;
    public BoxInt(int v) { this.v = v; }
}