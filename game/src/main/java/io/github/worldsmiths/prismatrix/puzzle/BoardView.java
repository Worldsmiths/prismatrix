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
import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.sandbox.ConsistencyBadge;
import io.github.worldsmiths.prismatrix.sandbox.GhostBoard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thin3D pitched board: 38° X / 0° Y orthographic camera, pick-ray → {@link BoardPlane} (cell or
 * near-edge port), diagonal mirror blades, cell grid, status-colored clue port pads (selected
 * taller), uniform amber emissive beams from entry rim through cells to exit rim.
 */
public final class BoardView {

    static final float PITCH_DEG = 38f;
    static final float YAW_DEG = 0f;
    static final float CAMERA_DISTANCE = 14f;

    private static final float DRAFT_H = 0.22f;
    private static final float GHOST_H = 0.48f;
    private static final float ORB_H = 0.36f;
    private static final float ORB_SIZE = 0.55f;
    private static final float MIRROR_LEN = 0.92f;
    /** Thick enough that the beam does not read through the blade face. */
    private static final float MIRROR_THICK = 0.22f;
    private static final float BEAM_Y = 0.14f;
    private static final float BEAM_THICK = 0.14f;
    /** Unit-length emissive box along local Z; scaled per segment. */
    private static final float BEAM_UNIT = 1f;
    private static final float GRID_H = 0.03f;
    private static final float GRID_THICK = 0.04f;
    private static final float PORT_PAD_H = 0.06f;
    private static final float PORT_PAD_W = 0.72f;
    private static final float PORT_PAD_D = 0.28f;
    /** FORWARD {@code /}: blade along (1,0,-1) → eulerY +45°. BACK {@code \}: eulerY -45°. */
    private static final float FORWARD_YAW = 45f;
    private static final float BACK_YAW = -45f;

    private final Thin3D thin3d;
    private final int gridSize;

    private OrthographicCamera camera;
    private Environment environment;
    private ModelBatch batch;
    private Model boardModel;
    private Model draftModel;
    private Model ghostModel;
    private Model orbRModel;
    private Model orbGModel;
    private Model orbBModel;
    private Model beamModel;
    private Model gridLineXModel;
    private Model gridLineZModel;
    private Model portIdleModel;
    private Model portClueExitModel;
    private Model portNeutralModel;
    private Model portGreenModel;
    private Model portRedModel;
    private ModelInstance boardInstance;
    private final List<ModelInstance> gridInstances = new ArrayList<ModelInstance>();
    private final List<ModelInstance> portInstances = new ArrayList<ModelInstance>();
    private final List<ModelInstance> draftInstances = new ArrayList<ModelInstance>();
    private final List<ModelInstance> ghostInstances = new ArrayList<ModelInstance>();
    private final List<ModelInstance> beamInstances = new ArrayList<ModelInstance>();
    private int lastWidth = -1;
    private int lastHeight = -1;
    private List<Cell> beamCells = Collections.emptyList();
    private Port beamEntryPort;
    private Port beamExitPort;

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

    /** Clears the active Test Fire beam. */
    public void clearBeam() {
        beamCells = Collections.emptyList();
        beamEntryPort = null;
        beamExitPort = null;
    }

    /**
     * Sets the Test Fire path: rim → visited cell centers → exit rim. All segments share the amber
     * beam; consistency status is drawn on port pads, not on border half-cells.
     */
    public void setBeamPath(Port entryPort, List<Cell> cells, Port exitPort) {
        beamEntryPort = entryPort;
        beamExitPort = exitPort;
        if (cells == null) {
            beamCells = Collections.emptyList();
        } else {
            beamCells = Collections.unmodifiableList(new ArrayList<Cell>(cells));
        }
    }

    public void render(
            PuzzleMode puzzle,
            GhostBoard ghosts,
            List<ConsistencyBadge> badges,
            Port activeTestFirePort) {
        requireCamera();
        syncPortPads(puzzle, badges, activeTestFirePort);
        syncInstances(puzzle, ghosts);
        drawFrame();
    }

