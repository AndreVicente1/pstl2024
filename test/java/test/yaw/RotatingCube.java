package test.yaw;

import org.joml.Vector3f;
import yaw.engine.SceneRenderer;
import yaw.engine.UpdateCallback;
import yaw.engine.World;
import yaw.engine.items.ItemObject;
import yaw.engine.light.LightModel;
import yaw.engine.mesh.*;

/**
 * Basic example of a cube rotating on y axis
 */
public class RotatingCube implements UpdateCallback {
	private int nbUpdates = 0;
	private double totalDeltaTime = 0.0;
	private static long deltaRefreshMillis = 1000;
	private long prevDeltaRefreshMillis = 0;
	private ItemObject cube ;
	private float speed = 0.025f;
	
	public RotatingCube(ItemObject cube) {
		this.cube = cube;
	}
	
	public ItemObject getItem() {
		return cube;
	}

	
	@Override
	public void update(double deltaTime) {
		nbUpdates++;
		totalDeltaTime += deltaTime;
		
		long currentMillis = System.currentTimeMillis();
		if (currentMillis - prevDeltaRefreshMillis > deltaRefreshMillis) {
			double avgDeltaTime = totalDeltaTime / (double) nbUpdates;
			//System.out.println("Average deltaTime = " + Double.toString(avgDeltaTime) +" s ("+nbUpdates+")");
			nbUpdates = 0;
			totalDeltaTime = 0.0;
			prevDeltaRefreshMillis = currentMillis;
		}

		//cube.rotateXYZ(0f, 3.1415925f * speed * (float) deltaTime, 0f);
		//cube.rotateZAround(1f, new Vector3f(0f, 0f, -3f));

		float angle = 2.0f * 3.1415925f * (float) deltaTime * speed;
		System.out.println(deltaTime);
		cube.rotateZ(angle);
		//cube.rotateXYZAround(0f, 3.1415925f * speed * (float) deltaTime, 0f, new Vector3f(0f, 0f, -10f));
		//cube.rotateX(0.0f);


	}
	
	public static void main(String[] args) {

		World world = new World(0, 0, 800, 600);
		world.installScene(new SceneRenderer(new LightModel()));

		Mesh cubem = MeshExamples.makeDice(1);
		ItemObject cube = world.createItemObject("cube", 0f, 0f, -2f, 1.0f, cubem);
		cube.translate(2f,0f, -5f);

		RotatingCube rCube = new RotatingCube(cube);

		world.registerUpdateCallback(rCube);

		world.launchSync();
	}

}
