package com.amine.battleship.ui

import android.graphics.Paint
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas

/** Kept in an API-isolated class so Android 8 never resolves RuntimeShader. */
@RequiresApi(33)
internal class ShaderWaterPainter : WaterPainter {
    private val shader = RuntimeShader(SOURCE)
    private val paint = Paint().apply { shader = this@ShaderWaterPainter.shader }
    override fun draw(scope: DrawScope, geometry: BoardGeometry, seconds: Float) {
        shader.setFloatUniform("origin", geometry.originX, geometry.originY)
        shader.setFloatUniform("extent", geometry.side)
        shader.setFloatUniform("time", seconds)
        scope.drawContext.canvas.nativeCanvas.drawRect(
            geometry.originX, geometry.originY, geometry.right, geometry.bottom, paint)
    }

    private companion object {
        const val SOURCE = """
            uniform float2 origin;
            uniform float extent;
            uniform float time;
            float hash(float2 p) {
                p = fract(p * float2(123.34, 345.45));
                p += dot(p, p + 34.345);
                return fract(p.x * p.y);
            }
            // Value and analytic derivatives: four small hashes, without texture reads or finite differences.
            float3 noiseD(float2 p) {
                float2 cell = floor(p);
                float2 f = fract(p);
                float2 u = f*f*(3.0-2.0*f);
                float2 du = 6.0*f*(1.0-f);
                float a = hash(cell);
                float b = hash(cell+float2(1.0,0.0));
                float c = hash(cell+float2(0.0,1.0));
                float d = hash(cell+float2(1.0,1.0));
                float crossTerm = a-b-c+d;
                return float3(a+(b-a)*u.x+(c-a)*u.y+crossTerm*u.x*u.y,
                    (b-a+crossTerm*u.y)*du.x, (c-a+crossTerm*u.x)*du.y);
            }
            half4 main(float2 fragCoord) {
                float2 p = (fragCoord-origin)/extent;
                float2 drift = float2(time*0.012, -time*0.008);
                // Two slow swells bend the ripple domain. No repeating directional wave bands.
                float3 warpX = noiseD(p*3.4+drift+float2(8.3,1.7));
                float3 warpY = noiseD(p*3.1-drift+float2(1.2,9.4));
                float2 q = p+float2(warpX.x-0.5,warpY.x-0.5)*0.24;
                float3 swell = noiseD(q*8.0+drift);
                float2 rotated = float2(q.x*0.80-q.y*0.60,q.x*0.60+q.y*0.80);
                float3 ripple = noiseD(rotated*23.0-drift*1.7+float2(5.1,2.4));
                float3 detail = noiseD(q*57.0+drift*2.1+float2(3.7,8.1));
                float2 slope = swell.yz*0.32+ripple.yz*0.17+detail.yz*0.065;
                float3 n = normalize(float3(-slope,1.0));
                float diffuse = clamp(dot(n,normalize(float3(-0.35,-0.55,1.0))),0.0,1.0);
                float facing = clamp(dot(n,normalize(float3(-0.22,-0.39,1.4))),0.0,1.0);
                float sky = smoothstep(-0.30,0.60,n.y*0.8+n.x*0.4);
                // Patchy sky reflection breaks uniform highlights, especially on the smaller board.
                float reflectionMask = smoothstep(0.46,0.80,swell.x*0.65+ripple.x*0.35);
                float specular = pow(facing,65.0)*(0.10+0.90*reflectionMask);
                float fresnel = pow(1.0-clamp(n.z,0.0,1.0),3.0);
                float3 deep = float3(0.025,0.080,0.125)*(0.75+diffuse*0.25);
                float3 reflected = float3(0.025,0.050,0.065)*sky;
                float3 glint = float3(0.14,0.18,0.19)*specular;
                float depth = 0.88+0.12*swell.x;
                float vignette = 1.0-0.10*length(p-float2(0.5));
                return half4((deep*depth+reflected+glint+fresnel*0.06)*vignette,1.0);
            }
        """
    }
}
