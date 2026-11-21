package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.core.thin3d.Camera;
import io.github.worldsmiths.core.thin3d.Environment;
import io.github.worldsmiths.core.thin3d.Model;
import io.github.worldsmiths.core.thin3d.ModelBatch;
import io.github.worldsmiths.core.thin3d.ModelBuilder;
import io.github.worldsmiths.core.thin3d.ModelInstance;
import io.github.worldsmiths.core.thin3d.OrthographicCamera;
import io.github.worldsmiths.core.thin3d.Ray;
import io.github.worldsmiths.core.thin3d.Thin3D;
import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.sandbox.GhostBoard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thin3D pitched board: 38° X / 0° Y orthographic camera, pick-ray → {@link BoardPlane} (cell or
 * near-edge port), tint/height-distinct ghosts, emissive cell-center beams from Test Fire visits.
 */
public final class BoardView {

    static final float PITCH_DEG = 38f;
    static final float YAW_DEG = 0f;
    static final float CAMERA_DISTANCE = 14f;

    private static final float DRAFT_H = 0.18f;
    private static final float GHOST_H = 0.42f;
    private static final float BEAM_Y = 0.12f;
    private static final float BEAM_THICK = 0.18f;
    private static final float BEAM_LEN = 0.85f;

    private final Thin3D thin3d;
    private final int gridSize;

    private OrthographicCamera camera;
    private Environment environment;
    private ModelBatch batch;
    private Model boardModel;
    private Model draftModel;
    private Model ghostModel;
    private Model beamModel;
    private ModelInstance boardInstance;
    private final List<ModelInstance> draftInstances = new ArrayList<ModelInstance>();
    private final List<ModelInstance> ghostInstances = new ArrayList<ModelInstance>();
    private final List<ModelInstance> beamInstances = new ArrayList<ModelInstance>();
    private int lastWidth = -1;
    private int lastHeight = -1;
    private List<Cell> beamCells = Collections.emptyList();

    public BoardView(Thin3D thin3d, int gridSize) {
        if (thin3d == null) {
            throw new IllegalArgumentException("thin3d must not be null");
        }
        if (gridSize < 1) {
            throw new IllegalArgumentException("gridSize must be >= 1");
        }
        this.thin3d = thin3d;
        this.gridSize = gridSize;
    }

    public void ensureSized(int screenWidth, int screenHeight) {
        int w = screenWidth > 0 ? screenWidth : 800;
        int h = screenHeight > 0 ? screenHeight : 600;
        if (camera != null && w == lastWidth && h == lastHeight) {
            return;
        }
        lastWidth = w;
        lastHeight = h;
        float aspect = (float) w / (float) h;
        float worldH = BoardPlane.boardExtent(gridSize) + 4f;
        float worldW = worldH * aspect;
        camera = thin3d.createOrthographicCamera(worldW, worldH);
        poseCamera();
        if (batch == null) {
            createSharedResources();
        }
    }

    /**
     * One pick-ray resolve against the pitched board plane: near-edge {@link Port} first, else
     * optics {@link Cell}. FakeThin3D tests should {@code seedPickRay} a downward ray onto the
     * intended world hit (cell center or just outside a border within {@link
     * BoardPlane#portStripMargin}); screen coords are ignored by the fake seed.
     */
    public Pick pick(float screenX, float screenY) {
        requireCamera();
        camera.update();
        Ray ray = camera.getPickRay(screenX, screenY);
        Port port = BoardPlane.portAt(ray, gridSize);
        if (port != null) {
            return Pick.port(port);
        }
        return Pick.cell(BoardPlane.cellAt(ray, gridSize));
    }

    public Cell pickCell(float screenX, float screenY) {
        return pick(screenX, screenY).cell();
    }

    /**
     * Hit-test expanded world-space port strips via pick-ray → board plane. Returns the port or
     * {@code null}.
     */
    public Port hitTestPortStrip(int inputX, int inputY) {
        return pick(inputX, inputY).port();
    }

    public void setBeamCells(List<Cell> cells) {
        if (cells == null) {
            beamCells = Collections.emptyList();
        } else {
            beamCells = Collections.unmodifiableList(new ArrayList<Cell>(cells));
        }
    }

    public void render(PuzzleMode puzzle, GhostBoard ghosts) {
        requireCamera();
        syncInstances(puzzle, ghosts);
        poseCamera();
        camera.update();
        batch.begin(camera);
        batch.render(boardInstance, environment);
        for (ModelInstance inst : draftInstances) {
            batch.render(inst, environment);
        }
        for (ModelInstance inst : ghostInstances) {
            batch.render(inst, environment);
        }
        for (ModelInstance inst : beamInstances) {
            batch.render(inst, environment);
        }
        batch.end();
    }

