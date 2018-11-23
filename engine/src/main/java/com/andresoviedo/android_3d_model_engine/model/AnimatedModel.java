package com.andresoviedo.android_3d_model_engine.model;

import android.opengl.Matrix;

import com.andresoviedo.android_3d_model_engine.animation.Animation;
import com.andresoviedo.android_3d_model_engine.animation.Animator;
import com.andresoviedo.android_3d_model_engine.services.collada.entities.Joint;

import java.nio.FloatBuffer;


public class AnimatedModel extends Object3DData {

	// skeleton
	private Joint rootJoint;
	private int jointCount;
	private int boneCount;
	private FloatBuffer jointIds;
	private FloatBuffer vertexWeigths;
	private Animation animation;

	// cache
	private float[][] jointMatrices;

	public AnimatedModel(FloatBuffer vertexArrayBuffer){
		super(vertexArrayBuffer);
	}


	public AnimatedModel setRootJoint(Joint rootJoint, int jointCount, int boneCount, boolean
									  recalculateInverseBindTransforms) {
		this.rootJoint = rootJoint;
		this.jointCount = jointCount;
		this.boneCount = boneCount;
        float[] parentTransform = new float[16];
        Matrix.setIdentityM(parentTransform,0);
        rootJoint.calcInverseBindTransform(parentTransform, recalculateInverseBindTransforms);
        this.jointMatrices = new float[boneCount][16];
		return this;
	}

	public int getJointCount(){
		return jointCount;
	}

	public int getBoneCount() {
		return boneCount;
	}

	public AnimatedModel setJointCount(int jointCount){
		this.jointCount = jointCount;
		return this;
	}

	public AnimatedModel setJointIds(FloatBuffer jointIds){
		this.jointIds = jointIds;
		return this;
	}

	public FloatBuffer getJointIds(){
		return jointIds;
	}

	public AnimatedModel setVertexWeights(FloatBuffer vertexWeigths){
		this.vertexWeigths = vertexWeigths;
		return this;
	}

	public FloatBuffer getVertexWeights(){
		return vertexWeigths;
	}

	public AnimatedModel doAnimation(Animation animation){
		this.animation = animation;
		return this;
	}

	public Animation getAnimation(){
		return animation;
	}


	public Joint getRootJoint() {
		return rootJoint;
	}


	public float[][] getJointTransforms() {
		addJointsToArray(rootJoint, jointMatrices);
		return jointMatrices;
	}


	private void addJointsToArray(Joint headJoint, float [][] jointMatrices) {
		if (headJoint.getIndex() >= 0) {
			jointMatrices[headJoint.getIndex()] = headJoint.getAnimatedTransform();
		}
		for (int i=0; i<headJoint.getChildren().size(); i++) {
			Joint childJoint = headJoint.getChildren().get(i);
			addJointsToArray(childJoint, jointMatrices);
		}
	}
}
