package com.dongnh.bubblepicker.rendering

/**
 * Created by irinagalata on 1/19/17.
 */
object BubbleShader {

    const val U_MATRIX = "u_Matrix"
    const val U_BACKGROUND = "u_Background"
    const val U_TEXT = "u_Text"
    const val U_VISIBILITY = "u_Visibility"
    const val U_ASPECT_RATIO = "u_AspectRatio"

    const val A_POSITION = "a_Position"
    const val A_UV = "a_UV"

    // language=GLSL
    val vertexShader = """
        uniform mat4 u_Matrix;
        attribute vec4 a_Position;
        attribute vec2 a_UV;
        varying vec2 v_UV;
        void main()
        {
            gl_Position = u_Matrix * a_Position;
            v_UV = a_UV;
        }
    """

    // language=GLSL
    val fragmentShader = """
        precision mediump float;
        uniform vec4 u_Background;
        uniform sampler2D u_Texture;
        uniform int u_Visibility;
        uniform float u_AspectRatio; // Uniform to pass aspect ratio of the image
        varying vec2 v_UV;

        void main() {
            vec2 centeredUV;
            if (u_AspectRatio > 1.0) {
                centeredUV = vec2((v_UV.x - 0.5) / u_AspectRatio + 0.5, v_UV.y);
            } else {
                centeredUV = vec2(v_UV.x, (v_UV.y - 0.5) * u_AspectRatio + 0.5);
            }

            float distance = distance(vec2(0.5, 0.5), v_UV);
            gl_FragColor = u_Visibility > 0 ?
                mix(texture2D(u_Texture, centeredUV), u_Background, smoothstep(0.49, 0.5, distance)) : vec4(0);
        }
    """

}