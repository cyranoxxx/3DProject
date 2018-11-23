package com.andresoviedo.android_3d_model_engine.animation;

import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import com.andresoviedo.android_3d_model_engine.model.Object3DData;
import com.andresoviedo.android_3d_model_engine.services.collada.entities.Joint;

import java.util.HashMap;
import java.util.Map;



public class Animator {

	private float animationTime = 0;

    private final float IDENTITY_MATRIX[] = new float[16];

    // TODO: implement slower/faster speed
    private float speed = 1f;

    private final Map<String,float[]> cache = new HashMap<>();

    public Animator() {
        Matrix.setIdentityM(IDENTITY_MATRIX,0);
	}


	public void update(Object3DData obj) {
		if (!(obj instanceof AnimatedModel)) {
			return;
		}
		// if (true) return;
		AnimatedModel animatedModel = (AnimatedModel)obj;
		if (animatedModel.getAnimation() == null) return;

		// add missing key transformations
		initAnimation(animatedModel);

		// increase time to progress animation
		increaseAnimationTime((AnimatedModel)obj);

		Map<String, float[]> currentPose = calculateCurrentAnimationPose(animatedModel);

		applyPoseToJoints(currentPose, (animatedModel).getRootJoint(), IDENTITY_MATRIX, 0);
	}

	private void initAnimation(AnimatedModel animatedModel) {
		if (animatedModel.getAnimation().isInitialized()) {
			return;
		}
		KeyFrame[] keyFrames = animatedModel.getAnimation().getKeyFrames();
		Log.i("Animator", "Initializing " + animatedModel.getId() + ". " + keyFrames.length + " key frames...");
		for (int i = 0; i < keyFrames.length; i++) {
			int j = (i + 1) % keyFrames.length;
			KeyFrame keyFramePrevious = keyFrames[i];
			KeyFrame keyFrameNext = keyFrames[j];
			Map<String, JointTransform> jointTransforms = keyFramePrevious.getJointKeyFrames();
			for (Map.Entry<String, JointTransform> transform : jointTransforms.entrySet()) {
				String jointId = transform.getKey();
				if (keyFrameNext.getJointKeyFrames().containsKey(jointId)) {
					continue;
				}
				JointTransform keyFramePreviousTransform = keyFramePrevious.getJointKeyFrames().get(jointId);
				JointTransform keyFrameNextTransform;
				KeyFrame keyFrameNextNext;
				int k = (j + 1) % keyFrames.length;
				do {
					keyFrameNextNext = keyFrames[k];
					keyFrameNextTransform = keyFrameNextNext.getJointKeyFrames().get(jointId);
					k = (k + 1) % keyFrames.length;
				} while (keyFrameNextTransform == null);
				this.animationTime = keyFrameNext.getTimeStamp();
				float progression = calculateProgression(keyFramePrevious, keyFrameNextNext);
				JointTransform missingFrameTransform = JointTransform.interpolate(keyFramePreviousTransform, keyFrameNextTransform, progression);
				keyFrameNext.getJointKeyFrames().put(jointId, missingFrameTransform);
				Log.i("Animator","Added missing key transform for "+jointId);
			}
		}
		animatedModel.getAnimation().setInitialized(true);
		Log.i("Animator", "Initialized " + animatedModel.getId() + ". " + keyFrames.length + " key frames");
	}


	private void increaseAnimationTime(AnimatedModel obj) {
		this.animationTime = SystemClock.uptimeMillis() / 1000f * speed;
		this.animationTime %= obj.getAnimation().getLength();
	}

	private Map<String, float[]> calculateCurrentAnimationPose(AnimatedModel obj) {
		KeyFrame[] frames = getPreviousAndNextFrames(obj);
		float progression = calculateProgression(frames[0], frames[1]);
		return interpolatePoses(frames[0], frames[1], progression);
	}


	private void applyPoseToJoints(Map<String, float[]> currentPose, Joint joint, float[] parentTransform, int limit) {

	    float[] currentTransform = cache.get(joint.getName());
	    if (currentTransform == null){
	        currentTransform = new float[16];
	        cache.put(joint.getName(), currentTransform);
        }

        // TODO: implement bind pose
        if (limit <= 0){
			if (currentPose.get(joint.getName()) != null) {
				Matrix.multiplyMM(currentTransform, 0, parentTransform, 0, currentPose.get(joint.getName()), 0);
			} else {
				Matrix.multiplyMM(currentTransform, 0, parentTransform, 0, joint.getBindLocalTransform(), 0);
			}
        } else{
            Matrix.multiplyMM(currentTransform, 0, parentTransform, 0, joint.getBindLocalTransform(), 0);
        }

        // calculate animation only if its used by vertices
        //joint.calcInverseBindTransform2(parentTransform);
        if (joint.getIndex() >= 0) {
            Matrix.multiplyMM(joint.getAnimatedTransform(), 0, currentTransform, 0,
                    joint.getInverseBindTransform(), 0);
        }

		// transform children
		for (int i=0; i<joint.getChildren().size(); i++) {
            Joint childJoint = joint.getChildren().get(i);
			applyPoseToJoints(currentPose, childJoint, currentTransform, limit-1);
		}
	}


	private KeyFrame[] getPreviousAndNextFrames(AnimatedModel obj) {
		KeyFrame[] allFrames = obj.getAnimation().getKeyFrames();
		KeyFrame previousFrame = allFrames[0];
		KeyFrame nextFrame = allFrames[0];
		for (int i = 1; i < allFrames.length; i++) {
			nextFrame = allFrames[i];
			if (nextFrame.getTimeStamp() > animationTime) {
				break;
			}
			previousFrame = allFrames[i];
		}
		return new KeyFrame[] { previousFrame, nextFrame };
	}

	private float calculateProgression(KeyFrame previousFrame, KeyFrame nextFrame) {
		float totalTime = nextFrame.getTimeStamp() - previousFrame.getTimeStamp();
		float currentTime = animationTime - previousFrame.getTimeStamp();
        // TODO: implement key frame display
        //return 0;
		return currentTime / totalTime;
	}

	private Map<String, float[]> interpolatePoses(KeyFrame previousFrame, KeyFrame nextFrame, float progression) {
	    // TODO: optimize this (memory allocation)
		Map<String, float[]> currentPose = new HashMap<>();
		for (String jointName : previousFrame.getJointKeyFrames().keySet()) {
			JointTransform previousTransform = previousFrame.getJointKeyFrames().get(jointName);
			if (Math.signum(progression) == 0){
                currentPose.put(jointName, previousTransform.getLocalTransform());
            } else {
			    // memory optimization
                float[] jointPose = cache.get(jointName);
                if (jointPose == null){
                    jointPose = new float[16];
                    cache.put(jointName, jointPose);
                }
                float[] jointPoseRot = cache.get("___rotation___interpolation___");
                if (jointPoseRot == null){
                    jointPoseRot = new float[16];
                    cache.put("___rotation___interpolation___", jointPoseRot);
                }
                // calculate interpolation
                JointTransform nextTransform = nextFrame.getJointKeyFrames().get(jointName);
                JointTransform.interpolate(previousTransform, nextTransform, progression, jointPose, jointPoseRot);
                currentPose.put(jointName, jointPose);
            }
		}
		return currentPose;
	}

}

