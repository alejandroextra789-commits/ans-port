package com.ans.client.bedrock;

/**
 * A Bedrock locator read from a .geo.json. Replaces GeckoLib 5's GeoLocator.
 * Offsets are in model units (pixels); rotations are in radians.
 */
public record GeoLocatorData(String boneName, float offsetX, float offsetY, float offsetZ,
                             float rotX, float rotY, float rotZ, boolean ignoreInheritedScale) {
}