    public void dispose() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        disposeModel(boardModel);
        disposeModel(draftModel);
        disposeModel(ghostModel);
        disposeModel(beamModel);
        boardModel = null;
        draftModel = null;
        ghostModel = null;
        beamModel = null;
        draftInstances.clear();
        ghostInstances.clear();
        beamInstances.clear();
        camera = null;
    }

    private void createSharedResources() {
        ModelBuilder builder = thin3d.createModelBuilder();
        float extent = BoardPlane.boardExtent(gridSize);
        boardModel =
                builder.createBox(extent, 0.05f, extent, 0.28f, 0.3f, 0.34f, 1f);
        draftModel = builder.createBox(0.7f, DRAFT_H, 0.7f, 0.85f, 0.85f, 0.9f, 1f);
        ghostModel = builder.createBox(0.7f, GHOST_H, 0.7f, 0.35f, 0.75f, 0.95f, 1f);
        beamModel =
                builder.createBoxEmissive(
                        BEAM_THICK,
                        BEAM_THICK,
                        BEAM_LEN,
                        1f,
                        0.85f,
                        0.35f,
                        1f,
                        1f,
                        0.55f,
                        0.15f,
                        1f);
        boardInstance = thin3d.createInstance(boardModel);
        float cx = BoardPlane.boardCenterX(gridSize);
        float cz = BoardPlane.boardCenterZ(gridSize);
        boardInstance.setTranslation(cx, -0.025f, cz);
        environment = thin3d.createEnvironment();
        environment.setAmbientLight(0.45f, 0.45f, 0.48f, 1f);
        environment.addDirectionalLight(0.85f, 0.85f, 0.8f, -0.4f, -1f, -0.2f);
        batch = thin3d.createModelBatch();
    }

    private void syncInstances(PuzzleMode puzzle, GhostBoard ghosts) {
        draftInstances.clear();
        ghostInstances.clear();
        beamInstances.clear();
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                PieceType draft = puzzle.pieceAt(x, y);
                if (draft != null) {
                    ModelInstance inst = thin3d.createInstance(draftModel);
                    inst.setTranslation(
                            BoardPlane.cellCenterX(x), DRAFT_H * 0.5f, BoardPlane.cellCenterZ(y));
                    draftInstances.add(inst);
                }
                PieceType ghost = ghosts == null ? null : ghosts.pieceAt(x, y);
                if (ghost != null) {
                    ModelInstance inst = thin3d.createInstance(ghostModel);
                    inst.setTranslation(
                            BoardPlane.cellCenterX(x), GHOST_H * 0.5f + 0.08f, BoardPlane.cellCenterZ(y));
                    ghostInstances.add(inst);
                }
            }
        }
        for (Cell cell : beamCells) {
            ModelInstance inst = thin3d.createInstance(beamModel);
            inst.setTranslationRotationScaling(
                    BoardPlane.cellCenterX(cell.x()),
                    BEAM_Y,
                    BoardPlane.cellCenterZ(cell.y()),
                    0f,
                    0f,
                    0f,
                    1f,
                    1f,
                    1f);
            beamInstances.add(inst);
        }
    }

    private void poseCamera() {
        float cx = BoardPlane.boardCenterX(gridSize);
        float cz = BoardPlane.boardCenterZ(gridSize);
        double pitchRad = Math.toRadians(PITCH_DEG);
        float eyeY = (float) (CAMERA_DISTANCE * Math.cos(pitchRad));
        float eyeZ = (float) (cz + CAMERA_DISTANCE * Math.sin(pitchRad));
        // Yaw 0°: stay on +Z side of the board looking toward center.
        camera.setPosition(cx + YAW_DEG, eyeY, eyeZ);
        camera.setUp(0f, 1f, 0f);
        camera.lookAt(cx, BoardPlane.PLANE_Y, cz);
        camera.setNear(0.1f);
        camera.setFar(100f);
    }

    private void requireCamera() {
        if (camera == null) {
            ensureSized(800, 600);
        }
    }

    private static void disposeModel(Model model) {
        if (model != null) {
            model.dispose();
        }
    }

    /** Package-visible for resize assertions. */
    Camera camera() {
        return camera;
    }

    /** Result of a single pick-ray against the board plane. */
    public static final class Pick {
        private final Port port;
        private final Cell cell;

        private Pick(Port port, Cell cell) {
            this.port = port;
            this.cell = cell;
        }

        static Pick port(Port port) {
            return new Pick(port, null);
        }

        static Pick cell(Cell cell) {
            return new Pick(null, cell);
        }

        public Port port() {
            return port;
        }

        public Cell cell() {
            return cell;
        }
    }
}
