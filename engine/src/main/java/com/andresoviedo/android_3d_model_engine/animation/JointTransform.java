package com.andresoviedo.android_3d_model_engine.animation;

import android.opengl.Matrix;

import com.andresoviedo.util.math.Quaternion;
import com.andresoviedo.android_3d_model_engine.services.collada.entities.Vector3f;


public class JointTransform {

	// remember, this position and rotation are relative to the parent bone!
    private final float[] matrix;
	private final Vector3f position;
	private final Quaternion rotation;

	public JointTransform(float[] matrix){
	    this.matrix = matrix;
        this.position = new Vector3f(matrix[12], matrix[13], matrix[14]);
        this.rotation = Quaternion.fromMatrix(matrix);
    }

	public JointTransform(Vector3f position, Quaternion rotation) {
	    this.matrix = null;
		this.position = position;
		this.rotation = rotation;
	}

	public Vector3f getPosition() {
		return position;
	}

	public Quaternion getRotation() {
		return rotation;
	}


	public float[] getLocalTransform() {
	    if (matrix != null){
	        return matrix;
        }
		float[] matrix = new float[16];
		Matrix.setIdentityM(matrix,0);
		Matrix.translateM(matrix,0,position.x,position.y,position.z);
		Matrix.multiplyMM(matrix,0,matrix,0,rotation.toRotationMatrix(new float[16]),0);
		return matrix;
	}


	protected static JointTransform interpolate(JointTransform frameA, JointTransform frameB, float progression) {
		Vector3f pos = interpolate(frameA.position, frameB.position, progression);
		Quaternion rot = Quaternion.interpolate(frameA.rotation, frameB.rotation, progression);
		return new JointTransform(pos, rot);
	}

    protected static float[] interpolate(JointTransform frameA, JointTransform frameB, float progression, float[]
            matrix1, float[] matrix2) {
        Vector3f pos = interpolate(frameA.position, frameB.position, progression);
        Quaternion rot = Quaternion.interpolate(frameA.rotation, frameB.rotation, progression);
        Matrix.setIdentityM(matrix1,0);
        Matrix.translateM(matrix1,0,pos.x,pos.y,pos.z);
        Matrix.multiplyMM(matrix1,0,matrix1,0,rot.toRotationMatrix(matrix2),0);
        return matrix1;
    }

	private static Vector3f interpolate(Vector3f start, Vector3f end, float progression) {
		float x = start.x + (end.x - start.x) * progression;
		float y = start.y + (end.y - start.y) * progression;
		float z = start.z + (end.z - start.z) * progression;
        // TODO: optimize this (memory allocation)
		return new Vector3f(x, y, z);
	}

}
