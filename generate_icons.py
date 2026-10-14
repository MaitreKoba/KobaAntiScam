#!/usr/bin/env python3
"""Generate Android launcher icons from source image."""

from PIL import Image, ImageDraw
import os

# Source image path
SOURCE_IMAGE = "C:/Users/lemai/Pictures/Tohru_Judge.png"

# Base directory for res
RES_DIR = "D:/Kobayashi/Developpement/shield/app/src/main/res"

# Icon sizes for each density
ICON_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

def create_round_icon(image):
    """Create a round icon by applying a circular mask."""
    # Create a mask with a circle
    mask = Image.new('L', image.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, image.size[0], image.size[1]), fill=255)
    
    # Apply the mask
    result = Image.new('RGBA', image.size, (0, 0, 0, 0))
    result.paste(image, (0, 0), mask)
    return result

def main():
    # Open source image
    source = Image.open(SOURCE_IMAGE)
    print(f"Source image: {source.size}, mode: {source.mode}")
    
    # Convert to RGBA if needed
    if source.mode != 'RGBA':
        source = source.convert('RGBA')
    
    # Process each density
    for density_dir, size in ICON_SIZES.items():
        full_dir = os.path.join(RES_DIR, density_dir)
        
        # Create directory if it doesn't exist
        os.makedirs(full_dir, exist_ok=True)
        
        # Resize source to target size (maintain aspect ratio, center crop)
        source_copy = source.copy()
        
        # Resize to fit the size while maintaining aspect ratio
        source_copy.thumbnail((size, size), Image.Resampling.LANCZOS)
        
        # Create a new image with exact size (center the resized image)
        new_image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        offset = ((size - source_copy.width) // 2, (size - source_copy.height) // 2)
        new_image.paste(source_copy, offset)
        
        # Save square icon
        square_path = os.path.join(full_dir, "ic_launcher.png")
        new_image.save(square_path, "PNG")
        print(f"Created square icon: {square_path} ({size}x{size})")
        
        # Create and save round icon
        round_image = create_round_icon(new_image)
        round_path = os.path.join(full_dir, "ic_launcher_round.png")
        round_image.save(round_path, "PNG")
        print(f"Created round icon: {round_path} ({size}x{size})")
    
    print("\nAll icons generated successfully!")

if __name__ == "__main__":
    main()
