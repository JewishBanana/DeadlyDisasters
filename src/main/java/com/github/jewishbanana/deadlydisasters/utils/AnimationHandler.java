package com.github.jewishbanana.deadlydisasters.utils;

import org.bukkit.entity.ArmorStand;
import org.bukkit.util.EulerAngle;

public class AnimationHandler {
	
	private Animation[] animations;
	private int currentPoint = 0, continueFrame = 0, frameRate = 1;
	public boolean shouldStop = true,resetOnStop,adjustOnStop,isDone,continous;
	
	public AnimationHandler(boolean resetOnStop, boolean adjustOnStop, boolean continous) {
		this.resetOnStop = resetOnStop;
		this.adjustOnStop = adjustOnStop;
		this.continous = continous;
	}
	public void tick(ArmorStand stand) {
		if (shouldStop) {
			if (!continous && isDone)
				return;
			if (!animations[currentPoint].isFinished) {
				animations[currentPoint].tick(stand);
				if (animations[currentPoint].isFinished) {
					if (adjustOnStop)
						for (AnimationCheckpoint point : animations[0].points)
							point.adjustToFrame(stand, 0);
					if (resetOnStop) {
						currentPoint = 0;
						animations[currentPoint].isFinished = false;
					}
					isDone = true;
				}
			} else
				isDone = true;
			return;
		}
		if (!animations[currentPoint].isFinished)
			animations[currentPoint].tick(stand);
		else {
			currentPoint++;
			if (currentPoint >= animations.length)
				currentPoint = continueFrame;
			animations[currentPoint].tick(stand);
		}
	}
	public void setAnimations(AnimationHandler.Animation... animations) {
		this.animations = animations;
	}
	public void go() {
		shouldStop = false;
		isDone = false;
	}
	public void stop() {
		shouldStop = true;
	}
	public void startAnimation(ArmorStand stand) {
		go();
		tick(stand);
		stop();
	}
	public void forceStop(ArmorStand stand) {
		for (Animation anim : animations)
			anim.forceStop(stand);
		shouldStop = true;
	}
	public boolean isFinished() {
		return isDone;
	}
	public int getContinueFrame() {
		return continueFrame;
	}
	public void setContinueFrame(int continueFrame) {
		this.continueFrame = continueFrame;
	}
	public int getFrameRate() {
		return frameRate;
	}
	public void setFrameRate(int frameRate) {
		this.frameRate = frameRate;
	}
	public class Animation {
		
		private AnimationCheckpoint[] points;
		private boolean isFinished;
		
		public Animation(AnimationCheckpoint... animationCheckpoints) {
			this.points = animationCheckpoints;
		}
		public void tick(ArmorStand stand) {
			isFinished = false;
			boolean ticked = false;
			for (AnimationCheckpoint point : points)
				if (!point.isFinished) {
					point.tick(stand);
					ticked = true;
				}
			if (!ticked) {
				isFinished = true;
				for (AnimationCheckpoint point : points)
					point.reset();
			}
		}
		public void forceStop(ArmorStand stand) {
			for (AnimationCheckpoint point : points)
				point.forceStop(stand);
			isFinished = true;
		}
	}
	public class AnimationCheckpoint {
		
		private BodyPart part;
		private int frame = 1;
		private boolean reverse,flipped,isFinished,overrideValues;
		private double[][] frames;
		
