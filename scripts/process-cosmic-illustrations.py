#!/usr/bin/env python3
"""
Extracts, processes, and packages the cosmic sci-fi UI illustrations
from user reference sheets and generated artwork into production assets.
Outputs transparent PNGs, mint-backed illustrations, and animated GIFs.
"""

from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import numpy as np

BRAIN_DIR = Path(r"C:\Users\Administrator\.gemini\antigravity\brain\f3d4377c-dd61-43f1-a267-ecb7a4a4b937")
USER_DIR = BRAIN_DIR / ".user_uploaded"
ASSETS_DIR = Path(r"D:\DEV_WORK\02_PERSONAL_PROJECTS\Mobile\MEDIADOCK\assets\illustrations")
DRAWABLE_DIR = Path(r"D:\DEV_WORK\02_PERSONAL_PROJECTS\Mobile\MEDIADOCK\android\app\src\main\res\drawable-nodpi")

ASSETS_DIR.mkdir(parents=True, exist_ok=True)
DRAWABLE_DIR.mkdir(parents=True, exist_ok=True)

MINT_BG = np.array([209, 241, 217], dtype=float)  # #D1F1D9
FOREST_DARK = (13, 51, 40)                         # #0D3328
VIBRANT_MINT = (52, 211, 153)                       # #34D399
COSMIC_VIOLET = (124, 92, 252)                      # #7C5CFC

def make_transparent(img, bg_color=MINT_BG, tolerance=18, ramp=10):
    """Smoothly converts background color to alpha transparency."""
    arr = np.array(img).astype(float)
    dist = np.linalg.norm(arr[:, :, :3] - bg_color, axis=2)
    
    # Alpha mask with smooth antialiasing
    alpha = np.clip((dist - tolerance) / max(ramp, 1) * 255.0, 0, 255).astype(np.uint8)
    
    # For pixels that are almost transparent, slightly neutralize color fringing
    rgba = np.dstack((arr[:, :, :3].astype(np.uint8), alpha))
    return Image.fromarray(rgba, mode="RGBA")

def crop_and_center(img, target_size=(512, 512), padding=24):
    """Crops transparent foreground and centers it in target canvas."""
    bbox = img.getbbox()
    if not bbox:
        return img.resize(target_size, Image.Resampling.LANCZOS)
    cropped = img.crop(bbox)
    
    # Calculate fit ratio
    cw, ch = cropped.size
    max_w, max_h = target_size[0] - padding * 2, target_size[1] - padding * 2
    scale = min(max_w / cw, max_h / ch)
    nw, nh = max(1, int(cw * scale)), max(1, int(ch * scale))
    resized = cropped.resize((nw, nh), Image.Resampling.LANCZOS)
    
    canvas = Image.new("RGBA", target_size, (0, 0, 0, 0))
    paste_x = (target_size[0] - nw) // 2
    paste_y = (target_size[1] - nh) // 2
    canvas.paste(resized, (paste_x, paste_y), resized)
    return canvas

def segment_sheet(sheet_path, num_rows=2, num_cols=3):
    """Splits a 2x3 grid sheet into individual cropped images."""
    im = Image.open(sheet_path).convert("RGB")
    w, h = im.size
    cell_w = w // num_cols
    cell_h = h // num_rows
    cells = []
    for r in range(num_rows):
        for c in range(num_cols):
            x0 = c * cell_w
            y0 = r * cell_h
            x1 = (c + 1) * cell_w
            y1 = (r + 1) * cell_h
            cell = im.crop((x0, y0, x1, y1))
            cells.append(cell)
    return cells

print("Processing user sheets...")
sheet1_cells = segment_sheet(USER_DIR / "media_1794993814703_a2d2041f.jpg")
sheet2_cells = segment_sheet(USER_DIR / "media_1794993821544_4cb858b5.jpg")

