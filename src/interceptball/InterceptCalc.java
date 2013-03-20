package interceptball;

import java.awt.geom.Point2D;

public class InterceptCalc {
	public static Point2D.Double calcSpeed(double ourX, double ourY,
			double ourBearing, double targetX, double targetY,
			double distanceThreshold) {
		double xDiff = targetX - ourX;
		double yDiff = targetY - ourY;
		double speedForward = 0, speedRight = 0;
		double speedCoef = 0;
		// our robot is more than the threshold to the left of the target X
		if (xDiff > distanceThreshold) {
			speedForward = -100;
		} else {
			speedForward = 0;
		}
		// We are below where we need to be
		if (yDiff < -distanceThreshold) {
			speedRight = 100;
		}
		// We are above where we need to be
		else if (yDiff > distanceThreshold) {
			speedRight = -100;
		}
		// We are where we need to be
		else {
			speedForward = 100;
			speedRight = 0;
		}
		// Slow down as we approach to avoid overshooting
		if (Math.abs(yDiff) > 2.0 * distanceThreshold) {
			speedCoef = 1.0;
		} else {
			speedCoef = 0.7;
		}
		// Transform speeds to world coordinates instead of local, in case our
		// robot is not facing the enemy
		// goal
		double angle = ourBearing - Math.PI * 1.5;
		double cos = Math.cos(angle), sin = Math.sin(angle);
		double speedX = speedRight * cos - speedForward * sin;
		if (speedX > 100.0)
			speedX = 100.0;
		else if (speedX < -100.0)
			speedX = -100.0;
		double speedY = speedForward * cos + speedRight * sin;
		if (speedY > 100.0)
			speedY = 100.0;
		else if (speedY < -100.0)
			speedY = -100.0;
		return new Point2D.Double(speedCoef * speedX, speedCoef * speedY);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for (double angle = 0.0; angle < 2.0 * Math.PI; angle += (Math.PI / 6)) {
			System.out.println(calcSpeed(500, 240, angle, 600, 140, 20)
					.toString());
		}
	}

}
