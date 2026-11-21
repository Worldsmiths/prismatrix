package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.core.thin3d.Ray;
import io.github.worldsmiths.prismatrix.optics.Cell;

/**
 * Game-owned board plane math: pick-ray ∩ y=0 → optics cell. World X maps to optics x; world Z maps
 * to optics y (y=0 at the top-entry edge).
 */
public final class BoardPlane {

    public static final float CELL = 1f;
    public static final float PLANE_Y = 0f;
    private static final float PARALLEL_EPS = 1e-6f;

    private BoardPlane() {}

    public static float cellCenterX(int opticsX) {
        return opticsX * CELL + CELL * 0.5f;
    }

    public static float cellCenterZ(int opticsY) {
        return opticsY * CELL + CELL * 0.5f;
    }

    public static float boardExtent(int gridSize) {
        return gridSize * CELL;
    }

    public static float boardCenterX(int gridSize) {
        return boardExtent(gridSize) * 0.5f;
    }

    public static float boardCenterZ(int gridSize) {
        return boardExtent(gridSize) * 0.5f;
    }

    /**
     * Intersects {@code ray} with the board plane and returns the optics cell, or {@code null} if
     * the ray is parallel to the plane, hits behind the origin, or misses the grid.
     */
    public static Cell cellAt(Ray ray, int gridSize) {
        if (ray == null) {
            throw new IllegalArgumentException("ray must not be null");
        }
        if (gridSize < 1) {
            throw new IllegalArgumentException("gridSize must be >= 1");
        }
        float dy = ray.direction.y;
        if (Math.abs(dy) < PARALLEL_EPS) {
            return null;
        }
        float t = (PLANE_Y - ray.origin.y) / dy;
        if (t < 0f) {
            return null;
        }
        float hitX = ray.origin.x + t * ray.direction.x;
        float hitZ = ray.origin.z + t * ray.direction.z;
        if (hitX < 0f || hitZ < 0f) {
            return null;
        }
        int ox = (int) (hitX / CELL);
        int oy = (int) (hitZ / CELL);
        if (ox < 0 || ox >= gridSize || oy < 0 || oy >= gridSize) {
            return null;
        }
        return new Cell(ox, oy);
    }
}
