package com.google.mediapipe.tasks.vision.facelandmarker

/**
 * Accessor for MediaPipe's own face-mesh topology.
 *
 * `FaceLandmarksConnections` and its connection sets are package-private in the Tasks
 * Vision AAR, so this file deliberately lives in MediaPipe's package to reach them. The
 * alternative — hardcoding the tessellation — would mean carrying roughly 2,500 index
 * pairs that could drift out of step with whatever model version is actually shipped.
 * Reflection would work too, but would fail at runtime rather than at compile time if the
 * API ever changes.
 *
 * Each connection is a landmark index pair, returned flattened as `[start, end, ...]` so
 * the drawing code can iterate without allocating per edge.
 */
object FaceMeshTopology {

    private fun flatten(connections: Set<com.google.mediapipe.tasks.components.containers.Connection>): IntArray {
        val out = IntArray(connections.size * 2)
        var i = 0
        for (c in connections) {
            out[i++] = c.start()
            out[i++] = c.end()
        }
        return out
    }

    /** The full triangulated mesh — the fine wireframe over the whole face. */
    val tessellation: IntArray by lazy {
        flatten(FaceLandmarksConnections.FACE_LANDMARKS_TESSELATION)
    }

    val faceOval: IntArray by lazy { flatten(FaceLandmarksConnections.FACE_LANDMARKS_FACE_OVAL) }
    val lips: IntArray by lazy { flatten(FaceLandmarksConnections.FACE_LANDMARKS_LIPS) }
    val leftEye: IntArray by lazy { flatten(FaceLandmarksConnections.FACE_LANDMARKS_LEFT_EYE) }
    val rightEye: IntArray by lazy { flatten(FaceLandmarksConnections.FACE_LANDMARKS_RIGHT_EYE) }
    val leftBrow: IntArray by lazy { flatten(FaceLandmarksConnections.FACE_LANDMARKS_LEFT_EYE_BROW) }
    val rightBrow: IntArray by lazy { flatten(FaceLandmarksConnections.FACE_LANDMARKS_RIGHT_EYE_BROW) }
    val leftIris: IntArray by lazy { flatten(FaceLandmarksConnections.FACE_LANDMARKS_LEFT_IRIS) }
    val rightIris: IntArray by lazy { flatten(FaceLandmarksConnections.FACE_LANDMARKS_RIGHT_IRIS) }
}
