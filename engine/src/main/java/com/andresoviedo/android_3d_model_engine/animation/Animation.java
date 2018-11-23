package com.andresoviedo.android_3d_model_engine.animation;


import com.andresoviedo.android_3d_model_engine.model.AnimatedModel;

public class Animation {

	private final float length;//in seconds
	private final KeyFrame[] keyFrames;
	private boolean initialized;

	public Animation(float lengthInSeconds, KeyFrame[] frames) {
		this.keyFrames = frames;
		this.length = lengthInSeconds;
	}

	public void setInitialized(boolean initialized){
		this.initialized = initialized;
	}

	public boolean isInitialized(){
		return initialized;
	}

	public float getLength() {
		return length;
	}

	public KeyFrame[] getKeyFrames() {
		return keyFrames;
	}

}