# Sheet 1:
# 0: Data Scan Scientist, 1: Mech Exoskeleton, 2: Lounge Astronaut
# 3: Guitar in Zero-G, 4: Space Rover, 5: Alien Scientist
# Sheet 2:
# 0: Watering Plants, 1: Sketching on Hill, 2: Quantum Cylinder Scientist
# 3: Video Call Home, 4: Pilot smiling, 5: Planetary Observatory

# 1. Browse scene: Friendly Alien Scientist (Sheet 1, cell 5)
alien_trans = make_transparent(sheet1_cells[5], MINT_BG)
alien_final = crop_and_center(alien_trans, (512, 512))
alien_final.save(ASSETS_DIR / "browse.png")
alien_final.save(DRAWABLE_DIR / "illustration_browse.png")

# 2. Connection scene: Quantum Cylinder Scientist (Sheet 2, cell 2)
conn_trans = make_transparent(sheet2_cells[2], MINT_BG)
conn_final = crop_and_center(conn_trans, (512, 512))
conn_final.save(ASSETS_DIR / "connection.png")
conn_final.save(DRAWABLE_DIR / "illustration_connection.png")

# 3. Library / Archive scene: Planetary Observatory Scientist (Sheet 2, cell 5)
lib_trans = make_transparent(sheet2_cells[5], MINT_BG)
lib_final = crop_and_center(lib_trans, (512, 512))
lib_final.save(ASSETS_DIR / "library.png")
lib_final.save(DRAWABLE_DIR / "illustration_library.png")

# 4. Workspace / Notes scene: Astronaut sketching on hill (Sheet 2, cell 1)
work_trans = make_transparent(sheet2_cells[1], MINT_BG)
work_final = crop_and_center(work_trans, (512, 512))
work_final.save(ASSETS_DIR / "workspace.png")
work_final.save(DRAWABLE_DIR / "illustration_workspace.png")

# 5. Audio scene: Astronaut playing guitar in zero-g (Sheet 1, cell 3)
audio_trans = make_transparent(sheet1_cells[3], MINT_BG)
audio_final = crop_and_center(audio_trans, (512, 512))
audio_final.save(ASSETS_DIR / "audio.png")
audio_final.save(DRAWABLE_DIR / "illustration_audio.png")

# 6. Video scene: Mech Exoskeleton Pilot (Sheet 1, cell 1)
video_trans = make_transparent(sheet1_cells[1], MINT_BG)
video_final = crop_and_center(video_trans, (512, 512))
video_final.save(ASSETS_DIR / "video.png")
video_final.save(DRAWABLE_DIR / "illustration_video.png")

# 7. Downloads / Queue scene:
# We have high-res space rover from the generator:
rover_gen_path = BRAIN_DIR / "space_rover_onboarding_illustration_1794996104382.jpg"
if rover_gen_path.exists():
    rover_img = Image.open(rover_gen_path).convert("RGB")
    rover_trans = make_transparent(rover_img, np.array([255, 255, 255]), tolerance=25, ramp=12)
    rover_final = crop_and_center(rover_trans, (512, 512))
else:
    rover_trans = make_transparent(sheet1_cells[4], MINT_BG)
    rover_final = crop_and_center(rover_trans, (512, 512))
rover_final.save(ASSETS_DIR / "downloads.png")
rover_final.save(DRAWABLE_DIR / "illustration_downloads.png")

# 8. Home / Hero scene:
# From generated high-res cosmic command bridge with holographic rings:
home_gen_path = BRAIN_DIR / "cosmic_home_clean_1794995676759.jpg"
if home_gen_path.exists():
    home_img = Image.open(home_gen_path).convert("RGB")
    home_trans = make_transparent(home_img, np.array([255, 255, 255]), tolerance=25, ramp=12)
    home_final = crop_and_center(home_trans, (512, 512))
else:
    home_trans = make_transparent(sheet1_cells[0], MINT_BG)
    home_final = crop_and_center(home_trans, (512, 512))
home_final.save(ASSETS_DIR / "home.png")
home_final.save(DRAWABLE_DIR / "illustration_home.png")

