package yaw.engine;

import org.joml.Vector3f;
import yaw.engine.camera.Camera;
import yaw.engine.helper.HelperAxesShaders;
import yaw.engine.helper.HelperNormalsShaders;
import yaw.engine.helper.HelperVerticesShaders;
import yaw.engine.items.ItemGroup;
import yaw.engine.items.ItemObject;
import yaw.engine.light.LightModel;
import yaw.engine.mesh.Texture;
import yaw.engine.shader.ShaderManager;
import yaw.engine.shader.ShaderProgram;
import yaw.engine.skybox.Skybox;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.glClearColor;

/**
 * This the main loop controlled by the World facade.
 *
 * */
public class GameLoop implements Runnable {
    private SceneRenderer mSceneRenderer;
    private final Vector<Skybox> mSkyboxToBeRemoved;
    private Camera mCamera;
    private Vector<Camera> mCamerasList;
    private Renderer mRenderer;
    private Vector<ItemGroup> mItemGroupArrayList;
    private Skybox mSkybox = null;
    private ConcurrentHashMap<String, Texture> mStringTextureConcurrentHashMap;
    private boolean mLoop;
    private int initX, initY, initWidth, initHeight;
    private boolean initVSYNC;
    private ShaderManager shaderManager;

    private float bgndBlue;
    private float bgndGreen;
    private float bgndRed;

    //3D click

    private RayCaster rayCaster= new RayCaster();
    private Vector3f mousePosition = null;

    private UpdateCallback updateCallback;
    private InputCallback inputCallback;
    private Mouse3DClickCallBack mouseCallback;
    private volatile boolean initialized;

    /*
     GetCameraMat -> ProjectionMatrix
     camera.setupviewmatrix -> viewMAtrix

     */


    /**
     * Initializes the elements to create the window
     *
     * @param pInitX      initX
     * @param pInitY      initY
     * @param pInitWidth  initWidth
     * @param pInitHeight initHeight
     * @param pInitVSYNC  initVSYNC
     */
    public GameLoop(int pInitX, int pInitY, int pInitWidth, int pInitHeight, boolean pInitVSYNC) {
        this(pInitX, pInitY, pInitWidth, pInitHeight);
        this.initVSYNC = pInitVSYNC;
    }

    /**
     * Initializes the elements to create the window
     *
     * @param pInitX      initX
     * @param pInitY      initY
     * @param pInitWidth  initWidth
     * @param pInitHeight initHeight
     */
    public GameLoop(int pInitX, int pInitY, int pInitWidth, int pInitHeight) {
        this();
        this.initX = pInitX;
        this.initY = pInitY;
        this.initWidth = pInitWidth;
        this.initHeight = pInitHeight;
    }

    public GameLoop() {
        this.mRenderer = new Renderer();
        this.mCamerasList = new Vector<>();
        this.mCamera = new Camera();
        mCamerasList.add(mCamera);
        this.mSceneRenderer = null;
        this.mItemGroupArrayList = new Vector<>();
        this.mSkyboxToBeRemoved = new Vector<>();
        this.mLoop = false;
        this.initVSYNC = true;
        this.mStringTextureConcurrentHashMap = new ConcurrentHashMap<>();
        this.updateCallback = null;
        this.inputCallback = null;
        initialized = false;
        bgndRed = 0; bgndGreen = 0; bgndBlue = 0;
    }

    public void setBackgroundColor(float red, float green, float blue) {
        bgndRed = red; bgndGreen = green; bgndBlue = blue;
    }

    /* package */ synchronized void addToScene(ItemObject itemObj) {
        mSceneRenderer.add(itemObj);
    }

    /* package */ synchronized void removeFromScene(ItemObject pItem) {
        mSceneRenderer.removeItem(pItem);
    }

    /* package */ synchronized ItemGroup createGroup(String id) {
        ItemGroup group = new ItemGroup(id);
        mItemGroupArrayList.add(group);
        return group;
    }

    /* package */ synchronized void removeGroup(ItemGroup pGroup) {
        pGroup.removeAll();
        mItemGroupArrayList.remove(pGroup);
    }

    /* package */ synchronized Vector<ItemGroup> getItemGroupArrayList() {
        return mItemGroupArrayList;
    }

    /* package */ synchronized LightModel getSceneLight() {
        return mSceneRenderer.getLightModel();
    }

    /* package */ synchronized Texture fetchTexture(String textureName) {
        return mStringTextureConcurrentHashMap.get(textureName);
    }

    /* package */ synchronized void removeSkybox() {
        mSkyboxToBeRemoved.add(mSkybox);
        this.mSkybox = null;
    }

    /* package */ synchronized void clearCameras() {
        mCamerasList = new Vector<>();
    }

    /* package */ void addCamera(int pIndex, Camera pCamera) {
        if (pIndex == 0) mCamera = pCamera;
        mCamerasList.add(pIndex, pCamera);
    }

    /* package */ synchronized Camera getCamera() {
        return mCamera;
    }

    /* package */ synchronized void setCamera(Camera pCamera) {
        this.mCamera = pCamera;
    }

    /* package */ synchronized Vector<Camera> getCamerasList() {
        return mCamerasList;
    }

    /* package */ synchronized Skybox getSkybox() {
        return mSkybox;
    }