		public AnimationCheckpoint(BodyPart part, double initialX, double initialY, double initialZ, double targetX, double targetY, double targetZ, int ticks, boolean reverse, double acceleration, double decceleration, boolean overrideValues) {
			this.part = part;
			this.reverse = reverse;
			this.overrideValues = overrideValues;
			
			frames = new double[ticks+1][];
			frames[0] = new double[] {initialX, initialY, initialZ};
			double speedX = (targetX - initialX) / ticks;
			double speedY = (targetY - initialY) / ticks;
			double speedZ = (targetZ - initialZ) / ticks;
			int halfSteps = ticks / 2;
			int quarterSteps = halfSteps / 2;
			if (acceleration > 0 && decceleration == 0) {
				quarterSteps = halfSteps;
				halfSteps = ticks;
			} else if (decceleration > 0 && acceleration == 0) {
				quarterSteps = halfSteps;
				halfSteps = 0;
			}
			double accStep = acceleration / (double) (quarterSteps);
			double decStep = decceleration / (double) (quarterSteps);
			for (int i=0; i < frames.length-1; i++) {
				if (frames[i+1] == null)
					frames[i+1] = new double[]{speedX, speedY, speedZ};
				if (i < quarterSteps && quarterSteps < halfSteps) {
					double[] startSpeeds = new double[3];
					startSpeeds[0] = speedX * (1 - (accStep * (quarterSteps-i)));
					startSpeeds[1] = speedY * (1 - (accStep * (quarterSteps-i)));
					startSpeeds[2] = speedZ * (1 - (accStep * (quarterSteps-i)));
					frames[i+1] = startSpeeds;
					double[] endSpeeds = new double[3];
					endSpeeds[0] = speedX * (1 + (accStep * (quarterSteps-i)));
					endSpeeds[1] = speedY * (1 + (accStep * (quarterSteps-i)));
					endSpeeds[2] = speedZ * (1 + (accStep * (quarterSteps-i)));
					frames[halfSteps-(i+1)+1] = endSpeeds;
				} else if (i >= halfSteps) {
					if (i >= halfSteps+quarterSteps)
						continue;
					double[] startSpeeds = new double[3];
					startSpeeds[0] = speedX * (1 + (decStep * (quarterSteps-(i-halfSteps))));
					startSpeeds[1] = speedY * (1 + (decStep * (quarterSteps-(i-halfSteps))));
					startSpeeds[2] = speedZ * (1 + (decStep * (quarterSteps-(i-halfSteps))));
					frames[i+1] = startSpeeds;
					double[] endSpeeds = new double[3];
					endSpeeds[0] = speedX * (1 - (decStep * (quarterSteps-(i-halfSteps))));
					endSpeeds[1] = speedY * (1 - (decStep * (quarterSteps-(i-halfSteps))));
					endSpeeds[2] = speedZ * (1 - (decStep * (quarterSteps-(i-halfSteps))));
					frames[ticks-((i-halfSteps)+1)+1] = endSpeeds;
				}
			}
			for (int i=1; i < frames.length; i++) {
				initialX += frames[i][0];
				initialY += frames[i][1];
				initialZ += frames[i][2];
				frames[i] = new double[] {initialX, initialY, initialZ};
			}
		}
		public AnimationCheckpoint(BodyPart part, double initialX, double initialY, double initialZ, double targetX, double targetY, double targetZ, int ticks, boolean reverse, double acceleration, double decceleration) {
			this(part, initialX, initialY, initialZ, targetX, targetY, targetZ, ticks, reverse, acceleration, decceleration, false);
		}
		public AnimationCheckpoint(BodyPart part, double initialX, double initialY, double initialZ, double targetX, double targetY, double targetZ, int ticks, boolean reverse) {
			this(part, initialX, initialY, initialZ, targetX, targetY, targetZ, ticks, reverse, 0.0, 0.0, false);
		}
		public void tick(ArmorStand stand) {
			if (isFinished)
				reset();
			adjustToFrame(stand, frame);
			if (!flipped)
				frame += frameRate;
			else
				frame -= frameRate;
			if (frame >= frames.length || frame < 0) {
				if (flipped || !reverse) {
					isFinished = true;
					return;
				}
				frame = frames.length-1;
				flipped = true;
			}
		}
		public void adjustToFrame(ArmorStand stand, int frame) {
			EulerAngle angle = new EulerAngle(Math.toRadians(frames[frame][0]), Math.toRadians(frames[frame][1]), Math.toRadians(frames[frame][2]));
			EulerAngle pose;
			switch (part) {
			case BODY:
				pose = stand.getBodyPose();
				stand.setBodyPose(overrideValues ? angle : new EulerAngle(angle.getX() != 0 ? angle.getX() : pose.getX(), angle.getY() != 0 ? angle.getY() : pose.getY(), angle.getZ() != 0 ? angle.getZ() : pose.getZ()));
				break;
			case HEAD:
				pose = stand.getHeadPose();
				stand.setHeadPose(overrideValues ? angle : new EulerAngle(angle.getX() != 0 ? angle.getX() : pose.getX(), angle.getY() != 0 ? angle.getY() : pose.getY(), angle.getZ() != 0 ? angle.getZ() : pose.getZ()));
				break;
			case RIGHT_ARM:
				pose = stand.getRightArmPose();
				stand.setRightArmPose(overrideValues ? angle : new EulerAngle(angle.getX() != 0 ? angle.getX() : pose.getX(), angle.getY() != 0 ? angle.getY() : pose.getY(), angle.getZ() != 0 ? angle.getZ() : pose.getZ()));
				break;
			case LEFT_ARM:
				pose = stand.getLeftArmPose();
				stand.setLeftArmPose(overrideValues ? angle : new EulerAngle(angle.getX() != 0 ? angle.getX() : pose.getX(), angle.getY() != 0 ? angle.getY() : pose.getY(), angle.getZ() != 0 ? angle.getZ() : pose.getZ()));
				break;
			case RIGHT_LEG:
				pose = stand.getRightLegPose();
				stand.setRightLegPose(overrideValues ? angle : new EulerAngle(angle.getX() != 0 ? angle.getX() : pose.getX(), angle.getY() != 0 ? angle.getY() : pose.getY(), angle.getZ() != 0 ? angle.getZ() : pose.getZ()));
				break;
			case LEFT_LEG:
				pose = stand.getLeftLegPose();
				stand.setLeftLegPose(overrideValues ? angle : new EulerAngle(angle.getX() != 0 ? angle.getX() : pose.getX(), angle.getY() != 0 ? angle.getY() : pose.getY(), angle.getZ() != 0 ? angle.getZ() : pose.getZ()));
				break;
			}
		}
		public void reset() {
			frame = 1;
			isFinished = false;
			flipped = false;
		}
		public void forceStop(ArmorStand stand) {
			adjustToFrame(stand, 0);
			isFinished = true;
		}
	}
	public enum BodyPart {
		HEAD,
		BODY,
		RIGHT_ARM,
		LEFT_ARM,
		RIGHT_LEG,
		LEFT_LEG
	}
}