# 9. Preparation / Inspect scene:
inspect_trans = make_transparent(sheet1_cells[0], MINT_BG) # Data scanner
inspect_final = crop_and_center(inspect_trans, (512, 512))
inspect_final.save(ASSETS_DIR / "inspect.png")
inspect_final.save(DRAWABLE_DIR / "illustration_inspect.png")

print("Generating animated GIFs for interactive waiting and active transfers...")

# 10. Animated GIF for media preparation waiting (Holographic pulse rings):
# Base image: home_final or inspect_final
base_inspect = home_final.copy()
gif_frames = []
num_frames = 16

for i in range(num_frames):
    frame = base_inspect.copy()
    draw = ImageDraw.Draw(frame)
    progress = i / num_frames
    
    # Draw two expanding concentric holographic rings centered on the astronaut console
    cx, cy = 256, 256
    for ring_idx in range(3):
        phase = (progress + ring_idx * 0.33) % 1.0
        r_x = int(60 + phase * 140)
        r_y = int(24 + phase * 56)
        alpha = int((1.0 - phase) * 200)
        
        # Draw dotted or glowing ellipse
        bbox = (cx - r_x, cy - r_y, cx + r_x, cy + r_y)
        draw.ellipse(bbox, outline=(52, 211, 153, alpha), width=3)
    
    # Draw animated audio frequency bars in upper right
    bx, by = 380, 140
    for b_idx in range(5):
        h_offset = np.sin((progress * np.pi * 2) + b_idx * 0.8) * 12 + 16
        bar_x = bx + b_idx * 10
        draw.rounded_rectangle((bar_x, by - h_offset, bar_x + 5, by + h_offset), radius=2, fill=(124, 92, 252, 220))
        
    gif_frames.append(frame)

# Save animated GIF
gif_frames[0].save(
    ASSETS_DIR / "cosmic_preparing.gif",
    save_all=True,
    append_images=gif_frames[1:],
    duration=75,
    loop=0,
    disposal=2
)
gif_frames[0].save(
    DRAWABLE_DIR / "illustration_cosmic_pulse.gif",
    save_all=True,
    append_images=gif_frames[1:],
    duration=75,
    loop=0,
    disposal=2
)

# 11. Animated GIF for active rover data transfer:
base_rover = rover_final.copy()
rover_frames = []
for i in range(num_frames):
    frame = base_rover.copy()
    draw = ImageDraw.Draw(frame)
    progress = i / num_frames
    
    # Satellite antenna beacon pulse
    cx, cy = 290, 160  # Antenna location
    for p_idx in range(3):
        phase = (progress + p_idx * 0.33) % 1.0
        r = int(10 + phase * 65)
        alpha = int((1.0 - phase) * 210)
        draw.arc((cx - r, cy - r, cx + r, cy + r), start=200, end=340, fill=(52, 211, 153, alpha), width=3)
        
    # Streaming data dots moving towards upper right
    for d_idx in range(4):
        d_phase = (progress + d_idx * 0.25) % 1.0
        dot_x = cx + int(d_phase * 110)
        dot_y = cy - int(d_phase * 70)
        dot_alpha = int(np.sin(d_phase * np.pi) * 255)
        draw.ellipse((dot_x - 3, dot_y - 3, dot_x + 3, dot_y + 3), fill=(124, 92, 252, dot_alpha))
        
    rover_frames.append(frame)

rover_frames[0].save(
    ASSETS_DIR / "cosmic_rover_stream.gif",
    save_all=True,
    append_images=rover_frames[1:],
    duration=75,
    loop=0,
    disposal=2
)
rover_frames[0].save(
    DRAWABLE_DIR / "illustration_rover_stream.gif",
    save_all=True,
    append_images=rover_frames[1:],
    duration=75,
    loop=0,
    disposal=2
)

print("All cosmic illustrations and animated GIFs generated successfully!")
