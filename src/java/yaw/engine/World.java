package yaw.engine;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import yaw.engine.camera.Camera;
import yaw.engine.geom.Geometry;
import yaw.engine.items.HitBox;
import yaw.engine.items.ItemGroup;
import yaw.engine.items.ItemObject;
import yaw.engine.light.LightModel;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.MaterialADS;
import yaw.engine.mesh.Mesh;
import yaw.engine.mesh.Texture;
import yaw.engine.mesh.strategy.DefaultDrawingStrategy;
import yaw.engine.skybox.Skybox;

import java.util.Vector;

/**
 * This is the facade of the engine, most Clojure calls are
 * made on an instance of this object. The stateful part
 * is delegated to the underlying MainLoop.
 *
 */
public class World  {
    private GameLoop gameLoop;
    private Thread mloopThread = null;
    private boolean isRunning = false;
    private boolean sceneInstalled = false;

    /**
     * Initializes the elements to create the window
     *
     * @param pInitX      initX
     * @param pInitY      initY
     * @param pInitWidth  initWidth
     * @param pInitHeight initHeight
     * @param pInitVSYNC  initVSYNC
     */
    public World(int pInitX, int pInitY, int pInitWidth, int pInitHeight, boolean pInitVSYNC) {
        this.gameLoop = new GameLoop(pInitX, pInitY, pInitWidth, pInitHeight, pInitVSYNC);
    }

    /**
     * Initializes the elements to create the window
     *
     * @param pInitX      initX
     * @param pInitY      initY
     * @param pInitWidth  initWidth
     * @param pInitHeight initHeight
     */
    public World(int pInitX, int pInitY, int pInitWidth, int pInitHeight) {
        this.gameLoop = new GameLoop(pInitX, pInitY, pInitWidth, pInitHeight);
    }

    public World() {
        this.gameLoop = new GameLoop();
    }

    public void launchAsync() {
        if (!sceneInstalled) {
            throw new Error("Scene is not installed, cannot launch.");
        }
        if(isRunning) {
            throw new Error("World is already running (multiple calls to launch...() method)");
        }
        isRunning = true;
        mloopThread = new Thread(gameLoop);
        mloopThread.start();
    }

    public void launchWithContinuation(Runnable cont) {
        if (!sceneInstalled) {
            throw new Error("Scene is not installed, cannot launch.");
        }
        if(isRunning) {
            throw new Error("World is already running (multiple calls to launch...() method)");
        }
        isRunning = true;
        mloopThread = Thread.currentThread();
        Thread contThread = new Thread(cont);
        contThread.start();
        gameLoop.run();
    }

    public void launchSync() {
        if (!sceneInstalled) {
            throw new Error("Scene is not installed, cannot launch.");
        }
        if(isRunning) {
            throw new Error("World is already running (multiple calls to launch...() method)");
        }
        isRunning = true;
        mloopThread = Thread.currentThread();
        gameLoop.run();
    }

    /**
     * Input of critical section, allows to protect the resource share loop.
     * Stop the game loop and stop the thread that manage the world.
     */
    public void terminate() throws InterruptedException {
        gameLoop.close();
    }

