package yaw.engine.shader;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import yaw.engine.light.AmbientLight;
import yaw.engine.light.DirectionalLight;
import yaw.engine.light.PointLight;
import yaw.engine.light.SpotLight;
import yaw.engine.mesh.Material;

import java.nio.FloatBuffer;
import java.util.HashMap;

import static org.lwjgl.opengl.GL32.*;


public abstract class ShaderProgram {
    private final int mProgramId;
    private final HashMap<String, Integer> mUniformsList = new HashMap<>();
    private int mVertexShaderId;
    private int mFragmentShaderId;
    private int mGeometryShaderId;


    /**
     * Create a new shader program
     */
    public ShaderProgram() {
        mProgramId = glCreateProgram();
        if (!glIsProgram(mProgramId)) {
            throw new Error("Could not create Shader program");
        }
    }

    public abstract void init();

    /**
     * Create a vertex type shader
     *
     * @param shaderCode source code for the shader
     */
    public void createVertexShader(String shaderCode) {
        mVertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
    }

    public void createVertexShader(ShaderCode code) {
        createVertexShader(code.toString());
    }
    /**
     * Create a fragment shader
     *
     * @param shaderCode source code for the shader
     */
    public void createFragmentShader(String shaderCode) {
        mFragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
    }

    public void createFragmentShader(ShaderCode shaderCode) {
        createFragmentShader(shaderCode.toString());
    }

    /**
     * Create a geometry shader
     *
     * @param shaderCode source code for the shader
     */
    public void createGeometryShader(String shaderCode) {
        mGeometryShaderId = createShader(shaderCode, GL_GEOMETRY_SHADER);
    }

    public void createGeometryShader(ShaderCode shaderCode) {
        createGeometryShader(shaderCode.toString());
    }

    private String fetchInfoLog() {
        int[] logLength = {0};
        glGetShaderiv(mProgramId, GL_INFO_LOG_LENGTH, logLength);
        return "" + glGetShaderInfoLog(mProgramId, logLength[0]+1);
    }

    /**
     * Links the program object.
     *
     */
    public void link()  {
        glLinkProgram(mProgramId);
        if (glGetProgrami(mProgramId, GL_LINK_STATUS) == 0) {
            throw new Error("Error linking Shader code\n  ==> " + fetchInfoLog());
        }

        glValidateProgram(mProgramId);
        if (glGetProgrami(mProgramId, GL_VALIDATE_STATUS) == 0) {
            throw new Error("Error validating Shader code\n  ==> " + fetchInfoLog());
        }

    }

    /**
     * Installs the program object as part of current rendering state.
     */
    public void bind() {
        /*Specifies the handle of the program object whose executables are to be used as part of current rendering state.*/
        glUseProgram(mProgramId);

    }

    /**
     *
     */
    public void cleanup() {
        unbind();
        if (mProgramId != 0) {
            if (mVertexShaderId != 0) {
                /*   Detaches a shader object from a program object to which it is attached*/
                glDetachShader(mProgramId, mVertexShaderId);
            }
            if (mFragmentShaderId != 0) {
                /*Detaches a shader object from a program object to which it is attached*/
                glDetachShader(mProgramId, mFragmentShaderId);
            }
            if (mGeometryShaderId != 0) {
                /*Detaches a shader object from a program object to which it is attached*/
                glDetachShader(mProgramId, mGeometryShaderId);
            }
            /*Deletes a program object*/
            glDeleteProgram(mProgramId);
        }
    }

    // Deallocate Shader Program

    /**
     * unbind
     */
    public void unbind() {
        glUseProgram(0);
    }

    /**
     * Create  a list of n point light uniform
     *
     * @param uniformName uniform name
     * @param size        size
     */
    public void createPointLightListUniform(String uniformName, int size)  {
        for (int i = 0; i < size; i++) {
            createPointLightUniform(uniformName + "[" + i + "]");
        }
    }