    /* package */ synchronized void setSkybox(Skybox pSkybox) {
        if (this.mSkybox != null) {
            mSkyboxToBeRemoved.add(pSkybox);
        }
        this.mSkybox = pSkybox;
    }

    /**
     * Function managed by a Thread, which creates the world and which manages our game loop.
     */
    public void run() {
        try {
            this.init();
        } catch (Exception pE) {
            pE.printStackTrace();
           return;
        }

        try {
            this.loop();
        } catch (Exception pE) {
            pE.printStackTrace();
        } finally {
//            cleanup();
        }
    }

    /**
     * Input of critical section, allows to protect the resource share loop.
     * Stop the game loop and stop the thread that manage the world.
     */
    public synchronized void close() throws InterruptedException {
        mLoop = false;
        this.wait();
    }

    /**
     * Input of critical section, allows to protect the synchronization;
     * Allows synchronization between Threads (releases the lock (caused by the wait ()) and allows other threads waiting to execute).
     */
    private synchronized void notifyFinished() {
        this.notify();
    }

    public synchronized void registerUpdateCallback(UpdateCallback cb) {
        updateCallback = cb;
    }

    public synchronized void registerInputCallback(InputCallback callback) {
        inputCallback = callback;
        if(initialized) {
            Window.getGLFWKeyCallback().registerInputCallback(callback);
        }
    }

    //3D click

    public synchronized void registerMouse3DClickCallBack(Mouse3DClickCallBack mc){
        if(mouseCallback != null) {
            throw new Error("Mouse callback already registered");
        }
        mouseCallback = mc;

        if(initialized) {
            Window.getGLFWMouseCallback().registerMouseCallback(mc);
        }
    }


    // End 3D click

    /**
     * Allows to initialize the parameters of the class World.
     *
     */
    public synchronized void init() {
        Window.init(initWidth, initHeight, initVSYNC);

        if(inputCallback != null) {
            Window.getGLFWKeyCallback().registerInputCallback(inputCallback);
        }

        if(mouseCallback != null) {
            Window.getGLFWMouseCallback().registerMouseCallback(mouseCallback);
        }
        initShaderManager();
        initialized = true;
    }

    // UpdateRate: FIXED
    // FrameRate: VARIABLE
    private void loop() {
        double dt = 0.01; // Update Rate: 1 ~= 2 fps | 0.001 ~= 1000 fps
        double beforeTime = glfwGetTime();
        double lag = 0d;
        mLoop = true;
        while (!Window.windowShouldClose() && mLoop) { /* Check if the window has not been closed. */
            double nowTime = glfwGetTime();
            double framet = nowTime - beforeTime;
            beforeTime = nowTime;
            lag += framet;
            //mousePosition=RayCaster.getWorldRay(Window.windowHandle, mCamera);

            //refresh rate ??
//            Thread.sleep(20); // XXX ? Why sleep ?

            if(updateCallback != null) {
                while (lag >= dt) {
                    updateCallback.update(dt);
                    lag -= dt;
                }
            }


            /*Clean the window*/
            boolean isResized = Window.clear();

           /* Input of critical section, allows to protect the resource mSkyboxToBeRemoved .
              Deallocation of VAO and VBO, Moreover Delete the buffers VBO and VAO. */

            for (Skybox lSkybox : mSkyboxToBeRemoved) {
                lSkybox.cleanUp();
            }
            mSkyboxToBeRemoved.clear();

            glClearColor(bgndRed, bgndGreen, bgndBlue, 0.0f);

           /*  Input of critical section, allows to protect the creation of our logic of Game .
               1 Maximum thread in Synchronize -> mutual exclusion.*/
            synchronized (mSceneRenderer) {
                // XXX: for now shadow mapping is deactivated
                //mSceneRenderer.getLightModel().renderShadowMap(mSceneRenderer, mCamera, shaderManager);
                mRenderer.render(mSceneRenderer, isResized, mCamera, mSkybox, shaderManager);
            }

           /*  Rendered with vSync (vertical Synchronization)
               Update the window's picture */
            Window.update();

        }
    }

    private void initShaderManager() {
        shaderManager = new ShaderManager();
        // register helpers
        ShaderProgram helper = new HelperVerticesShaders();
        helper.init();
        shaderManager.register("VertexHelper", helper);
        helper = new HelperNormalsShaders();
        helper.init();
        shaderManager.register("NormalHelper", helper);
        helper = new HelperAxesShaders();
        helper.init();
        shaderManager.register("AxisHelper", helper);
    }

    public void installScene(SceneRenderer sceneRenderer) {
        if (mSceneRenderer != null) {
            throw new Error("Scene already installed, uninstall first.");
        }
        if (shaderManager != null) {
            throw new Error("shaderManager non-null (please report)");
        }
        mSceneRenderer = sceneRenderer;
    }

    private void cleanupScene() {
        mSceneRenderer.cleanUp(shaderManager);
        shaderManager.cleanUp();
        mSceneRenderer = null;
        shaderManager = null;
    }

    /**
     * Deallocates the resources used by the world
     */
    private void cleanup() {
        /* Deallocations renderer, SceneVertex and Skybox. */
        //mRenderer.cleanUp();
        cleanupScene();

        if (mSkybox != null) mSkybox.cleanUp();
        /* Deallocation of the window's resources. */
        Window.cleanUp();
        this.notifyFinished();
    }

}

