package io.github.hyscript7.projectfusion.warpzones.util;

public final class YawUtils {

    private YawUtils() {}

    public enum CardinalDirection {
        SOUTH(  0f),
        WEST(  90f),
        NORTH(180f),
        EAST( 270f);

        public final float yaw;

        CardinalDirection(float yaw) {
            this.yaw = yaw;
        }
    }

    public static float snapToCardinal(float yaw) {
        // Normalize to [0, 360)
        yaw = ((yaw % 360) + 360) % 360;

        CardinalDirection nearest = CardinalDirection.SOUTH;
        float smallestDelta = Float.MAX_VALUE;

        for (CardinalDirection direction : CardinalDirection.values()) {
            // Compute the shortest arc between two yaw values on the circle
            float delta = Math.abs(yaw - direction.yaw);
            delta = Math.min(delta, 360 - delta);

            if (delta < smallestDelta) {
                smallestDelta = delta;
                nearest = direction;
            }
        }

        return nearest.yaw;
    }
}