    /**
     * Returns the location of a uniform variable
     *
     * @param uniformName Points to a null terminated string containing the name of the uniform variable whose location is to be queried.
     * @return the location
     */
    public int createUniform(String uniformName) {
        int res = glGetUniformLocation(mProgramId, uniformName);
        if (res < 0) {
            System.out.println("Uniform creation error: " + uniformName);
            return -1;
        }
        mUniformsList.put(uniformName, res);
        return res;
    }

    /**
     * Create a list of n spotlight uniform
     *
     * @param uniformName uniform name
     * @param size        the size
     */
    public void createSpotLightUniformList(String uniformName, int size) {
        for (int i = 0; i < size; i++) {
            createSpotLightUniform(uniformName + "[" + i + "]");
        }
    }

    /**
     * Create uniform for each attribute of the directional light
     *
     * @param uniformName uniform name
     */
    public void createDirectionalLightUniform(String uniformName) {
        createUniform(uniformName + ".color");
        createUniform(uniformName + ".direction");
        createUniform(uniformName + ".intensity");
    }

    /**
     * Retrieve a uniform location by name
     *
     * @param uniformName the uniform name
     * @return the location of the uniform
     */
    public int getUniform(String uniformName) {
        return mUniformsList.get(uniformName);
    }

    /**
     * Modifies the value of a uniform variable with the specified value
     *
     * @param uniformName the uniform name
     * @param value       the value
     */
    public void setUniform(String uniformName, Matrix4f value) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        /*load the value in th floatbuffer*/
        value.get(fb);
        /*Warning can cause nullpointer exception*/
        glUniformMatrix4fv(mUniformsList.get(uniformName), false, fb);
    }

    public void setUniform(String uniformName, Matrix3f value) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        /*load the value in th floatbuffer*/
        value.get(fb);
        /*Warning can cause nullpointer exception*/
        glUniformMatrix3fv(mUniformsList.get(uniformName), false, fb);
    }

    /**
     * Modifies the value of a uniform Vector3f with the specified Vector3f
     *
     * @param uniformName the uniform name
     * @param value       the Vector3f
     */
    public void setUniform(String uniformName, Vector3f value) {
        glUniform3f(mUniformsList.get(uniformName), value.x, value.y, value.z);
    }

    /**
     * Modifies the value of a uniform int with the specified float
     *
     * @param uniformName the uniform name
     * @param value       the float
     */
    public void setUniform(String uniformName, int value) {
        System.out.println("============> " + uniformName + " : " + value + " <============");
        Integer location = mUniformsList.get(uniformName);
        if (location != null) {
            glUniform1i(location, value);
        } else {
            System.err.println("Uniform '" + uniformName + "' not found or not used in the shader.");
        }
    }

    /**
     * Modifies the value of a uniform float with the specified float
     *
     * @param uniformName the uniform name
     * @param value       the float
     */
    public void setUniform(String uniformName, float value) {
        glUniform1f(mUniformsList.get(uniformName), value);
    }

    /**
     * Modifies the value of uniforms specified by the uniformName with the specified pointLights
     *
     * @param uniformName the uniform name
     * @param pointLights the pointlights
     */
    public void setUniform(String uniformName, PointLight[] pointLights) {
        int numLights = pointLights != null ? pointLights.length : 0;
        for (int i = 0; i < numLights; i++) {
            setUniform(uniformName, pointLights[i], i);
        }
    }

    /**
     * Modifies the value of uniform specified by the uniformName and the position with the specified value
     *
     * @param uniformName the uniform name
     * @param pointLight  the point light
     * @param pos         the position
     */
    public void setUniform(String uniformName, PointLight pointLight, int pos) {
        setUniform(uniformName + "[" + pos + "]", pointLight);
    }

    /**
     * Modifies the value of uniforms specified by the uniformName with the specified spotLights
     *
     * @param uniformName the uniform name
     * @param spotLights  the spotLights
     */
    public void setUniform(String uniformName, SpotLight[] spotLights) {
        int numLights = spotLights != null ? spotLights.length : 0;
        for (int i = 0; i < numLights; i++) {
            setUniform(uniformName, spotLights[i], i);
        }
    }

    /**
     * Modifies the value of uniform specified by the uniformName and the position with the specified value
     *
     * @param uniformName the uniform name
     * @param spotLight   the spotlight
     * @param pos         the position
     */
    public void setUniform(String uniformName, SpotLight spotLight, int pos) {
        setUniform(uniformName + "[" + pos + "]", spotLight);
    }

    /**
     * Modifies the value of uniform specified by the uniformName with the specified value
     *
     * @param uniformName the uniform name
     * @param dirLight    the directional Light
     */
    public void setUniform(String uniformName, DirectionalLight dirLight) {
        setUniform(uniformName + ".color", dirLight.getColor());
        setUniform(uniformName + ".direction", dirLight.mDirection);
        setUniform(uniformName + ".intensity", dirLight.getIntensity());
    }

    /**
     * Modifies the value of uniform specified by the uniformName with the specified value
     *
     * @param uniformName the uniform name
     * @param ambient     the ambient light
     */
    public void setUniform(String uniformName, AmbientLight ambient) {
        setUniform(uniformName, ambient.getShaderValue());
    }

    /**
     * Create a shader attach the specified source to it compile it and attach the shader to the main program
     *
     * @param shaderCode source code for the shader
     * @param shaderType the type of shader to be created. One of:
     *                   VERTEX_SHADER	FRAGMENT_SHADER	GEOMETRY_SHADER	TESS_CONTROL_SHADER
     *                   TESS_EVALUATION_SHADER
     * @return the id of the shader
     */
    private int createShader(String shaderCode, int shaderType) {
        /*the shader object whose source code is to be replaced*/
        int shaderId = glCreateShader(shaderType);
        /*can't use glIsShader here because it alwayse return false and we don't know why */
        if (shaderId == GL_FALSE) {
            throw new Error("Error creating shader. Code: shader id = " + shaderId + "\n ==> " + glGetShaderInfoLog(shaderId));
        }
        /*Sets the source code in shader to the source code in the array of strings specified by strings.*/
        glShaderSource(shaderId, shaderCode);
        /*Compiles a shader object.*/
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new Error("Error compiling Shader code: shader id = " + shaderId + "\n ==>" + glGetShaderInfoLog(shaderId, 1024));
        }

        /*void function */
        glAttachShader(mProgramId, shaderId);

        return shaderId;
    }

    /**
     * Create uniform for each attribute of the point light
     *
     * @param uniformName uniform name
     */
    private void createPointLightUniform(String uniformName) {
        createUniform(uniformName + ".color");
        createUniform(uniformName + ".intensity");
        createUniform(uniformName + ".position");
        createUniform(uniformName + ".att_constant");
        createUniform(uniformName + ".att_linear");
        createUniform(uniformName + ".att_quadratic");
    }

    /**
     * Create uniform for each attribute of the spot light
     *
     * @param uniformName uniform name
     */
    private void createSpotLightUniform(String uniformName) {
        createPointLightUniform(uniformName + ".pl");
        createUniform(uniformName + ".conedir");
        createUniform(uniformName + ".cutoff");
    }

    /**
     * Modifies the value of uniform specified by the uniformName with the specified value
     *
     * @param uniformName the uniform name
     * @param pointLight  the point light
     */
    private void setUniform(String uniformName, PointLight pointLight) {
        setUniform(uniformName + ".color", pointLight.getColor());
        setUniform(uniformName + ".position", pointLight.getPosition());
        setUniform(uniformName + ".intensity", pointLight.getIntensity());
        setUniform(uniformName + ".att_constant", pointLight.getConstantAtt());
        setUniform(uniformName + ".att_linear", pointLight.getLinearAtt());
        setUniform(uniformName + ".att_exponent", pointLight.getQuadraticAtt());
    }

    /**
     * Modifies the value of uniform specified by the uniformName with the specified value
     *
     * @param uniformName the uniform name
     * @param spotLight   the spotlight
     */
    private void setUniform(String uniformName, SpotLight spotLight) {
        setUniform(uniformName + ".pl", (PointLight) spotLight);
        setUniform(uniformName + ".conedir", spotLight.getConedir());
        setUniform(uniformName + ".cutoff", spotLight.getCutoffAngle());
    }

    public int getId() {
        return mProgramId;
    }

}