    /**
     * Mode-agnostic board bind for PvP phase-switch: pieces from {@link BoardState} (mirrors +
     * orbs), optional active port highlight, plus any beam set via {@link #setBeamPath}.
     */
    public void renderBoard(BoardState board, Port activePort) {
        requireCamera();
        syncPortPads(null, Collections.<ConsistencyBadge>emptyList(), activePort);
        syncBoardPieces(board);
        drawFrame();
    }

    private void drawFrame() {
        poseCamera();
        camera.update();
        batch.begin(camera);
        batch.render(boardInstance, environment);
        for (ModelInstance inst : gridInstances) {
            batch.render(inst, environment);
        }
        for (ModelInstance inst : portInstances) {
            batch.render(inst, environment);
        }
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
        disposeModel(orbRModel);
        disposeModel(orbGModel);
        disposeModel(orbBModel);
        disposeModel(beamModel);
        disposeModel(gridLineXModel);
        disposeModel(gridLineZModel);
        disposeModel(portIdleModel);
        disposeModel(portClueExitModel);
        disposeModel(portNeutralModel);
        disposeModel(portGreenModel);
        disposeModel(portRedModel);
        boardModel = null;
        draftModel = null;
        ghostModel = null;
        orbRModel = null;
        orbGModel = null;
        orbBModel = null;
        beamModel = null;
        gridLineXModel = null;
        gridLineZModel = null;
        portIdleModel = null;
        portClueExitModel = null;
        portNeutralModel = null;
        portGreenModel = null;
        portRedModel = null;
        gridInstances.clear();
        portInstances.clear();
        draftInstances.clear();
        ghostInstances.clear();
        beamInstances.clear();
        camera = null;
    }

    private void createSharedResources() {
        ModelBuilder builder = thin3d.createModelBuilder();
        float extent = BoardPlane.boardExtent(gridSize);
        boardModel = builder.createBox(extent, 0.05f, extent, 0.22f, 0.24f, 0.28f, 1f);
        draftModel =
                builder.createBox(
                        MIRROR_LEN, DRAFT_H, MIRROR_THICK, 0.88f, 0.88f, 0.92f, 1f);
        ghostModel =
                builder.createBox(
                        MIRROR_LEN, GHOST_H, MIRROR_THICK, 0.35f, 0.78f, 0.98f, 1f);
        orbRModel = builder.createSphere(ORB_SIZE, ORB_SIZE, ORB_SIZE, 12, 12, 0.9f, 0.2f, 0.2f, 1f);
        orbGModel = builder.createSphere(ORB_SIZE, ORB_SIZE, ORB_SIZE, 12, 12, 0.2f, 0.85f, 0.3f, 1f);
        orbBModel = builder.createSphere(ORB_SIZE, ORB_SIZE, ORB_SIZE, 12, 12, 0.25f, 0.45f, 0.95f, 1f);
        beamModel =
                builder.createBoxEmissive(
                        BEAM_THICK,
                        BEAM_THICK,
                        BEAM_UNIT,
                        1f,
                        0.85f,
                        0.35f,
                        1f,
                        1f,
                        0.55f,
                        0.15f,
                        1f);
        gridLineXModel =
                builder.createBox(extent, GRID_H, GRID_THICK, 0.55f, 0.58f, 0.62f, 1f);
        gridLineZModel =
                builder.createBox(GRID_THICK, GRID_H, extent, 0.55f, 0.58f, 0.62f, 1f);
        portIdleModel =
                builder.createBox(PORT_PAD_W, PORT_PAD_H, PORT_PAD_D, 0.28f, 0.28f, 0.32f, 1f);
        // Cool grey marks puzzle exits that are not also clue entries.
        portClueExitModel =
                builder.createBox(PORT_PAD_W, PORT_PAD_H, PORT_PAD_D, 0.38f, 0.48f, 0.62f, 1f);
        portNeutralModel =
                builder.createBox(PORT_PAD_W, PORT_PAD_H, PORT_PAD_D, 0.55f, 0.55f, 0.6f, 1f);
        portGreenModel =
                builder.createBox(PORT_PAD_W, PORT_PAD_H, PORT_PAD_D, 0.2f, 0.85f, 0.35f, 1f);
        portRedModel =
                builder.createBox(PORT_PAD_W, PORT_PAD_H, PORT_PAD_D, 0.9f, 0.25f, 0.25f, 1f);
        boardInstance = thin3d.createInstance(boardModel);
        float cx = BoardPlane.boardCenterX(gridSize);
        float cz = BoardPlane.boardCenterZ(gridSize);
        boardInstance.setTranslation(cx, -0.025f, cz);
        buildGrid();
        environment = thin3d.createEnvironment();
        environment.setAmbientLight(0.5f, 0.5f, 0.52f, 1f);
        environment.addDirectionalLight(0.9f, 0.9f, 0.85f, -0.35f, -1f, -0.25f);
        batch = thin3d.createModelBatch();
    }

