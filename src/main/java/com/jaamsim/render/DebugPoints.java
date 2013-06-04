/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.render;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Vec4d;

public class DebugPoints implements Renderable {

	private FloatBuffer fb;
	private List<Vec4d> _points;
	private final float[] _colour;
	private final float[] _hoverColour;
	private double _pointWidth;
	private long _pickingID;
	private VisibilityInfo _visInfo;

	private double _collisionAngle = 0.008727; // 0.5 degrees in radians

	private AABB _bounds;

	public DebugPoints(List<Vec4d> points, Color4d colour, Color4d hoverColour, double pointWidth, VisibilityInfo visInfo, long pickingID) {
		_points = points;
		_colour = colour.toFloats();
		_hoverColour = hoverColour.toFloats();
		_pointWidth = pointWidth;
		_pickingID = pickingID;
		_visInfo = visInfo;

		_bounds = new AABB(points, 100000); // TODO, tune this fudge factor by something more real

		fb = FloatBuffer.allocate(3 * points.size());
		for (Vec4d point : points) {
			RenderUtils.putPointXYZ(fb, point);
		}
		fb.flip();
	}

	@Override
	public void render(Map<Integer, Integer> vaoMap, Renderer renderer,
			Camera cam, Ray pickRay) {

		float[] renderColour = _colour;
		if (pickRay != null && getCollisionDist(pickRay, false) > 0)
			renderColour = _hoverColour;

		DebugUtils.renderPoints(vaoMap, renderer, fb, renderColour, _pointWidth, cam);
	}

	@Override
	public long getPickingID() {
		return _pickingID;
	}

	@Override
	public AABB getBoundsRef() {
		return _bounds;
	}

	/**
	 * Set the angle of the collision cone in radians
	 * @param angle
	 */
	public void setCollisionAngle(double angle) {
		_collisionAngle = angle;
	}

	@Override
	public double getCollisionDist(Ray r, boolean precise) {
		if (r == null) {
			return -1;
		}

		double boundsDist = _bounds.collisionDist(r);
		if (boundsDist < 0) { return boundsDist; } // no bounds collision

		double tan = Math.tan(_collisionAngle);

		Vec4d op = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d); // Vector from ray start to

		double nearDist = Double.POSITIVE_INFINITY;

		for (Vec4d p : _points) {
			op.sub3(p, r.getStartRef());
			double hypot2 = op.magSquare3();

			double dot = op.dot3(r.getDirRef()); // Dot is the distance along the ray to the nearest point

			double rayDist = Math.sqrt(hypot2 - dot*dot);

			double collsionThreshold = dot * tan;
			if (rayDist < collsionThreshold && dot < nearDist) {
				// This is the closest point so far
				nearDist = dot;
			}
		}

		if (nearDist == Double.POSITIVE_INFINITY) {
			return -1;
		}
		return nearDist;
	}

	@Override
	public boolean hasTransparent() {
		return false;
	}

	@Override
	public void renderTransparent(Map<Integer, Integer> vaoMap, Renderer renderer, Camera cam, Ray pickRay) {
	}

	@Override
	public boolean renderForView(int viewID, double dist) {
		return _visInfo.isVisible(viewID, dist);
	}
}
