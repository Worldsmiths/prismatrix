package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.core.thin3d.Ray;
import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.Port;

/**
 * Game-owned board plane math: pick-ray ∩ y=0 → optics cell or near-edge {@link Port}. World X maps
 * to optics x; world Z maps to optics y (y=0 at the top-entry edge).
 */
public final class BoardPlane {

    public static final float CELL = 1f;
    public static final float PLANE_Y = 0f;
    /**
     * Outside-board port-hit margin as a fraction of board extent (~former 28px strip on a 300px
     * screen footprint).
     */
    public static final float PORT_STRIP_FRAC = 28f / 300f;
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

    /** World-space depth of each expanded port strip for the given grid. */
    public static float portStripMargin(int gridSize) {
        return PORT_STRIP_FRAC * boardExtent(gridSize);
    }

    /** World X of the port on the board rim (cell-edge, not strip center). */
    public static float portEdgeX(Port port, int gridSize) {
        if (port == null) {
            throw new IllegalArgumentException("port must not be null");
        }
        switch (port.side()) {
            case T:
            case B:
                return cellCenterX(port.index());
            case L:
                return 0f;
            case R:
                return boardExtent(gridSize);
            default:
                throw new IllegalStateException("unknown side " + port.side());
        }
    }

    /** World Z of the port on the board rim (cell-edge, not strip center). */
    public static float portEdgeZ(Port port, int gridSize) {
        if (port == null) {
            throw new IllegalArgumentException("port must not be null");
        }
        switch (port.side()) {
            case T:
                return 0f;
            case B:
                return boardExtent(gridSize);
            case L:
            case R:
                return cellCenterZ(port.index());
            default:
                throw new IllegalStateException("unknown side " + port.side());
        }
    }

    /**
     * Intersects {@code ray} with the board plane and returns the optics cell, or {@code null} if
     * the ray is parallel to the plane, hits behind the origin, or misses the grid.
     */
    public static Cell cellAt(Ray ray, int gridSize) {
        float[] hit = planeHit(ray, gridSize);
        if (hit == null) {
            return null;
        }
        float hitX = hit[0];
        float hitZ = hit[1];
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

    /**
     * Intersects {@code ray} with the board plane and, if the hit lies just outside a border within
     * {@link #portStripMargin}, returns the corresponding {@link Port}. Inside the grid, corners
     * outside both axes, or misses return {@code null}.
     */
    public static Port portAt(Ray ray, int gridSize) {
        float[] hit = planeHit(ray, gridSize);
        if (hit == null) {
            return null;
        }
        float hitX = hit[0];
        float hitZ = hit[1];
        float extent = boardExtent(gridSize);
        float margin = portStripMargin(gridSize);

        if (hitZ >= -margin && hitZ < 0f && hitX >= 0f && hitX < extent) {
            return Port.of(Port.Side.T, clampIndex((int) (hitX / CELL), gridSize), gridSize);
        }
        if (hitZ >= extent && hitZ < extent + margin && hitX >= 0f && hitX < extent) {
            return Port.of(Port.Side.B, clampIndex((int) (hitX / CELL), gridSize), gridSize);
        }
        if (hitX >= -margin && hitX < 0f && hitZ >= 0f && hitZ < extent) {
            return Port.of(Port.Side.L, clampIndex((int) (hitZ / CELL), gridSize), gridSize);
        }
        if (hitX >= extent && hitX < extent + margin && hitZ >= 0f && hitZ < extent) {
            return Port.of(Port.Side.R, clampIndex((int) (hitZ / CELL), gridSize), gridSize);
        }
        return null;
    }

    /** @return {@code {hitX, hitZ}} or {@code null} if no forward plane hit */
    private static float[] planeHit(Ray ray, int gridSize) {
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
        return new float[] {
            ray.origin.x + t * ray.direction.x, ray.origin.z + t * ray.direction.z
        };
    }

    private static int clampIndex(int index, int gridSize) {
        if (index < 0) {
            return 0;
        }
        if (index >= gridSize) {
            return gridSize - 1;
        }
        return index;
    }
}