    private void buildGrid() {
        gridInstances.clear();
        float cx = BoardPlane.boardCenterX(gridSize);
        float cz = BoardPlane.boardCenterZ(gridSize);
        float gridY = GRID_H * 0.5f + 0.01f;
        for (int i = 0; i <= gridSize; i++) {
            float z = i * BoardPlane.CELL;
            ModelInstance alongX = thin3d.createInstance(gridLineXModel);
            alongX.setTranslation(cx, gridY, z);
            gridInstances.add(alongX);
            float x = i * BoardPlane.CELL;
            ModelInstance alongZ = thin3d.createInstance(gridLineZModel);
            alongZ.setTranslation(x, gridY, cz);
            gridInstances.add(alongZ);
        }
    }

    private void syncPortPads(
            PuzzleMode puzzle, List<ConsistencyBadge> badges, Port activeTestFirePort) {
        portInstances.clear();
        Map<Port, ConsistencyBadge.Kind> byEntry = badgeIndex(badges);
        Set<Port> clueExits = new HashSet<Port>();
        if (puzzle != null) {
            for (ClueDefinition clue : puzzle.clues()) {
                clueExits.add(clue.exitPort());
            }
        }
        ConsistencyBadge.Kind activeStatus =
                activeTestFirePort == null ? null : byEntry.get(activeTestFirePort);
        float extent = BoardPlane.boardExtent(gridSize);
        float margin = BoardPlane.portStripMargin(gridSize);
        float portY = PORT_PAD_H * 0.5f + 0.02f;
        float outward = Math.min(margin * 0.55f, PORT_PAD_D * 0.55f);
        for (int i = 0; i < gridSize; i++) {
            placePortPad(
                    Port.of(Port.Side.T, i, gridSize),
                    BoardPlane.cellCenterX(i),
                    -outward,
                    0f,
                    portY,
                    byEntry,
                    clueExits,
                    activeTestFirePort,
                    activeStatus);
            placePortPad(
                    Port.of(Port.Side.B, i, gridSize),
                    BoardPlane.cellCenterX(i),
                    extent + outward,
                    0f,
                    portY,
                    byEntry,
                    clueExits,
                    activeTestFirePort,
                    activeStatus);
            placePortPad(
                    Port.of(Port.Side.L, i, gridSize),
                    -outward,
                    BoardPlane.cellCenterZ(i),
                    90f,
                    portY,
                    byEntry,
                    clueExits,
                    activeTestFirePort,
                    activeStatus);
            placePortPad(
                    Port.of(Port.Side.R, i, gridSize),
                    extent + outward,
                    BoardPlane.cellCenterZ(i),
                    90f,
                    portY,
                    byEntry,
                    clueExits,
                    activeTestFirePort,
                    activeStatus);
        }
    }

