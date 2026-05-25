# Galaxy Watch GLB Model — Asset Pipeline Guide

## Required File Path

```
app/src/main/assets/models/galaxy_watch.glb
```

Filament (via SceneView 2.3.0) reads `.glb` files directly from the `assets/` folder
at runtime. Do NOT place the file in `res/raw/` or `drawable/`.

---

## Step 1 — Source the Raw Model

### Option A: Sketchfab (Best Quality, Free)
1. Go to [sketchfab.com](https://sketchfab.com)
2. Search: `Galaxy Watch 6` or `Wear OS Smartwatch`
3. Filter: Downloadable + Staff Pick
4. Download as `.glb` if available, or `.fbx`/`.obj` if not

### Option B: Meshy AI (Generative, Unbranded)
Prompt: `"Premium minimalist round smartwatch, clean glass bezel, black chrome metallic
texture, low-poly topology under 15000 polygons, mobile AR ready, GLB format"`

### Option C: Samsung Developer Portal
Download the Play Store Asset Creator package from developer.samsung.com.
Extract the layered PSDs and trace watch geometry in Blender.

---

## Step 2 — Optimize in Blender (Sub-2ms GPU target)

```
Raw Asset (.FBX / .OBJ / .glTF)
         |
         v
[Blender: Import Asset]
         |
         v
[Decimate Modifier — Target: < 15,000 polygons total]
  - Apply to all mesh objects
  - Ratio: ~0.3 for high-poly CAD downloads
         |
         v
[Separate Watch Face Plane as independent sub-mesh]
  - Select the flat circular screen geometry
  - Mesh > Separate > By Selection
  - Rename object to exactly: "watch_face_screen"
  - This node is programmatically targeted by WatchFaceTextureEngine at runtime
         |
         v
[Bake PBR Textures to 1024x1024]
  - Bake: Albedo/Base Color, Roughness, Metallic, Normal maps
  - Combine into a single texture atlas (1024x1024 px, power-of-two)
  - Embed all maps inside the .glb container (File > Export > glTF Binary)
         |
         v
[Export: File > Export > glTF 2.0 (.glb)]
  - Format: GLB (Binary)
  - Include: Selected Objects, Geometry (Apply Modifiers)
  - Textures: Embed (stores textures inside .glb file)
  - Target size: < 8 MB for sub-2ms Filament loading time
```

---

## Step 3 — Verify Sub-Mesh Node Name

The SceneView / Filament integration targets a specific sub-mesh by name to
swap in the live watch-face texture at runtime:

```kotlin
// WatchScene3D.kt — production texture assignment block
val watchFaceNode = modelNode.getChildNode("watch_face_screen")
watchFaceNode?.materialInstance?.setExternalTexture(
    "baseColorMap",
    ExternalTexture().apply { attachToView(watchFaceTextureBitmap) }
)
```

**The sub-mesh MUST be named exactly `watch_face_screen`** (case-sensitive, no spaces).
Verify in Blender's Outliner panel before export.

---

## Step 4 — Validate with Filament Asset Validator

```bash
# Install Google's GLTF validator
npx gltf-validator galaxy_watch.glb

# Check polycount
blender --background galaxy_watch.glb --python -c "
import bpy
total = sum(len(o.data.polygons) for o in bpy.data.objects if o.type == 'MESH')
print(f'Total polygons: {total}')
"
```

---

## PBR Texture Requirements for Filament

| Map | Format | Resolution | Embedded |
|-----|--------|-----------|---------|
| Albedo (Base Color) | RGBA PNG | 1024 x 1024 | Yes |
| Roughness/Metallic | RGB PNG | 1024 x 1024 | Yes |
| Normal Map | RGB PNG (OpenGL Y-up) | 1024 x 1024 | Yes |
| Emissive (watch face glow) | RGB PNG | 512 x 512 | Yes |

Watch face screen node (`watch_face_screen`) should have an **Emissive** material
so the live-data texture projection glows correctly in dark environments.