    public void waitFortermination() {
        try {
            mloopThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void installScene(SceneRenderer sceneRenderer) {
        gameLoop.installScene(sceneRenderer);
        sceneInstalled = true;
    }

    /**
     * Create an item with the specified parameters and add it to the  world
     *
     * @param id        id
     * @param x         x coordinate
     * @param y         y coordinate
     * @param z         z coordinate
     * @param pScale    scale
     * @param pMesh     mesh
     * @return the item
     */
    public ItemObject createItemObject(String id, float x, float y, float z, float pScale, Mesh pMesh) {
        ItemObject lItem = new ItemObject(id, new Vector3f(x, y, z)
                , new Quaternionf(), pScale, pMesh);

        gameLoop.addToScene(lItem);
        return lItem;
    }

    /**
     * Create a mesh with the specified parameters
     * MeshOld won't be load into the grahpic cards unless you bind it to an item
     *
     * @param pVertices    vetices
     * @param pTextCoords  texture coordonates
     * @param pNormals     normals
     * @param pIndices     indices
     * @param pWeight      weight
     * @param rgb          rgb array
     * @param pTextureName texture path
     * @return the mesh
     */
    public Mesh createMesh(float[] pVertices, float[] pTextCoords, float[] pNormals, int[] pIndices, int pWeight, float[] rgb, String pTextureName) {
        if (rgb.length != 3) {
            throw new RuntimeException("RGB must represent 3 colors");
        }
        System.out.println("pTextureName " + pTextureName);
        Vector3f lMaterialColor = new Vector3f(rgb[0], rgb[1], rgb[2]);
        Material lMaterial = new MaterialADS(lMaterialColor);
        
        //Texture part
        if (pTextureName != null && !(pTextureName.matches("Material.*") || pTextureName.isEmpty())) {
            Texture lTexture = gameLoop.fetchTexture(pTextureName);
            if (lTexture == null) {
                lTexture = new Texture(pTextureName);
            } else {
                System.err.println("[Warning] Texture not found: " + pTextureName);
            }
            lMaterial.setTexture(lTexture);
        }
        Geometry geom = new Geometry(pVertices, pTextCoords, pNormals, pIndices);
        Mesh lMesh = new Mesh(geom, lMaterial);
        return lMesh;
    }

    /**
     * Create a mesh with the specified parameters
     * MeshOld won't be load into the grahpic cards unless you bind it to an item
     *
     * @param pVertices    vetices
     * @param pNormals     normals
     * @param pIndices     indices
     * @return the mesh
     */
    public Mesh createMesh(float[] pVertices, float[] pNormals, int[] pIndices, float[] rgb) {
        if (rgb.length != 3) {
            throw new RuntimeException("RGB must represent 3 colors");
        }
        Vector3f lMaterialColor = new Vector3f(rgb[0], rgb[1], rgb[2]);
        Material lMaterial = new MaterialADS(lMaterialColor);
        Mesh lMesh = new Mesh(new Geometry(pVertices, pNormals, pIndices), lMaterial);
        return lMesh;
    }



    /**
     * Create a bounding box with the specified parameters and add it to the  world
     *
     * @param id        id
     * @param x         x coordinate
     * @param y         y coordinate
     * @param z         z coordinate
     * @param pScale    scale
     * @return BoundingBox
     */
    public HitBox createHitBox(String id, float x, float y, float z, float pScale, float xLength, float yLength, float zLength, boolean isVisible){
        HitBox hb = new HitBox(id, new Vector3f(x, y, z), new Quaternionf(), pScale, xLength, yLength, zLength, isVisible);
        gameLoop.addToScene(hb);
        return hb;

    }

    /**
     * Remove the specified item from the world
     *
     * @param pItem the item
     */
    public void removeItem(ItemObject pItem) {
        gameLoop.removeFromScene(pItem);
    }

    public boolean isInCollision(HitBox hb1, HitBox hb2) {
        return hb1.collidesWith(hb2);
    }

    public void setBackgroundColor(float red, float green, float blue) {
        gameLoop.setBackgroundColor(red, green, blue);
    }

    public void registerUpdateCallback(UpdateCallback cb) {
        gameLoop.registerUpdateCallback(cb);
    }

    public void registerInputCallback(InputCallback callback) {
        gameLoop.registerInputCallback(callback);
    }

    //3D click
    public void registerMouseCallback(Mouse3DClickCallBack callback) {
        gameLoop.registerMouse3DClickCallBack(callback);
    }




    /**
     * Gets an empty camera list
     */
    public void clearCameras() {
        gameLoop.clearCameras();
    }

    /**
     * Adds a camera and its index in the list of cameras (not in live)
     *
     * @param pIndex  index
     * @param pCamera camera
     */
    public void addCamera(int pIndex, Camera pCamera) {
        gameLoop.addCamera(pIndex, pCamera);
    }

    public Camera getCamera() {
        return gameLoop.getCamera();
    }

    public void setCamera(Camera pCamera) {
        gameLoop.setCamera(pCamera);
    }

    public Vector<Camera> getCamerasList() {
        return gameLoop.getCamerasList();
    }

    /**
     * Create a group of item
     *
     * @return group of item
     */
    public ItemGroup createGroup(String id) {
        return gameLoop.createGroup(id);
    }

    /**
     * Remove a group of item from the world
     *
     * @param pGroup the specified group
     */
    public void removeGroup(ItemGroup pGroup) {
        gameLoop.removeGroup(pGroup);
    }

    public Vector<ItemGroup> getItemGroupArrayList() {
        return gameLoop.getItemGroupArrayList();
    }

    public LightModel getSceneLight() {
        if (!sceneInstalled) {
            throw new Error("Scene is not installed");
        }
        return gameLoop.getSceneLight();
    }

    /**
     * Create a skybox with all its parameters and replace the current skybox
     *
     * @param pWidth  width
     * @param pLength length
     * @param pHeight height
     * @param pR      r
     * @param pG      g
     * @param pB      b
     */
    public void setSkybox(float pWidth, float pLength, float pHeight, float pR, float pG, float pB) {
        Skybox lSkybox = new Skybox(pWidth, pLength, pHeight, new Vector3f(pR, pG, pB));
        setSkybox(lSkybox);
    }

    public void removeSkybox() {
        gameLoop.removeSkybox();
    }

    public Skybox getSkybox() {
        return gameLoop.getSkybox();
    }

    /**
     * Allows to change the skybox by the new Skybox passed in arguments.
     * If a skybox already exists, it's added to the list of mSkyboxToBeRemoved
     *
     * @param pSkybox skybox
     */
    public void setSkybox(Skybox pSkybox) {
        gameLoop.setSkybox(pSkybox);
    }

}