    private void placePortPad(
            Port port,
            float x,
            float z,
            float yawDeg,
            float portY,
            Map<Port, ConsistencyBadge.Kind> byEntry,
            Set<Port> clueExits,
            Port activeTestFirePort,
            ConsistencyBadge.Kind activeStatus) {
        Model model = portModelFor(port, byEntry, clueExits, activeStatus);
        boolean selected =
                (activeTestFirePort != null && activeTestFirePort.equals(port))
                        || (beamExitPort != null && beamExitPort.equals(port) && beamEntryPort != null);
        float scaleY = selected ? 1.8f : 1f;
        float scaleXZ = selected ? 1.1f : 1f;
        ModelInstance inst = thin3d.createInstance(model);
        inst.setTranslationRotationScaling(
                x, portY * scaleY, z, 0f, yawDeg, 0f, scaleXZ, scaleY, scaleXZ);
        portInstances.add(inst);
    }

    /**
     * Status lives on pads: clue entry → badge color; active Test Fire exit inherits that status;
     * other clue exits stay cool grey; everything else idle.
     */
    private Model portModelFor(
            Port port,
            Map<Port, ConsistencyBadge.Kind> byEntry,
            Set<Port> clueExits,
            ConsistencyBadge.Kind activeStatus) {
        ConsistencyBadge.Kind kind = byEntry.get(port);
        if (kind == null
                && activeStatus != null
                && beamExitPort != null
                && beamExitPort.equals(port)) {
            kind = activeStatus;
        }
        if (kind != null) {
            switch (kind) {
                case GREEN:
                    return portGreenModel;
                case RED:
                    return portRedModel;
                case NEUTRAL:
                default:
                    return portNeutralModel;
            }
        }
        if (clueExits.contains(port)) {
            return portClueExitModel;
        }
        return portIdleModel;
    }

    private static Map<Port, ConsistencyBadge.Kind> badgeIndex(List<ConsistencyBadge> badges) {
        Map<Port, ConsistencyBadge.Kind> map = new HashMap<Port, ConsistencyBadge.Kind>();
        if (badges == null) {
            return map;
        }
        for (ConsistencyBadge badge : badges) {
            map.put(badge.entryPort(), badge.kind());
        }
        return map;
    }

