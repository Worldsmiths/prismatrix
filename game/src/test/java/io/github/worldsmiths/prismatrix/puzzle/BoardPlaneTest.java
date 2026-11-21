package io.github.worldsmiths.prismatrix.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.worldsmiths.core.thin3d.Ray;
import io.github.worldsmiths.core.thin3d.Vec3;
import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.Port;
import org.junit.jupiter.api.Test;

class BoardPlaneTest {

    @Test
    void mapsRayJustOutsideTopEntryCellToTPort() {
        float margin = BoardPlane.portStripMargin(6);
        Ray ray = downwardOnto(BoardPlane.cellCenterX(2), -margin * 0.5f);
        assertEquals(Port.of(Port.Side.T, 2, 6), BoardPlane.portAt(ray, 6));
    }

    @Test
    void portEdgeLiesOnBoardRim() {
        Port t2 = Port.of(Port.Side.T, 2, 6);
        assertEquals(BoardPlane.cellCenterX(2), BoardPlane.portEdgeX(t2, 6), 1e-5f);
        assertEquals(0f, BoardPlane.portEdgeZ(t2, 6), 1e-5f);
        Port r3 = Port.of(Port.Side.R, 3, 6);
        assertEquals(BoardPlane.boardExtent(6), BoardPlane.portEdgeX(r3, 6), 1e-5f);
        assertEquals(BoardPlane.cellCenterZ(3), BoardPlane.portEdgeZ(r3, 6), 1e-5f);
    }

    @Test
    void returnsNullWhenRayMissesPortStripsFarAway() {
        Ray ray = downwardOnto(-10f, -10f);
        assertNull(BoardPlane.portAt(ray, 6));
    }

    @Test
    void mapsRayHittingTopLeftCellCenterToOpticsYZero() {
        Ray ray = downwardOnto(BoardPlane.cellCenterX(0), BoardPlane.cellCenterZ(0));
        Cell cell = BoardPlane.cellAt(ray, 6);
        assertEquals(new Cell(0, 0), cell);
    }

    @Test
    void mapsRayHittingBottomRowToOpticsYFive() {
        Ray ray = downwardOnto(BoardPlane.cellCenterX(0), BoardPlane.cellCenterZ(5));
        Cell cell = BoardPlane.cellAt(ray, 6);
        assertEquals(new Cell(0, 5), cell);
    }

    @Test
    void returnsNullWhenRayMissesBoard() {
        Ray ray = downwardOnto(-10f, -10f);
        assertNull(BoardPlane.cellAt(ray, 6));
    }

    private static Ray downwardOnto(float hitX, float hitZ) {
        return new Ray(new Vec3(hitX, 10f, hitZ), new Vec3(0f, -1f, 0f));
    }
}