    private void syncInstances(PuzzleMode puzzle, GhostBoard ghosts) {
        draftInstances.clear();
        ghostInstances.clear();
        beamInstances.clear();
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                PieceType draft = puzzle.pieceAt(x, y);
                if (draft != null) {
                    draftInstances.add(
                            placeMirror(
                                    draftModel,
                                    x,
                                    y,
                                    DRAFT_H * 0.5f,
                                    yawFor(draft)));
                }
                PieceType ghost = ghosts == null ? null : ghosts.pieceAt(x, y);
                if (ghost != null) {
                    ghostInstances.add(
                            placeMirror(
                                    ghostModel,
                                    x,
                                    y,
                                    GHOST_H * 0.5f + 0.06f,
                                    yawFor(ghost)));
                }
            }
        }
        placeBeams();
    }

    /** Bind pieces from a BoardState (mirrors as blades, orbs as spheres). No ghosts. */
    private void syncBoardPieces(BoardState board) {
        draftInstances.clear();
        ghostInstances.clear();
        beamInstances.clear();
        if (board != null) {
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    PieceType piece = board.pieceAt(x, y);
                    if (piece == null) {
                        continue;
                    }
                    if (piece == PieceType.FORWARD || piece == PieceType.BACK) {
                        draftInstances.add(
                                placeMirror(
                                        draftModel,
                                        x,
                                        y,
                                        DRAFT_H * 0.5f,
                                        yawFor(piece)));
                    } else if (piece == PieceType.ORB_R
                            || piece == PieceType.ORB_G
                            || piece == PieceType.ORB_B) {
                        draftInstances.add(placeOrb(piece, x, y));
                    }
                }
            }
        }
        placeBeams();
    }

    private ModelInstance placeMirror(
            Model model, int opticsX, int opticsY, float y, float eulerY) {
        ModelInstance inst = thin3d.createInstance(model);
        inst.setTranslationRotationScaling(
                BoardPlane.cellCenterX(opticsX),
                y,
                BoardPlane.cellCenterZ(opticsY),
                0f,
                eulerY,
                0f,
                1f,
                1f,
                1f);
        return inst;
    }

    private ModelInstance placeOrb(PieceType type, int opticsX, int opticsY) {
        Model model = orbModelFor(type);
        ModelInstance inst = thin3d.createInstance(model);
        inst.setTranslationRotationScaling(
                BoardPlane.cellCenterX(opticsX),
                ORB_H * 0.5f + 0.04f,
                BoardPlane.cellCenterZ(opticsY),
                0f,
                0f,
                0f,
                1f,
                1f,
                1f);
        return inst;
    }

    private Model orbModelFor(PieceType type) {
        if (type == PieceType.ORB_R) {
            return orbRModel;
        }
        if (type == PieceType.ORB_G) {
            return orbGModel;
        }
        return orbBModel;
    }

    private static float yawFor(PieceType type) {
        return type == PieceType.FORWARD ? FORWARD_YAW : BACK_YAW;
    }

    /** Uniform amber segments along entry rim → cell centers → exit rim. */
    private void placeBeams() {
        if (beamCells.isEmpty() && beamEntryPort == null) {
            return;
        }
        List<float[]> points = new ArrayList<float[]>();
        if (beamEntryPort != null) {
            points.add(
                    new float[] {
                        BoardPlane.portEdgeX(beamEntryPort, gridSize),
                        BoardPlane.portEdgeZ(beamEntryPort, gridSize)
                    });
        }
        for (Cell cell : beamCells) {
            points.add(
                    new float[] {
                        BoardPlane.cellCenterX(cell.x()), BoardPlane.cellCenterZ(cell.y())
                    });
        }
        if (beamExitPort != null) {
            points.add(
                    new float[] {
                        BoardPlane.portEdgeX(beamExitPort, gridSize),
                        BoardPlane.portEdgeZ(beamExitPort, gridSize)
                    });
        }
        if (points.size() == 1) {
            float[] p = points.get(0);
            ModelInstance inst = thin3d.createInstance(beamModel);
            inst.setTranslationRotationScaling(p[0], BEAM_Y, p[1], 0f, 0f, 0f, 1f, 1f, 0.4f);
            beamInstances.add(inst);
            return;
        }
        for (int i = 0; i < points.size() - 1; i++) {
            float[] a = points.get(i);
            float[] b = points.get(i + 1);
            addBeamSegment(a[0], a[1], b[0], b[1]);
        }
    }

    private void addBeamSegment(float ax, float az, float bx, float bz) {
        float dx = bx - ax;
        float dz = bz - az;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-4f) {
            return;
        }
        float yawDeg = (float) Math.toDegrees(Math.atan2(dx, dz));
        ModelInstance inst = thin3d.createInstance(beamModel);
        inst.setTranslationRotationScaling(
                (ax + bx) * 0.5f,
                BEAM_Y,
                (az + bz) * 0.5f,
                0f,
                yawDeg,
                0f,
                1f,
                1f,
                len / BEAM_UNIT);
        beamInstances.add(inst);
    }

    private void poseCamera() {
        float cx = BoardPlane.boardCenterX(gridSize);
        float cz = BoardPlane.boardCenterZ(gridSize);
        double pitchRad = Math.toRadians(PITCH_DEG);
        float eyeY = (float) (CAMERA_DISTANCE * Math.cos(pitchRad));
        float eyeZ = (float) (cz + CAMERA_DISTANCE * Math.sin(pitchRad));
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
